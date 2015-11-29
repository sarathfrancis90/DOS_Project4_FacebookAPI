package client

import java.util.Calendar

import akka.actor.{ActorSystem, _}
import server.facebook._
import server.http.FbJsonProtocol
import spray.client.pipelining._
import spray.http._
import spray.json.{DefaultJsonProtocol, _}

import scala.collection.mutable.ListBuffer
import scala.concurrent.{Await, Future}
import scala.util.{Random, Failure, Success}
import scala.language.postfixOps
import scala.concurrent.duration._


case class StartCreatingUsers(numberOfUsers: Int)

case class DoneCreatingUser(userName: String, userId: String)

case class StartCreatingPages(numberOfPages: Int)

case class DoneCreatingPage(pageName: String, pageId: String)

case class StartUsersLikingPages(registeredUsers: List[(String, String)], registeredPages: List[(String, String)], percent: Int, average: Int)

case object DoneUserLikingPage

case class StartPostsForPages(registeredPages: List[(String, String)])

class Master extends Actor with ActorLogging {
  val totalUsersCount = 100000
  val totalPagesCount = 10000

  val percentageOfUsersWhoClickLike: Int = 64
  val averageNumberOfPagesLikedByAUser: Int = 40
  var totalNumberOfLikesToDo = 0
  var totalNumberOfLikesDone = 0

  var myRegisteredUsers: ListBuffer[(String, String)] = new ListBuffer[(String, String)]()
  var myRegisteredPages: ListBuffer[(String, String)] = new ListBuffer[(String, String)]()

  var createUsersActorRef: ActorRef = _
  var createPagesActorRef: ActorRef = _
  var usersLikingPagesActorRef: ActorRef = _
  var pagesMakingPostsActorRef: ActorRef = _

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
        usersLikingPagesActorRef = context.system.actorOf(Props(new UsersLikingPagesSubActor), "UsersLikingPages")
        usersLikingPagesActorRef ! StartUsersLikingPages(myRegisteredUsers.toList, myRegisteredPages.toList, percentageOfUsersWhoClickLike, averageNumberOfPagesLikedByAUser)
        totalNumberOfLikesToDo = myRegisteredUsers.length * percentageOfUsersWhoClickLike / 100 * averageNumberOfPagesLikedByAUser
      }

    case DoneUserLikingPage =>
      totalNumberOfLikesDone += 1
      if (totalNumberOfLikesDone == 1000) {
        log.info("Done liking " + totalNumberOfLikesDone + " pages")
//        usersLikingPagesActorRef ! "PleaseKillYourself"
        pagesMakingPostsActorRef = context.system.actorOf(Props(new PagesMakingPostsSubActor), "PagesMakingPosts")
        pagesMakingPostsActorRef ! StartPostsForPages(myRegisteredPages.toList)
      }

  }
}

class PagesMakingPostsSubActor extends Actor with ActorLogging {
  import FbJsonProtocol._
  implicit val system = ActorSystem()
  import system.dispatcher

  var registeredPages: List[(String, String)] = _
  var mySender: ActorRef = _

  def receive = {
    case StartPostsForPages(registeredPagesIn) =>
      mySender = sender
      registeredPages = registeredPagesIn
      makeAPost(registeredPages)
      Thread.sleep(10)
      self ! "Again"

    case "Again" =>
      makeAPost(registeredPages)
      Thread.sleep(10)
      self ! "Again"

    case "PleaseKillYourself" =>
      log.info(self.path.name + " says bye")
      context.stop(self)
  }

  def makeAPost(registeredPages: List[(String, String)]) = {
    val randomPageIndex = Random.nextInt(registeredPages.length)

    val now = Calendar.getInstance().getTime.toString

    val postNode = PostNode(
      id = "",
      created_time = now,
      description = "",
      from = registeredPages(randomPageIndex)._2,
      message = registeredPages(randomPageIndex)._1 + " posting a random number " + Random.nextInt(1000).toString + " at " + now,
      to = List.empty,
      updated_time = now)
    val createPagePostReq = CreatePagePostReq(
      pageId = registeredPages(randomPageIndex)._2,
      post = postNode)

    val entity = HttpEntity(contentType = ContentType(MediaTypes.`application/json`, HttpCharsets.`UTF-8`), createPagePostReq.toJson.toString)

    val pipeline: HttpRequest => Future[HttpResponse] = sendReceive

    val future: Future[HttpResponse] = pipeline(Post("http://127.0.0.1:8080/page/post", entity))
//    Await.result(future, 5 second)
  }
}

class UsersLikingPagesSubActor extends Actor with ActorLogging {
  import FbJsonProtocol._
  implicit val system = ActorSystem()
  import system.dispatcher

  var percentageOfUsersWhoClickLike: Int = 0
  var averageNumberOfPagesLikedByAUser: Int = 0

  def receive = {
    case StartUsersLikingPages(registeredUsers, registeredPages, percent, average) =>
      val mySender = sender

      percentageOfUsersWhoClickLike = percent
      averageNumberOfPagesLikedByAUser = average
      val totalNumberOfLikesToDo = registeredUsers.length * percentageOfUsersWhoClickLike / 100 * averageNumberOfPagesLikedByAUser

      (0 until totalNumberOfLikesToDo).foreach(i => {
        val addUserLikedPageReq = AddUserLikedPageReq(
          userId = registeredUsers(Random.nextInt(registeredUsers.length * percentageOfUsersWhoClickLike / 100))._2,
          pageName = registeredPages(Random.nextInt(registeredPages.length))._1)

        val entity = HttpEntity(contentType = ContentType(MediaTypes.`application/json`, HttpCharsets.`UTF-8`), addUserLikedPageReq.toJson.toString)

        val pipeline: HttpRequest => Future[HttpResponse] = sendReceive

        val future: Future[HttpResponse] = pipeline(Post("http://127.0.0.1:8080/like_this_page", entity))
        future onComplete {
          case Success(response) =>
            mySender ! DoneUserLikingPage

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

object FbSimulator {

  def main(args: Array[String]) {
    ActorSystem("dos-project-4").actorOf(Props(new Master), "Master") ! "Init"
  }

}
