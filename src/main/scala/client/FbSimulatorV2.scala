package client

import java.security._
import java.security.spec.X509EncodedKeySpec
import java.util.{Base64, Calendar}
import javax.crypto.spec.SecretKeySpec
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
  var myAesKey: SecretKey = _

  var registeredUsersList: List[(String, String, ActorRef)] = _

  val getPipeline: HttpRequest => Future[HttpResponse] = sendReceive
  val postPipeline: HttpRequest => Future[HttpResponse] = sendReceive

  var myFriendList: ListBuffer[(String)] = _
  var postType: String = "tagged"


  def receive = {

    case SignUpUser(uniqueId) =>
      mySender = sender
      myUserName = uniqueId
      keyPairGeneratorRsa.initialize(1024, secureRandom)
      keyPair = keyPairGeneratorRsa.genKeyPair()
      myAesKey = keyGeneratorAes.generateKey()


      val userNode = UserNode(
        id = "",
        about = "Something about " + "user" + myUserName.toString,
        birthday = "01/01/1988",
        email = "user" + myUserName.toString + "@ufl.edu",
        first_name = "user" + myUserName.toString,
        public_key = new String(Base64.getEncoder.encode(keyPair.getPublic.getEncoded)),
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

    case "DoneSendingFriendRequests" =>

      self ! "AcceptPendingInFriendRequests"
      val totalTimeDuration = Duration(20000, "millis")
      context.system.scheduler.scheduleOnce(totalTimeDuration, self, "CreateUserPost")

    case "DoneCreatingPost" =>
      //      self ! "ViewOwnPosts"
      self ! "ViewTaggedPosts"

    case "DoneViewingTaggedPosts" =>
      Thread.sleep(10000)

    case "ViewTaggedPosts" =>

      Thread.sleep(1000)
      println("Tryingtoseetaggedposts")
      val viewTaggedPostFuture: Future[HttpResponse] = getPipeline(Get(s"http://127.0.0.1:8080/user/tagged_posts/$myUserId"))
      val viewTaggedPostFutureResponse = Await.result(viewTaggedPostFuture, 5 second)
      val taggedPosts = viewTaggedPostFutureResponse.entity.asString.parseJson.convertTo[List[PostNode]]

      cipherRsa.init(Cipher.DECRYPT_MODE, keyPair.getPrivate)

      taggedPosts.foreach(taggedPost => {

        if (taggedPost.to_all_friends == true && taggedPost.encrypted == true) {
          println("trying to see  post for me and everyone")
          val postOwner = taggedPost.from_name
          val getFriendDetailsReqFuture: Future[HttpResponse] = getPipeline(Get(s"http://127.0.0.1:8080/user/get_friend_details/$myUserId/$postOwner"))
          val getFriendDetailsFutureResponse = Await.result(getFriendDetailsReqFuture, 5 second)
          println("@#@#@# - " + getFriendDetailsFutureResponse.entity.asString.parseJson.prettyPrint)
                    val postOwnerDetails: UserNode = getFriendDetailsFutureResponse.entity.asString.parseJson.convertTo[GetFriendDetailsRsp].friendNode
                    val postOwnersAesKey : SecretKey = new SecretKeySpec(cipherRsa.doFinal(Base64.getDecoder.decode(postOwnerDetails.encrypted_special_key.getBytes)), "AES")
                    cipherAes.init(Cipher.DECRYPT_MODE, postOwnersAesKey)

                    val plainMessage = new String(cipherAes.doFinal(Base64.getDecoder.decode(taggedPost.message.getBytes)))
                    println("Seeing post for me and everyone - " + plainMessage)

        }
        if (taggedPost.to_all_friends == false && taggedPost.encrypted == true) {

          val secretKey = new SecretKeySpec(cipherRsa.doFinal(Base64.getDecoder.decode(taggedPost.encrypted_secret_keys.find(x => {
            x.to == "user" + myUserName.toString
          }).get.encrypted_secret_key.getBytes)), "AES")

          cipherAes.init(Cipher.DECRYPT_MODE, secretKey)

          val plainMessage = new String(cipherAes.doFinal(Base64.getDecoder.decode(taggedPost.message.getBytes)))
          println("Seeingtaggedpost - " + plainMessage)
        }
        if (taggedPost.encrypted == false) {

        }

      })
      self ! "DoneViewingTaggedPosts"

    case "ViewOwnPosts" =>

      Thread.sleep(1000)
      println("Tryingtoseemyposts")

      val viewOwnPostFuture: Future[HttpResponse] = getPipeline(Get(s"http://127.0.0.1:8080/user/own_posts/$myUserId"))
      val viewOwnPostFutureResponse = Await.result(viewOwnPostFuture, 5 second)
      val ownPosts = viewOwnPostFutureResponse.entity.asString.parseJson.convertTo[List[PostNode]]

      if (ownPosts.isEmpty)
        println("Seeingmypost - ")

      //      cipherRsa.init(Cipher.DECRYPT_MODE, keyPair.getPrivate)

      ownPosts.foreach(ownPost => {

        if (ownPost.to_all_friends == true && ownPost.encrypted == true) {
          println("trying Seeingmypost for everyone")
          cipherAes.init(Cipher.DECRYPT_MODE, myAesKey)
          val plainMessage = new String(cipherAes.doFinal(Base64.getDecoder.decode(ownPost.message.getBytes)))
          println("Seeing my post for everyone - " + plainMessage)
        }

        if (ownPost.to_all_friends == false && ownPost.encrypted == true) {
          println("trying Seeing my post for some")
          val secretKey = new SecretKeySpec(cipherRsa.doFinal(Base64.getDecoder.decode(ownPost.encrypted_secret_keys.find(x => {
            x.to == "self"
          }).get.encrypted_secret_key.getBytes)), "AES")

          cipherAes.init(Cipher.DECRYPT_MODE, secretKey)

          val plainMessage = new String(cipherAes.doFinal(Base64.getDecoder.decode(ownPost.message.getBytes)))
          println("Seeing my post for some friends - " + plainMessage)
        }
        if (ownPost.encrypted == false) {

        }
      })

    case "CreateUserPost" =>

      if (postType == "tagged") {
        postType = "to_all"

        val now = Calendar.getInstance().getTime.toString
        val getFriendListFuture: Future[HttpResponse] = getPipeline(Get(s"http://127.0.0.1:8080/user/get_friends/$myUserId"))
        val getFriendListFutureResponse = Await.result(getFriendListFuture, 5 second)
        val friends = getFriendListFutureResponse.entity.asString.parseJson.convertTo[List[UserNode]]
        val encryptedAesKeys: ListBuffer[EncryptedSecretKey] = new ListBuffer[EncryptedSecretKey]()
        val plainTextPostMessage = myUserName + " posting a message " + " at " + now

        println("Iamposting - " + plainTextPostMessage)


        keyAes = keyGeneratorAes.generateKey()
        cipherRsa.init(Cipher.ENCRYPT_MODE, keyPair.getPublic)
        val encryptedAesKeyAsString = new String(Base64.getEncoder.encode(cipherRsa.doFinal(keyAes.getEncoded)))

        val encryptedAesKey = EncryptedSecretKey(
          to = "self",
          encrypted_secret_key = encryptedAesKeyAsString
        )
        encryptedAesKeys += encryptedAesKey

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

        val post = PostNode(
          id = "",
          created_time = now,
          description = "",
          from = myUserId,
          from_name = "",
          message = encryptedPostMessageAsString,
          encrypted_secret_keys = encryptedAesKeys.toList,
          to = List.empty,
          updated_time = now,
          encrypted = true,
          to_all_friends = false,
          message_iv = ""
        )

        val createUserPostReq = CreateUserPostReq(
          userId = myUserId,
          post = post
        )
        val entity = HttpEntity(contentType = ContentType(MediaTypes.`application/json`, HttpCharsets.`UTF-8`), createUserPostReq.toJson.toString)

        val future: Future[HttpResponse] = postPipeline(Post("http://127.0.0.1:8080/user/post", entity))
        val response = Await.result(future, 5 second)
        println("Ihaveposted" + response.entity.asString)
        //        future onComplete {
        //          case Success(response) =>
        //            println("Ihaveposted" + response.entity.asString)
        //
        //          case Failure(error) =>
        //            println("Some error has occurred: " + error.getMessage)
        //        }

      }
      if (postType == "to_all") {
        postType = "tagged"

        val now = Calendar.getInstance().getTime.toString
        val plainTextBroadcastMessage = myUserName + " posting a message for everyone at " + now
        cipherAes.init(Cipher.ENCRYPT_MODE, myAesKey)
        val encryptedPostMessageAsString = new String(Base64.getEncoder.encode(cipherAes.doFinal(plainTextBroadcastMessage.getBytes)))
        val post = PostNode(
          id = "",
          created_time = now,
          description = "",
          from = myUserId,
          from_name = "",
          message = encryptedPostMessageAsString,
          encrypted_secret_keys = List.empty,
          to = List.empty,
          updated_time = now,
          encrypted = true,
          to_all_friends = true,
          message_iv = ""
        )
        val createUserPostReq = CreateUserPostReq(
          userId = myUserId,
          post = post
        )
        val entity = HttpEntity(contentType = ContentType(MediaTypes.`application/json`, HttpCharsets.`UTF-8`), createUserPostReq.toJson.toString)

        val future: Future[HttpResponse] = postPipeline(Post("http://127.0.0.1:8080/user/post", entity))
        val response = Await.result(future, 5 second)
        println("Ihaveposted for everyone" + response.entity.asString)

        //        future onComplete {
        //          case Success(response) =>
        //            println("Ihaveposted for everyone" + response.entity.asString)
        //
        //          case Failure(error) =>
        //            println("Some error has occurred: " + error.getMessage)
        //        }

      }
      self ! "DoneCreatingPost"


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

      println("**&&** - " + myUserId)
      val future: Future[HttpResponse] = getPipeline(Get(s"http://127.0.0.1:8080/user/pending_in_friend_requests/$myUserId"))
      val response = Await.result(future, 5 second)
      val pendingInFriendRequests = response.entity.asString.parseJson.convertTo[List[String]]
      pendingInFriendRequests.foreach(pendingInFriendRequest => {
        val addFriendReq = AddFriendReq(
          userId = myUserId,
          friendName = pendingInFriendRequest
        )
        val entity = HttpEntity(contentType = ContentType(MediaTypes.`application/json`, HttpCharsets.`UTF-8`), addFriendReq.toJson.toString)
        val addFriendReqfuture: Future[HttpResponse] = postPipeline(Post("http://127.0.0.1:8080/user/add_friend_request", entity))
        addFriendReqfuture onComplete {
          case Success(addFriendReqfutureresponse) =>
            println(addFriendReqfutureresponse.entity.asString.parseJson.convertTo[AddFriendRsp].result)
          case Failure(error) =>
            println("Some error has occurred: " + error.getMessage)
        }
      })
      self ! "ShareAesKey"
      val totalTimeDuration = Duration(2000, "millis")
      context.system.scheduler.scheduleOnce(totalTimeDuration, self, "AcceptPendingInFriendRequests")


    case "ShareAesKey" =>

      val future: Future[HttpResponse] = getPipeline(Get(s"http://127.0.0.1:8080/user/get_friends/$myUserId"))
      val response = Await.result(future, 5 second)
      val friends = response.entity.asString.parseJson.convertTo[List[UserNode]]
      println("***Friends for the user: " + myUserName)
      friends.foreach(friend => {
        val publicKeyOfFriend: PublicKey =
          KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(
            Base64.getDecoder.decode(friend.public_key.getBytes)))
        cipherRsa.init(Cipher.ENCRYPT_MODE, publicKeyOfFriend)
        val encryptedAesKeyOfUserAsString = new String(Base64.getEncoder.encode(cipherRsa.doFinal(myAesKey.getEncoded)))
        val addSpecialKeyToFriendReq = AddSpecialKeyToFriendReq(
          userId = myUserId,
          friendName = friend.first_name,
          encrypted_special_key = encryptedAesKeyOfUserAsString
        )
        val entity = HttpEntity(contentType = ContentType(MediaTypes.`application/json`, HttpCharsets.`UTF-8`), addSpecialKeyToFriendReq.toJson.toString)
        val addSpecialKeyToFriendReqfuture: Future[HttpResponse] = postPipeline(Post("http://127.0.0.1:8080/user/add_special_key", entity))
        addSpecialKeyToFriendReqfuture onComplete {
          case Success(addSpecialKeyToFriendReqfutureresponse) =>
            println("Add specialkey to friend " + addSpecialKeyToFriendReqfutureresponse.entity.asString.parseJson.convertTo[AddSpecialKeyToFriendRsp].result)
          case Failure(error) =>
            println("Some error has occurred: " + error.getMessage)
        }
      })

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
