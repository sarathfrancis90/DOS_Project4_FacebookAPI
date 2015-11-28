package client

import akka.actor.{ActorSystem, _}
import server.facebook.{CreateFbNodeRsp, UserNode}
import spray.client.pipelining._
import spray.http._
import spray.json.{DefaultJsonProtocol, _}

import scala.collection.mutable.ListBuffer
import scala.concurrent.Future
import scala.util.{Failure, Success}

case class StartRegisteringUsers(numberOfUsers: Integer)

case class DoneRegisteringUser(userName: String, userId: String)

object FbJsonProtocol extends DefaultJsonProtocol {
  implicit val userNodeFormat = jsonFormat5(UserNode)
  implicit val createFbNodeRspFormat = jsonFormat2(CreateFbNodeRsp)
}

class Master extends Actor with ActorLogging {
  val totalUsersCount = 100000
  var myRegisteredUsers: ListBuffer[(String, String)] = new ListBuffer[(String, String)]()

  var registerUsersActorRef: ActorRef = _

  def receive = {
    case "Init" =>
      log.info("Master init")
      registerUsersActorRef = context.system.actorOf(Props(new RegisterUsers), "RegisterUsers")
      registerUsersActorRef ! StartRegisteringUsers(totalUsersCount)

    case DoneRegisteringUser(userName, userId) =>
      val registeredUser = (userName, userId)
      myRegisteredUsers += registeredUser
      if (myRegisteredUsers.length == totalUsersCount) {
        log.info("Done registering " + myRegisteredUsers.length.toString + " users")
        registerUsersActorRef ! "PleaseKillYourself"
      }
  }
}

class RegisterUsers extends Actor with ActorLogging {

  import FbJsonProtocol._

  implicit val system = ActorSystem()

  import system.dispatcher

  def receive = {
    case StartRegisteringUsers(numberOfUsers) =>
      val mySender = sender
      log.info(s"Registering $numberOfUsers users")

      (0 until numberOfUsers).foreach(i => {

        val userNode = UserNode("",
          "Something about " + "user" + i.toString,
          "01/01/1988",
          "user" + i.toString + "@ufl.edu",
          "user" + i.toString)

        val entity = HttpEntity(contentType = ContentType(MediaTypes.`application/json`, HttpCharsets.`UTF-8`), userNode.toJson.toString)

        val pipeline: HttpRequest => Future[HttpResponse] = sendReceive

        val future: Future[HttpResponse] = pipeline(Post("http://127.0.0.1:8080/user/create", entity))
        future onComplete {
          case Success(response) =>
            mySender ! DoneRegisteringUser(userNode.first_name, response.entity.asString.parseJson.convertTo[CreateFbNodeRsp].id)

          case Failure(error) =>
            println("Some error has occurred: " + error.getMessage)
        }
      })

    case "PleaseKillYourself" =>
      log.info(self.path.name + " says bye")
      context.stop(self)
  }
}

object FbSimulator {

  def main(args: Array[String]) {
    ActorSystem("dos-project-4").actorOf(Props(new Master), "Master") ! "Init"
  }

}
