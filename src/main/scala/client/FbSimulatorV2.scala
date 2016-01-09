package client

import java.security._
import java.security.spec.X509EncodedKeySpec
import java.util.{Base64, Calendar}
import javax.crypto.spec.{GCMParameterSpec, SecretKeySpec}
import javax.crypto.{Cipher, KeyGenerator, SecretKey}

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


case class SignUpUser(someUniqueId: Int)

case class RegisterPage(someUniqueId: Int)

case class StartUserActivities(registeredUsersList: List[(String, String, ActorRef)])

case class StartPageActivities(registeredPagesList: List[(String, String, ActorRef)], registeredUsersList: List[(String, String, ActorRef)])


case class DoneCreatingUser(userName: String, userId: String)

case class DoneRegisteringPage(pageName: String, pageId: String)


class Master extends Actor with ActorLogging {

  var fbUsers: ListBuffer[ActorRef] = new ListBuffer[ActorRef]()
  var fbPages: ListBuffer[ActorRef] = new ListBuffer[ActorRef]()
  val totalUsersCount = 50
  val totalPagesCount = 20
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
        self ! "CreatePages"
        log.info("Done creating " + myRegisteredUsers.length.toString + " users")
        for (i <- 1 until fbUsers.length) {
          fbUsers(i) ! StartUserActivities(myRegisteredUsers.toList)
        }
      }
    case DoneRegisteringPage(pageName, pageId) =>
      val registeredPage = (pageName, pageId, sender)
      myRegisteredPages += registeredPage
      if (myRegisteredPages.length == totalPagesCount) {
        log.info("Done creating " + myRegisteredPages.length.toString + " pages")
        for (i <- 1 until fbPages.length) {
          fbUsers(i) ! StartPageActivities(myRegisteredPages.toList, myRegisteredUsers.toList)
        }
      }

    case "CreatePages" =>
      for (i <- 0 until totalPagesCount) {
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

  // for this user's Rsa keys
  val keyPairGeneratorRsa = KeyPairGenerator.getInstance("RSA")

  // for this user's "special" key for posts to all friends
  // and keys for posts to select set of friends
  val keyGeneratorAes = KeyGenerator.getInstance("AES")

  val secureRandom = SecureRandom.getInstance("SHA1PRNG")

  val cipherRsa = Cipher.getInstance("RSA")
  val cipherAes = Cipher.getInstance("AES")
  val cipherAesWithGcm = Cipher.getInstance("AES/GCM/NoPadding")

  val signatureInst = Signature.getInstance("SHA1withRSA")

  var mySender: ActorRef = _
  var myUserId: String = _
  var myUserName: Int = _
  var myRsaKeyPair: KeyPair = _
  var myAesKey: SecretKey = _
  var myPostCount: Long = 0

  var registeredUsersList: List[(String, String, ActorRef)] = _

  val getPipeline: HttpRequest => Future[HttpResponse] = sendReceive
  val postPipeline: HttpRequest => Future[HttpResponse] = sendReceive

  var myFriendList: ListBuffer[(String)] = _
  var postType: String = "to_select_friends"

  val postTemplate = PostNode(
    id = "",
    created_time = "",
    description = "",
    from = "",
    from_name = "",
    message = "",
    encrypted_secret_keys = List.empty,
    to = List.empty,
    updated_time = "",
    encrypted = false,
    to_all_friends = false,
    message_iv = "",
    signature = ""
  )

  def receive = {

    // this is also an init for this user
    case SignUpUser(uniqueId) =>
      mySender = sender
      myUserName = uniqueId

      // this user's Rsa keys
      keyPairGeneratorRsa.initialize(1024, secureRandom)
      myRsaKeyPair = keyPairGeneratorRsa.genKeyPair()

      // this user's aes keys
      keyGeneratorAes.init(128, secureRandom)
      myAesKey = keyGeneratorAes.generateKey()

      val userNode = UserNode(
        id = "",
        about = "Something about " + "user" + myUserName.toString,
        birthday = "01/01/1988",
        email = "user" + myUserName.toString + "@ufl.edu",
        first_name = "user" + myUserName.toString,
        public_key = new String(Base64.getEncoder.encode(myRsaKeyPair.getPublic.getEncoded)),
        encrypted_special_key = ""
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

    case "SendFriendRequests" =>
      for (i <- myUserName + 5 until registeredUsersList.length by 5) {
        val addFriendReq = AddFriendReq(
          userId = myUserId,
          friendName = "user" + i.toString
        )

        val entity = HttpEntity(contentType = ContentType(MediaTypes.`application/json`, HttpCharsets.`UTF-8`), addFriendReq.toJson.toString)

        val future: Future[HttpResponse] = postPipeline(Post("http://127.0.0.1:8080/user/add_friend_request", entity))
        future onFailure {
          case error =>
            println("Some error has occurred: " + error.getMessage)
        }

      }

      self ! "DoneSendingFriendRequests"

    case "DoneSendingFriendRequests" =>
      context.system.scheduler.scheduleOnce(Duration(5000, "millis"), self, "AcceptPendingInFriendRequests")
      context.system.scheduler.scheduleOnce(Duration(15000, "millis"), self, "AcceptPendingInFriendRequests")

      context.system.scheduler.scheduleOnce(Duration(20000, "millis"), self, "CreateUserPost")

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
        future onFailure {
          case error =>
            println("Some error has occurred: " + error.getMessage)
        }
      })

      context.system.scheduler.scheduleOnce(Duration(2500, "millis"), self, "ShareAesKey")

    case "ShareAesKey" =>
      val future: Future[HttpResponse] = getPipeline(Get(s"http://127.0.0.1:8080/user/get_friends/$myUserId"))
      val response = Await.result(future, 5 second)
      val friends = response.entity.asString.parseJson.convertTo[List[UserNode]]
      friends.foreach(friend => {

        val publicKeyOfFriend: PublicKey = KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(Base64.getDecoder.decode(friend.public_key.getBytes)))

        cipherRsa.init(Cipher.ENCRYPT_MODE, publicKeyOfFriend)
        val myEncryptedAesKeyAsString = new String(Base64.getEncoder.encode(cipherRsa.doFinal(myAesKey.getEncoded)))

        val addSpecialKeyToFriendReq = AddSpecialKeyToFriendReq(
          userId = myUserId,
          friendName = friend.first_name,
          encrypted_special_key = myEncryptedAesKeyAsString
        )

        val entity = HttpEntity(contentType = ContentType(MediaTypes.`application/json`, HttpCharsets.`UTF-8`), addSpecialKeyToFriendReq.toJson.toString)
        postPipeline(Post("http://127.0.0.1:8080/user/add_special_key", entity)) onFailure {
          case error =>
            println("Some error has occurred: " + error.getMessage)
        }
      })

    case "CreateUserPost" =>
      myPostCount += 1

      postType match {
        case "to_select_friends" =>
          makeAPostToSelectFriends
          postType = "to_all_friends"

        case "to_all_friends" =>
          makeAPostToAllFriends
          postType = "to_select_friends"

      }

      context.system.scheduler.scheduleOnce(Duration(20, "millis"), self, "CreateUserPost")
      context.system.scheduler.scheduleOnce(Duration(1000, "millis"), self, "DoneCreatingPost")

    case "DoneCreatingPost" =>
      self ! "ViewOwnPosts"
      self ! "ViewTaggedPosts"

    case "ViewTaggedPosts" =>
      val viewTaggedPostFuture: Future[HttpResponse] = getPipeline(Get(s"http://127.0.0.1:8080/user/tagged_posts/$myUserId"))
      val viewTaggedPostFutureResponse = Await.result(viewTaggedPostFuture, 5 second)
      val taggedPosts = viewTaggedPostFutureResponse.entity.asString.parseJson.convertTo[List[PostNode]]

      taggedPosts.foreach(taggedPost => {

        if (taggedPost.encrypted) {
          if (taggedPost.to_all_friends) {

            val postOwnerName = taggedPost.from_name
            val future: Future[HttpResponse] = getPipeline(Get(s"http://127.0.0.1:8080/user/get_friend_details/$myUserId/$postOwnerName"))
            val getFriendDetailsRsp = Await.result(future, 5 second)
            val postOwner = getFriendDetailsRsp.entity.asString.parseJson.convertTo[GetFriendDetailsRsp].friendNode

            cipherRsa.init(Cipher.DECRYPT_MODE, myRsaKeyPair.getPrivate)
            val aesKeyForPost: SecretKey = new SecretKeySpec(cipherRsa.doFinal(Base64.getDecoder.decode(postOwner.encrypted_special_key.getBytes)), "AES")
            cipherAesWithGcm.init(Cipher.DECRYPT_MODE, aesKeyForPost, new GCMParameterSpec(128, Base64.getDecoder.decode(taggedPost.message_iv.getBytes)))

            val post = new String(cipherAesWithGcm.doFinal(Base64.getDecoder.decode(taggedPost.message.getBytes)))
            println("Tagged post to all friends - " + post)

          }
          else {

            cipherRsa.init(Cipher.DECRYPT_MODE, myRsaKeyPair.getPrivate)
            val aesKeyForThisPost = new SecretKeySpec(cipherRsa.doFinal(Base64.getDecoder.decode(taggedPost.encrypted_secret_keys.find(x => {
              x.to == "user" + myUserName.toString
            }).get.encrypted_secret_key.getBytes)), "AES")
            cipherAes.init(Cipher.DECRYPT_MODE, aesKeyForThisPost)

            val post = new String(cipherAes.doFinal(Base64.getDecoder.decode(taggedPost.message.getBytes)))
            println("Tagged post to select friends - " + post)

          }
        }
        else {
          println(taggedPost.message)
        }
      })

      println("")
      context.system.scheduler.scheduleOnce(Duration(1000, "millis"), self, "ViewTaggedPosts")

    case "ViewOwnPosts" =>
      val viewOwnPostFuture: Future[HttpResponse] = getPipeline(Get(s"http://127.0.0.1:8080/user/own_posts/$myUserId"))
      val viewOwnPostFutureResponse = Await.result(viewOwnPostFuture, 5 second)
      val ownPosts = viewOwnPostFutureResponse.entity.asString.parseJson.convertTo[List[PostNode]]

      ownPosts.foreach(ownPost => {

        if (ownPost.encrypted) {
          if (ownPost.to_all_friends) {

            cipherAesWithGcm.init(Cipher.DECRYPT_MODE, myAesKey,new GCMParameterSpec(128, Base64.getDecoder.decode(ownPost.message_iv.getBytes)))
            val post = new String(cipherAesWithGcm.doFinal(Base64.getDecoder.decode(ownPost.message.getBytes)))
            println("Own post to all friends - " + post)

          }
          else {

            cipherRsa.init(Cipher.DECRYPT_MODE, myRsaKeyPair.getPrivate)
            val aesKeyForThisPost = new SecretKeySpec(cipherRsa.doFinal(Base64.getDecoder.decode(ownPost.encrypted_secret_keys.find(x => {
              x.to == "self"
            }).get.encrypted_secret_key.getBytes)), "AES")
            cipherAes.init(Cipher.DECRYPT_MODE, aesKeyForThisPost)

            val post = new String(cipherAes.doFinal(Base64.getDecoder.decode(ownPost.message.getBytes)))
            println("Own post to select friends - " + post)
          }
        }
        else {
          println(ownPost.message)
        }
      })

      println("")
      context.system.scheduler.scheduleOnce(Duration(1000, "millis"), self, "ViewOwnPosts")
  }

  def now = {
    Calendar.getInstance().getTime.toString
  }

  def makeAPostToSelectFriends = {
    val plainPostMessage = myUserName + " posting message#" + myPostCount.toString + " at " + now

    val keyAes = keyGeneratorAes.generateKey()
    cipherAes.init(Cipher.ENCRYPT_MODE, keyAes)
    val encryptedPostMessageAsString = new String(Base64.getEncoder.encode(cipherAes.doFinal(plainPostMessage.getBytes)))

    //
    signatureInst.initSign(myRsaKeyPair.getPrivate)
    signatureInst.update(encryptedPostMessageAsString.getBytes)
    val realSignature = new String(Base64.getEncoder.encode(signatureInst.sign()))
    //

    val encryptedSecretKeys: ListBuffer[EncryptedSecretKey] = new ListBuffer[EncryptedSecretKey]()
    cipherRsa.init(Cipher.ENCRYPT_MODE, myRsaKeyPair.getPublic)
    encryptedSecretKeys += EncryptedSecretKey(to = "self", encrypted_secret_key = new String(Base64.getEncoder.encode(cipherRsa.doFinal(keyAes.getEncoded))))

    val getFriendListFuture: Future[HttpResponse] = getPipeline(Get(s"http://127.0.0.1:8080/user/get_friends/$myUserId"))
    val getFriendListFutureResponse = Await.result(getFriendListFuture, 5 second)
    val friends = getFriendListFutureResponse.entity.asString.parseJson.convertTo[List[UserNode]]

    for (i <- friends.indices by 5) {
      val publicKeyOfFriend = KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(Base64.getDecoder.decode(friends(i).public_key.getBytes)))
      cipherRsa.init(Cipher.ENCRYPT_MODE, publicKeyOfFriend)
      encryptedSecretKeys += EncryptedSecretKey(to = friends(i).first_name, encrypted_secret_key = new String(Base64.getEncoder.encode(cipherRsa.doFinal(keyAes.getEncoded))))
    }

    val createUserPostReq = CreateUserPostReq(
      userId = myUserId,
      post = postTemplate.copy(
        from = myUserId,
        message = encryptedPostMessageAsString,
        encrypted_secret_keys = encryptedSecretKeys.toList,
        encrypted = true,
        to_all_friends = false,
        signature = realSignature)
    )
    val entity = HttpEntity(contentType = ContentType(MediaTypes.`application/json`, HttpCharsets.`UTF-8`), createUserPostReq.toJson.toString)

    postPipeline(Post("http://127.0.0.1:8080/user/post", entity)) onFailure {
      case error =>
        println("Some error has occurred: " + error.getMessage)
    }
  }

  def makeAPostToAllFriends = {
    val plainPostMessage = myUserName + " posting message#" + myPostCount.toString + " to all friends at " + now
    cipherAesWithGcm.init(Cipher.ENCRYPT_MODE, myAesKey, secureRandom)
    val encryptedPostMessageAsString = new String(Base64.getEncoder.encode(cipherAesWithGcm.doFinal(plainPostMessage.getBytes)))

    signatureInst.initSign(myRsaKeyPair.getPrivate)
    signatureInst.update(encryptedPostMessageAsString.getBytes)
    val realSignature = new String(Base64.getEncoder.encode(signatureInst.sign()))

    val createUserPostReq = CreateUserPostReq(
      userId = myUserId,
      post = postTemplate.copy(
        from = myUserId,
        message = encryptedPostMessageAsString,
        encrypted = true,
        to_all_friends = true,
        message_iv = new String(Base64.getEncoder.encode(cipherAesWithGcm.getIV)),
        signature = realSignature)
    )

    val entity = HttpEntity(contentType = ContentType(MediaTypes.`application/json`, HttpCharsets.`UTF-8`), createUserPostReq.toJson.toString)

    postPipeline(Post("http://127.0.0.1:8080/user/post", entity)) onFailure {
      case error =>
        println("Some error has occurred: " + error.getMessage)
    }
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
  var registeredPagesList: List[(String, String, ActorRef)] = _


  val getPipeline: HttpRequest => Future[HttpResponse] = sendReceive
  val postPipeline: HttpRequest => Future[HttpResponse] = sendReceive

  def receive = {


    case StartPageActivities(registeredPagesIn, registeredUsersIn) =>
      log.info("Pages starting to create posts and share photos")
      mySender = sender
      registeredPagesList = registeredPagesIn
      registeredUsersList = registeredUsersIn
      self ! "CreatepagePost"

    case "DoneCreatingPagePost" =>
      self ! "PostAPhotoByPage"
    //      Thread.sleep(10)

    case "DonePostingPhotoFrompage" =>
      //      Thread.sleep(10)
      self ! "CreatepagePost"

    case "CreatepagePost" =>

      val now = Calendar.getInstance().getTime.toString

      self ! "DoneCreatingPagePost"

    case "PostAPhotoByPage" =>

      self ! "DonePostingPhotoFrompage"


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
          myPageId = response.entity.asString.parseJson.convertTo[CreateFbNodeRsp].id
          mySender ! DoneRegisteringPage(pageNode.name, myPageId)

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
