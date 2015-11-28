package server.http

import akka.pattern.ask
import akka.util.Timeout
import server.facebook._
import spray.can.Http
import spray.http.HttpMethods._
import spray.http._
import spray.httpx.SprayJsonSupport
import spray.json._
import akka.actor._
import akka.io.IO
import spray.can._

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.language.postfixOps

object FbJsonProtocol extends DefaultJsonProtocol {
  implicit val userNodeFormat = jsonFormat5(UserNode)
  implicit val pageNodeFormat = jsonFormat5(PageNode)
  implicit val postNodeFormat = jsonFormat7(PostNode)
  implicit val friendListNodeFormat = jsonFormat3(FriendListNode)
  implicit val photoNodeFormat = jsonFormat8(PhotoNode)
  implicit val albumNodeFormat = jsonFormat9(AlbumNode)
  implicit val createFbNodeRspFormat = jsonFormat2(CreateFbNodeRsp)
}

class FbServerHttp extends Actor with ActorLogging with AdditionalFormats with SprayJsonSupport {

  import FbJsonProtocol._
  import context.dispatcher

  implicit val someTimeout = Timeout(5 seconds)

  val system = ActorSystem("HttpServerTest")
  val fbServer = system.actorOf(Props(new FbServer), "FbServer00")

  def receive = {
    case _: Http.Connected =>
      sender ! Http.Register(self)

    case HttpRequest(GET, Uri.Path("/ping"), _, _, _) =>
      sender ! HttpResponse(entity = "PONG!")

    case HttpRequest(POST, Uri.Path("/user/create"), _, entity, _) =>
      val requestor = sender
      val userNode = entity.asString.parseJson.convertTo[UserNode]
      val future: Future[CreateFbNodeRsp] = (fbServer ? CreateFbNodeReq("user", userNode)).mapTo[CreateFbNodeRsp]
      future.onSuccess {
        case createFbNodeRsp: CreateFbNodeRsp =>
          requestor ! HttpResponse(entity = HttpEntity(ContentTypes.`application/json`, createFbNodeRsp.toJson.toString))
      }
  }
}

object FbHttpServer extends App {
  implicit val system = ActorSystem()

  IO(Http) ! Http.Bind(system.actorOf(Props[FbServerHttp], name = "FbServerHttp"), interface = "localhost", port = 8080)
}