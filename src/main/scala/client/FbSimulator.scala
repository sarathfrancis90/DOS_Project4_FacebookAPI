package client

import akka.actor.{ActorSystem, _}
import server.facebook.{PageNode, CreateFbNodeRsp, UserNode}
import spray.client.pipelining._
import spray.http._
import spray.json.{DefaultJsonProtocol, _}

import scala.collection.mutable.ListBuffer
import scala.concurrent.Future
import scala.util.{Failure, Success}

case class StartCreatingUsers(numberOfUsers: Int)

case class DoneCreatingUser(userName: String, userId: String)

case class StartCreatingPages(numberOfPages: Int)

case class DoneCreatingPage(pageName: String, pageId: String)

object FbJsonProtocol extends DefaultJsonProtocol {
  implicit val userNodeFormat = jsonFormat5(UserNode)
  implicit val pageNodeFormat = jsonFormat5(PageNode)
  implicit val createFbNodeRspFormat = jsonFormat2(CreateFbNodeRsp)
}

class Master extends Actor with ActorLogging {
  val totalUsersCount = 100
  val totalPagesCount = 100

  var myRegisteredUsers: ListBuffer[(String, String)] = new ListBuffer[(String, String)]()
  var myRegisteredPages: ListBuffer[(String, String)] = new ListBuffer[(String, String)]()

  var createUsersActorRef: ActorRef = _
  var createPagesActorRef: ActorRef = _

  def receive = {
    case "Init" =>
      log.info("Master init")
      createUsersActorRef = context.system.actorOf(Props(new CreateUsersSubActor), "CreateUsers")
      createUsersActorRef ! StartCreatingUsers(totalUsersCount)

    case DoneCreatingUser(userName, userId) =>
      val registeredUser = (userName, userId)
      myRegisteredUsers += registeredUser
      if (myRegisteredUsers.length == totalUsersCount) {
        log.info("Done creating " + myRegisteredUsers.length.toString + " users")
        createUsersActorRef ! "PleaseKillYourself"
        createPagesActorRef = context.system.actorOf(Props(new CreatePagesSubActor), "CreatePages")
        createPagesActorRef ! StartCreatingPages(totalPagesCount)
      }

    case DoneCreatingPage(pageName, pageId) =>
      val registeredPage = (pageName, pageId)
      myRegisteredPages += registeredPage
      if (myRegisteredPages.length == totalPagesCount) {
        log.info("Done creating " + myRegisteredPages.length.toString + " pages")
        createPagesActorRef ! "PleaseKillYourself"
      }

  }
}

class CreateUsersSubActor extends Actor with ActorLogging {

  import FbJsonProtocol._

  implicit val system = ActorSystem()

  import system.dispatcher

  def receive = {
    case StartCreatingUsers(numberOfUsers) =>
      val mySender = sender
      log.info(s"Creating $numberOfUsers users")

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
            mySender ! DoneCreatingUser(userNode.first_name, response.entity.asString.parseJson.convertTo[CreateFbNodeRsp].id)

          case Failure(error) =>
            println("Some error has occurred: " + error.getMessage)
        }
      })

    case "PleaseKillYourself" =>
      log.info(self.path.name + " says bye")
      context.stop(self)
  }
}

class CreatePagesSubActor extends Actor with ActorLogging {

  import FbJsonProtocol._

  implicit val system = ActorSystem()

  import system.dispatcher

  def receive = {
    case StartCreatingPages(numberOfPages) =>
      val mySender = sender
      log.info(s"Creating $numberOfPages pages")

      (0 until numberOfPages).foreach(i => {

        val pageNode = PageNode(
          id = "",
          about = "Something about page" + i.toString,
          description = "page" + i.toString + "'s description",
          name = "page" + i.toString,
          likes = 0)

        val entity = HttpEntity(contentType = ContentType(MediaTypes.`application/json`, HttpCharsets.`UTF-8`), pageNode.toJson.toString)

        val pipeline: HttpRequest => Future[HttpResponse] = sendReceive

        val future: Future[HttpResponse] = pipeline(Post("http://127.0.0.1:8080/page/create", entity))
        future onComplete {
          case Success(response) =>
            mySender ! DoneCreatingPage(pageNode.name, response.entity.asString.parseJson.convertTo[CreateFbNodeRsp].id)

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
