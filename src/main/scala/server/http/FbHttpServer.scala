package server.http

import _root_.server.facebook._
import akka.actor._
import akka.io.IO
import akka.pattern.ask
import akka.util.Timeout
import spray.can.{Http, _}
import spray.http.HttpMethods._
import spray.http._
import spray.httpx.SprayJsonSupport
import spray.json._

import scala.collection.mutable.ListBuffer
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
  implicit object NodeFormat extends RootJsonFormat[Node] {
    def write(n: Node) = n match {
      case post: PostNode => post.toJson
      case photo: PhotoNode => photo.toJson
    }

    def read(value: JsValue) = value match {
      case _ =>
        throw new DeserializationException("Not supported")
    }
  }
  implicit val createFbNodeRspFormat = jsonFormat2(CreateFbNodeRsp)
  implicit val addUserLikedPageReqFormat = jsonFormat2(AddUserLikedPageReq)
  implicit val addUserLikedPageRspFormat = jsonFormat1(AddUserLikedPageRsp)
  implicit val createPagePostReqFormat = jsonFormat2(CreatePagePostReq)
  implicit val createPagePostRspFormat = jsonFormat1(CreatePagePostRsp)
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
        case result: CreateFbNodeRsp =>
          requestor ! HttpResponse(entity = HttpEntity(ContentTypes.`application/json`, result.toJson.toString))
      }

    case HttpRequest(POST, Uri.Path("/page/create"), _, entity, _) =>
      val requestor = sender
      val pageNode = entity.asString.parseJson.convertTo[PageNode]
      val future: Future[CreateFbNodeRsp] = (fbServer ? CreateFbNodeReq("page", pageNode)).mapTo[CreateFbNodeRsp]
      future.onSuccess {
        case result: CreateFbNodeRsp =>
          requestor ! HttpResponse(entity = HttpEntity(ContentTypes.`application/json`, result.toJson.toString))
      }

    case HttpRequest(POST, Uri.Path("/like_this_page"), _, entity, _) =>
      val requestor = sender
      val addUserLikedPageReq = entity.asString.parseJson.convertTo[AddUserLikedPageReq]
      val future: Future[AddUserLikedPageRsp] = (fbServer ? addUserLikedPageReq).mapTo[AddUserLikedPageRsp]
      future.onSuccess {
        case result: AddUserLikedPageRsp =>
          requestor ! HttpResponse(entity = HttpEntity(ContentTypes.`application/json`, result.toJson.toString()))
      }

    case HttpRequest(POST, Uri.Path("/page/post"), _, entity, _) =>
      val requestor = sender
      val createPagePostReq = entity.asString.parseJson.convertTo[CreatePagePostReq]
      val future: Future[CreatePagePostRsp] = (fbServer ? createPagePostReq).mapTo[CreatePagePostRsp]
      future.onSuccess {
        case result: CreatePagePostRsp =>
          requestor ! HttpResponse(entity = HttpEntity(ContentTypes.`application/json`, result.toJson.toString))
      }

    case HttpRequest(GET, Uri.Path(path), _, _, _) if path startsWith "/user/timeline" =>
      val requestor = sender
      val userId = path.split('/').last
      val getUserTimelineReq = GetUserTimelineReq(
        userId = userId,
        startFrom = "",
        limit = 0)
      val future: Future[GetUserTimelineRsp] = (fbServer ? getUserTimelineReq).mapTo[GetUserTimelineRsp]
      future.onSuccess {
        case result: GetUserTimelineRsp =>
          val eventsOnly: ListBuffer[Node] = new ListBuffer[Node]()
          result.events.foreach(event => {
            eventsOnly += event._2
          })
          requestor ! HttpResponse(entity = HttpEntity(ContentTypes.`application/json`, eventsOnly.toList.take(10).toJson.toString))
      }
  }
}

object FbHttpServer extends App {
  implicit val system = ActorSystem()

  IO(Http) ! Http.Bind(system.actorOf(Props[FbServerHttp], name = "FbServerHttp"), interface = "localhost", port = 8080)
}