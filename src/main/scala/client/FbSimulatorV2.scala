package client

import akka.actor.{ActorSystem, _}
import server.facebook._
import server.http.FbJsonProtocol
import spray.client.pipelining._
import spray.http._
import spray.json._

import scala.collection.mutable.ListBuffer
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.language.postfixOps
import scala.util.{Failure, Success}

import java.security._
import java.security.spec.X509EncodedKeySpec
import java.util.Base64
import javax.crypto.spec.SecretKeySpec
import javax.crypto.{Cipher, KeyGenerator, SecretKey}

case class SignUpUser(someUniqueId: Int)

case class RegisteredUsersList(registeredUsersList: List[(String, String, ActorRef)])

case class DoneCreatingUser(userName: String, userId: String)

class Master extends Actor with ActorLogging {

  var fbUsers: ListBuffer[ActorRef] = new ListBuffer[ActorRef]()
  var fbPages: ListBuffer[ActorRef] = new ListBuffer[ActorRef]()

  val totalUsersCount = 50
  val totalPagesCount = 100

  val percentageOfUsersWhoClickLike: Int = 64
  val averageNumberOfPagesLikedByAUser: Int = 40
  val percentageofActiveUsers: Double = 58

  var myRegisteredUsers: ListBuffer[(String, String, ActorRef)] = new ListBuffer[(String, String, ActorRef)]()
  var myRegisteredPages: ListBuffer[(String, String)] = new ListBuffer[(String, String)]()

  def receive = {

    case "MasterInit" =>
      self ! "CreateUsers"
    //      self ! "CreatePages"

    case "CreateUsers" =>
      for (i <- 0 until totalUsersCount) {
        fbUsers += ActorSystem("dos-project-4").actorOf(Props(new FbUser), name = i.toString)
      }
      for (i <- 0 until fbUsers.length) {
        fbUsers(i) ! SignUpUser(i)
      }

    case DoneCreatingUser(userName, userId) =>
      val registeredUser = (userName, userId, sender)
      myRegisteredUsers += registeredUser
      if (myRegisteredUsers.length == totalUsersCount) {
        log.info("Done creating " + myRegisteredUsers.length.toString + " users")
        for (i <- 1 until fbUsers.length) {
          fbUsers(i) ! RegisteredUsersList(myRegisteredUsers.toList)
        }
      }

    case "CreatePages" =>
      for (i <- 1 until totalPagesCount) {
        fbPages += ActorSystem("dos-project-4").actorOf(Props(new FbPage), name = i.toString)
        fbPages(i) ! "RegisterPage"
      }
      for (fbPage <- fbPages) {
        fbPage ! "RegisterPage"
      }
  }
}

class FbUser extends Actor with ActorLogging {

  import FbJsonProtocol._
  implicit val system = ActorSystem("dos-project-4")
  import system.dispatcher

  val keyPairGeneratorRsa = KeyPairGenerator.getInstance("RSA")
  val keyGeneratorAes = KeyGenerator.getInstance("AES")
  val secureRandom = SecureRandom.getInstance("SHA1PRNG")
  val cipher = Cipher.getInstance("RSA")
  val cipherAes = Cipher.getInstance("AES")
  val signature = Signature.getInstance("SHA1withRSA")

  var mySender: ActorRef = _
  var myUserId: String = _
  var myUserName: Int = _
  var keyPair: KeyPair = _
  var keyAes: SecretKey = _

  var registeredUsersList: List[(String, String, ActorRef)] = _

  def receive = {

    case SignUpUser(uniqueId) =>
      mySender = sender
      myUserName = uniqueId
      keyPairGeneratorRsa.initialize(1024, secureRandom)
      keyPair = keyPairGeneratorRsa.genKeyPair()

      val userNode = UserNode(
        id = "",
        about = "Something about " + "user" + uniqueId.toString,
        birthday = "01/01/1988",
        email = "user" + uniqueId.toString + "@ufl.edu",
        first_name = "user" + uniqueId.toString,
        public_key = new String(Base64.getEncoder.encode(keyPair.getPublic.getEncoded))
      )

      val entity = HttpEntity(contentType = ContentType(MediaTypes.`application/json`, HttpCharsets.`UTF-8`), userNode.toJson.toString)

      val pipeline: HttpRequest => Future[HttpResponse] = sendReceive

      val future: Future[HttpResponse] = pipeline(Post("http://127.0.0.1:8080/user/create", entity))
      future onComplete {
        case Success(response) =>
          myUserId = response.entity.asString.parseJson.convertTo[CreateUserRsp].id
          mySender ! DoneCreatingUser(userNode.first_name, myUserId)

        case Failure(error) =>
          println("Some error has occurred: " + error.getMessage)
      }

    case RegisteredUsersList(registeredUserList) =>
      registeredUsersList = registeredUserList
      self ! "AddFriends"

    case "AddFriends" =>
      for (i <- myUserName + 5 until registeredUsersList.length by 5) {

        val addFriendReq = AddFriendReq(
          userId = myUserId,
          friendName = "user" + i.toString
        )

        val entity = HttpEntity(contentType = ContentType(MediaTypes.`application/json`, HttpCharsets.`UTF-8`), addFriendReq.toJson.toString)

        val pipeline: HttpRequest => Future[HttpResponse] = sendReceive

        val future: Future[HttpResponse] = pipeline(Post("http://127.0.0.1:8080/user/add_friend_request", entity))
        future onComplete {
          case Success(response) =>
          //            println(response.entity.asString.parseJson.convertTo[AddFriendRsp].result)

          case Failure(error) =>
            println("Some error has occurred: " + error.getMessage)
        }

      }
      self ! "GetPendingInFriendRequests"

    case "GetPendingInFriendRequests" =>

      val pipeline: HttpRequest => Future[HttpResponse] = sendReceive

      val future: Future[HttpResponse] = pipeline(Get(s"http://127.0.0.1:8080/user/pending_in_friend_requests/$myUserId"))
      val response = Await.result(future, 5 second)
      val pendingInFriendRequests = response.entity.asString.parseJson.convertTo[List[String]]

      //      println("Incoming friendRequests at the User :" + myUserName)
      //      pendingInFriendRequests.foreach(println(_))
      pendingInFriendRequests.foreach(pendingInFriendRequest => {
        val addFriendReq = AddFriendReq(
          userId = myUserId,
          friendName = pendingInFriendRequest
        )
        val entity = HttpEntity(contentType = ContentType(MediaTypes.`application/json`, HttpCharsets.`UTF-8`), addFriendReq.toJson.toString)

        val pipeline: HttpRequest => Future[HttpResponse] = sendReceive

        val future: Future[HttpResponse] = pipeline(Post("http://127.0.0.1:8080/user/add_friend_request", entity))
        future onComplete {
          case Success(response) =>
            println(response.entity.asString.parseJson.convertTo[AddFriendRsp].result)

          case Failure(error) =>
            println("Some error has occurred: " + error.getMessage)
        }

      })

      val totalTimeDuration = Duration(2000, "millis")
      context.system.scheduler.scheduleOnce(totalTimeDuration, self, "GetPendingInFriendRequests")

      context.system.scheduler.scheduleOnce(totalTimeDuration, self, "GetFriendsList")

    case "GetFriendsList" =>
      val pipeline: HttpRequest => Future[HttpResponse] = sendReceive

      val future: Future[HttpResponse] = pipeline(Get(s"http://127.0.0.1:8080/user/get_friends/$myUserId"))
      val response = Await.result(future, 5 second)
//      val FriendsList = response.entity.asString.parseJson.convertTo[List[Node]]

      println("Friends for the User :" + myUserName)
      println (response.entity.asString.parseJson.prettyPrint)
//      FriendsList.foreach(println(_))
      Thread.sleep(1000)
  }
}

class FbPage extends Actor with ActorLogging {

  def receive = {
    case "RegisterPage" =>
  }

}

object FbSimulatorV2 {

  def main(args: Array[String]) {
    ActorSystem("dos-project-4").actorOf(Props(new Master), "Master") ! "MasterInit"
  }

}
