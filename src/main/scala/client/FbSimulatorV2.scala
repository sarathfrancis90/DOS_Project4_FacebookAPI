package client

import java.util

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
import scala.util.{Random, Failure, Success}
import java.security._
import java.security.spec.X509EncodedKeySpec
import java.util.{Calendar, Base64}
import javax.crypto.spec.SecretKeySpec
import javax.crypto.{Cipher, KeyGenerator, SecretKey}


case class SignUpUser(someUniqueId: Int)

case class RegisterPage(someUniqueId: Int)

case class StartUserActivities(registeredUsersList: List[(String, String, ActorRef)])

case class StartPageActivities(registeredPagesList: List[(String, String, ActorRef)])


case class DoneCreatingUser(userName: String, userId: String)

case class DoneCreatingPage(pageName: String, pageId: String)


class Master extends Actor with ActorLogging {

  var fbUsers: ListBuffer[ActorRef] = new ListBuffer[ActorRef]()
  var fbPages: ListBuffer[ActorRef] = new ListBuffer[ActorRef]()
  val totalUsersCount = 50
  val totalPagesCount = 100
  val percentageOfUsersWhoClickLike: Int = 64
  val averageNumberOfPagesLikedByAUser: Int = 40
  val percentageOfActiveUsers: Double = 58
  var myRegisteredUsers: ListBuffer[(String, String, ActorRef)] = new ListBuffer[(String, String, ActorRef)]()
  var myRegisteredPages: ListBuffer[(String, String, ActorRef)] = new ListBuffer[(String, String, ActorRef)]()

  def receive = {

    case "MasterInit" =>
      self ! "CreateUsers"

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
          fbUsers(i) ! StartUserActivities(myRegisteredUsers.toList)
        }
      }
    case DoneCreatingPage(pageName, pageId) =>
      val registeredPage = (pageName, pageId, sender)
      myRegisteredPages += registeredPage
      if (myRegisteredPages.length == totalPagesCount) log.info("Done creating " + myRegisteredPages.length.toString + " pages")
    case "CreatePages" =>
      for (i <- 1 until totalPagesCount) {
        fbPages += ActorSystem("dos-project-4").actorOf(Props(new FbPage), name = i.toString)
        fbPages(i) ! "RegisterPage"
      }
      for (i <- 0 until fbPages.length) {
        fbPages(i) ! RegisterPage(i)
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
  val cipherRsa = Cipher.getInstance("RSA")
  val cipherAes = Cipher.getInstance("AES")
  val signature = Signature.getInstance("SHA1withRSA")

  var mySender: ActorRef = _
  var myUserId: String = _
  var myUserName: Int = _
  var keyPair: KeyPair = _
  var keyAes: SecretKey = _

  var registeredUsersList: List[(String, String, ActorRef)] = _

  val getPipeline: HttpRequest => Future[HttpResponse] = sendReceive
  val postPipeline: HttpRequest => Future[HttpResponse] = sendReceive

  var myFriendList: ListBuffer[(String)] = _

  def receive = {

    case SignUpUser(uniqueId) =>
      mySender = sender
      myUserName = uniqueId
      keyPairGeneratorRsa.initialize(1024, secureRandom)
      keyPair = keyPairGeneratorRsa.genKeyPair()

      val userNode = UserNode(
        id = "",
        about = "Something about " + "user" + myUserName.toString,
        birthday = "01/01/1988",
        email = "user" + myUserName.toString + "@ufl.edu",
        first_name = "user" + myUserName.toString,
        public_key = new String(Base64.getEncoder.encode(keyPair.getPublic.getEncoded))
      )

      val entity = HttpEntity(contentType = ContentType(MediaTypes.`application/json`, HttpCharsets.`UTF-8`), userNode.toJson.toString)

      val future: Future[HttpResponse] = postPipeline(Post("http://127.0.0.1:8080/user/create", entity))
      future onComplete {
        case Success(response) =>
          myUserId = response.entity.asString.parseJson.convertTo[CreateUserRsp].id
          mySender ! DoneCreatingUser(userNode.first_name, myUserId)

        case Failure(error) =>
          println("Some error has occurred: " + error.getMessage)
      }

    case StartUserActivities(registeredUserList) =>
      registeredUsersList = registeredUserList

      self ! "SendFriendRequests"

    case "DoneSendingFriendRequests" =>

      self ! "AcceptPendingInFriendRequests"
      Thread.sleep(1000)
      self ! "CreateUserPost"

    case "CreateUserPost" =>

      val now = Calendar.getInstance().getTime.toString

      val getFriendListFuture: Future[HttpResponse] = getPipeline(Get(s"http://127.0.0.1:8080/user/get_friends/$myUserId"))
      val getFriendListFutureResponse = Await.result(getFriendListFuture, 5 second)
      val friends = getFriendListFutureResponse.entity.asString.parseJson.convertTo[List[UserNode]]
      val encryptedAesKeys: ListBuffer[EncryptedSecretKey] = new ListBuffer[EncryptedSecretKey]()

      val plainTextPostMessage = myUserName + " posting a message " + " at " + now

      keyAes = keyGeneratorAes.generateKey()
      cipherAes.init(Cipher.ENCRYPT_MODE, keyAes)
      val encryptedPostMessageAsString = new String(Base64.getEncoder.encode(cipherAes.doFinal(plainTextPostMessage.getBytes)))

      for (i <- friends.indices by 5) {
        val publicKeyOfFriend: PublicKey =
          KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(
            Base64.getDecoder.decode(friends(i).public_key.getBytes)))
        cipherRsa.init(Cipher.ENCRYPT_MODE, publicKeyOfFriend)
        val encryptedAesKeyAsString = new String(Base64.getEncoder.encode(cipherRsa.doFinal(keyAes.getEncoded)))

        val encryptedPrivateKey = EncryptedSecretKey(
        to = friends(i).first_name,
          encrypted_secret_key = encryptedAesKeyAsString
        )
        encryptedAesKeys += encryptedPrivateKey
      }

      val postNodeV2 = PostNodeV2(
        id = "",
        created_time = now,
        description = "",
        from = myUserId,
        encrypted_message = encryptedPostMessageAsString,
        encrypted_secret_keys = encryptedAesKeys.toList,
        to = List.empty,
        updated_time = now
      )

      val createUserPostReqV2 = CreateUserPostReqV2(
        userId = myUserId,
        postV2 = postNodeV2
      )
      val entity = HttpEntity(contentType = ContentType(MediaTypes.`application/json`, HttpCharsets.`UTF-8`), createUserPostReqV2.toJson.toString)

      val future: Future[HttpResponse] = postPipeline(Post("http://127.0.0.1:8080/user/postv2", entity))
      future onComplete {
        case Success(response) =>

        case Failure(error) =>
          println("Some error has occurred: " + error.getMessage)
      }


    case "SendFriendRequests" =>
      for (i <- myUserName + 5 until registeredUsersList.length by 5) {

        val addFriendReq = AddFriendReq(
          userId = myUserId,
          friendName = "user" + i.toString
        )

        val entity = HttpEntity(contentType = ContentType(MediaTypes.`application/json`, HttpCharsets.`UTF-8`), addFriendReq.toJson.toString)

        val future: Future[HttpResponse] = postPipeline(Post("http://127.0.0.1:8080/user/add_friend_request", entity))
        future onComplete {
          case Success(response) =>
          //            println(response.entity.asString.parseJson.convertTo[AddFriendRsp].result)

          case Failure(error) =>
            println("Some error has occurred: " + error.getMessage)
        }

      }
      self ! "DoneSendingFriendRequests"

    case "AcceptPendingInFriendRequests" =>

      val future: Future[HttpResponse] = getPipeline(Get(s"http://127.0.0.1:8080/user/pending_in_friend_requests/$myUserId"))
      val response = Await.result(future, 5 second)
      val pendingInFriendRequests = response.entity.asString.parseJson.convertTo[List[String]]
      pendingInFriendRequests.foreach(pendingInFriendRequest => {
        val addFriendReq = AddFriendReq(
          userId = myUserId,
          friendName = pendingInFriendRequest
        )
        val entity = HttpEntity(contentType = ContentType(MediaTypes.`application/json`, HttpCharsets.`UTF-8`), addFriendReq.toJson.toString)
        val future: Future[HttpResponse] = postPipeline(Post("http://127.0.0.1:8080/user/add_friend_request", entity))
        future onComplete {
          case Success(response) =>
            println(response.entity.asString.parseJson.convertTo[AddFriendRsp].result)

          case Failure(error) =>
            println("Some error has occurred: " + error.getMessage)
        }
      })
    //      val totalTimeDuration = Duration(2000, "millis")
    //      context.system.scheduler.scheduleOnce(totalTimeDuration, self, "CreateUserPost")


    case "GetFriendsList" =>

      val future: Future[HttpResponse] = getPipeline(Get(s"http://127.0.0.1:8080/user/get_friends/$myUserId"))
      val response = Await.result(future, 5 second)
      val friends = response.entity.asString.parseJson.convertTo[List[UserNode]]
      println("***Friends for the user: " + myUserName)
      friends.foreach(friend => {

        myFriendList += friend.first_name
        //        println (friend.first_name)
      })
  }
}

class FbPage extends Actor with ActorLogging {

  import FbJsonProtocol._

  implicit val system = ActorSystem("dos-project-4")

  import system.dispatcher

  var mySender: ActorRef = _
  var myPageId: String = _
  var myPageName: Int = _

  var registeredUsersList: List[(String, String, ActorRef)] = _

  val getPipeline: HttpRequest => Future[HttpResponse] = sendReceive
  val postPipeline: HttpRequest => Future[HttpResponse] = sendReceive

  def receive = {

    case RegisterPage(uniqueId) =>

      mySender = sender
      myPageName = uniqueId


      val pageNode = PageNode(
        id = "",
        about = "Something about page" + myPageName.toString,
        description = "page" + myPageName.toString + "'s description",
        name = "page" + myPageName.toString,
        likes = 0)

      val entity = HttpEntity(contentType = ContentType(MediaTypes.`application/json`, HttpCharsets.`UTF-8`), pageNode.toJson.toString)
      val future: Future[HttpResponse] = postPipeline(Post("http://127.0.0.1:8080/page/create", entity))

      future onComplete {
        case Success(response) =>
          mySender ! DoneCreatingPage(pageNode.name, response.entity.asString.parseJson.convertTo[CreateFbNodeRsp].id)

        case Failure(error) =>
          println("Some error has occurred: " + error.getMessage)
      }
  }
}

object FbSimulatorV2 {

  def main(args: Array[String]) {
    ActorSystem("dos-project-4").actorOf(Props(new Master), "Master") ! "MasterInit"
  }

}
