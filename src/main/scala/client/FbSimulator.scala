//package client
//
//import java.util.Calendar
//
//import akka.actor.{ActorSystem, _}
//import server.facebook._
//import server.http.FbJsonProtocol
//import spray.client.pipelining._
//import spray.http._
//import spray.json.{DefaultJsonProtocol, _}
//
//import scala.collection.mutable.ListBuffer
//import scala.concurrent.{Await, Future}
//import scala.util.{Random, Failure, Success}
//import scala.language.postfixOps
//import scala.concurrent.duration._
//
//
//case class StartCreatingUsers(numberOfUsers: Int)
//
//case class DoneCreatingUser(userName: String, userId: String)
//
//case class StartCreatingPages(numberOfPages: Int)
//
//case class DoneCreatingPage(pageName: String, pageId: String)
//
//case class StartUsersLikingPages(registeredUsers: List[(String, String)], registeredPages: List[(String, String)], percent: Int, average: Int)
//
//case object DoneUserLikingPage
//
//case class StartPageActivities(registeredPages: List[(String, String)], registeredUsers: List[(String, String)])
//
//case class ActiveUserStartActivities(actveUsers: List[(String, String)], registeredUsers: List[(String, String)])
//
//case class PassiveUserStartActivities(passiveUsers: List[(String, String)], registeredUsers: List[(String, String)])
//
//case class StartAllActivities(registeredUsers: List[(String, String)], registeredPages: List[(String, String)], percent: Int, average: Int)
//
//
//class Master extends Actor with ActorLogging {
//  val totalUsersCount = 100000
//  val totalPagesCount = 10000
//
//  val percentageOfUsersWhoClickLike: Int = 64
//  val averageNumberOfPagesLikedByAUser: Int = 40
//  val percentageofActiveUsers: Double = 58
//  var totalNumberOfLikesToDo = 0
//  var totalNumberOfLikesDone = 0
//
//  var myRegisteredUsers: ListBuffer[(String, String)] = new ListBuffer[(String, String)]()
//  var myRegisteredPages: ListBuffer[(String, String)] = new ListBuffer[(String, String)]()
//
//  var createUsersActorRef: ActorRef = _
//  var createPagesActorRef: ActorRef = _
//  var usersLikingPagesActorRef: ActorRef = _
//  var pagesMakingPostsActorRef: ActorRef = _
//  var pagesActivitiesActorRef: ActorRef = _
//  var activeUsersActorRef: ActorRef = _
//  var passiveUsersActorRef: ActorRef = _
//  var allActivitiesActorRef: ActorRef = _
//
//  def receive = {
//    case "Init" =>
//      log.info("Master init")
//      createUsersActorRef = context.system.actorOf(Props(new CreateUsersSubActor), "CreateUsers")
//      createUsersActorRef ! StartCreatingUsers(totalUsersCount)
//
//    case DoneCreatingUser(userName, userId) =>
//      val registeredUser = (userName, userId)
//      myRegisteredUsers += registeredUser
//      if (myRegisteredUsers.length == totalUsersCount) {
//        log.info("Done creating " + myRegisteredUsers.length.toString + " users")
//        createUsersActorRef ! "PleaseKillYourself"
//        createPagesActorRef = context.system.actorOf(Props(new CreatePagesSubActor), "CreatePages")
//        createPagesActorRef ! StartCreatingPages(totalPagesCount)
//      }
//
//    case DoneCreatingPage(pageName, pageId) =>
//      val registeredPage = (pageName, pageId)
//      myRegisteredPages += registeredPage
//      if (myRegisteredPages.length == totalPagesCount) {
//        log.info("Done creating " + myRegisteredPages.length.toString + " pages")
//        createPagesActorRef ! "PleaseKillYourself"
//        usersLikingPagesActorRef = context.system.actorOf(Props(new UsersLikingPagesSubActor), "UsersLikingPages")
//        usersLikingPagesActorRef ! StartUsersLikingPages(myRegisteredUsers.toList, myRegisteredPages.toList, percentageOfUsersWhoClickLike, averageNumberOfPagesLikedByAUser)
//        totalNumberOfLikesToDo = myRegisteredUsers.length * percentageOfUsersWhoClickLike / 100 * averageNumberOfPagesLikedByAUser
//      }
//
//    case DoneUserLikingPage =>
//      totalNumberOfLikesDone += 1
//      if (totalNumberOfLikesDone == 1000) {
//        log.info("Done liking 100 pages")
//        pagesActivitiesActorRef = context.system.actorOf(Props(new PagesActivitiessSubActor), "PagesActivities")
//        pagesActivitiesActorRef ! StartPageActivities(myRegisteredPages.toList, myRegisteredUsers.toList)
//        val (activeUsers, passiveUsers) = myRegisteredUsers.splitAt(((percentageofActiveUsers / 100) * myRegisteredUsers.length).toInt)
//        activeUsersActorRef = context.system.actorOf(Props(new ActiveUsersSubActor), "ActiveUsers")
//        passiveUsersActorRef = context.system.actorOf(Props(new PassiveUsersSubActor), "PassiveUsers")
//        activeUsersActorRef ! ActiveUserStartActivities(activeUsers.toList, myRegisteredUsers.toList)
//        passiveUsersActorRef ! PassiveUserStartActivities(passiveUsers.toList, myRegisteredUsers.toList)
//        //        allActivitiesActorRef = context.system.actorOf(Props(new AllActivitiesUserSubActor), "AllActivities")
//        //        allActivitiesActorRef ! StartAllActivities(myRegisteredUsers.toList, myRegisteredPages.toList,percentageOfUsersWhoClickLike,averageNumberOfPagesLikedByAUser)
//      }
//      if (totalNumberOfLikesDone == totalNumberOfLikesToDo) {
//        log.info("Done liking all the " + totalNumberOfLikesDone + " pages ")
//        usersLikingPagesActorRef ! "PleaseKillYourself"
//      }
//
//  }
//}
//
//class AllActivitiesUserSubActor extends Actor with ActorLogging {
//
//  import FbJsonProtocol._
//
//  implicit val system = ActorSystem()
//
//  import system.dispatcher
//
//  var registeredPages: List[(String, String)] = _
//  var registeredUsers: List[(String, String)] = _
//  var mySender: ActorRef = _
//  var percentageOfUsersWhoClickLike: Int = _
//  var averageNumberOfPagesLikedByAUser: Int = _
//  var currentUserIndex: Int = _
//  var currentPageIndex: Int = _
//
//
//  def receive = {
//
//    case StartAllActivities(registeredUsersIn, registeredPagesIn, percent, average) =>
//      log.info("Pages starting to create posts and share photos")
//      mySender = sender
//      registeredPages = registeredPagesIn
//      registeredUsers = registeredUsersIn
//      percentageOfUsersWhoClickLike = percent
//      averageNumberOfPagesLikedByAUser = average
//      currentUserIndex = 0
//      currentPageIndex = 0
//
//      self ! "StartLikingPages"
//
//    case "StartLikingPages" =>
//      if (currentUserIndex == registeredUsers.length - 1) currentUserIndex = 0 else currentUserIndex = currentUserIndex + 1
//      if (currentPageIndex == registeredPages.length - 1) currentPageIndex = 0 else currentPageIndex = currentPageIndex + 1
//      UserLikingPages(registeredUsers, registeredPages, percentageOfUsersWhoClickLike, averageNumberOfPagesLikedByAUser, currentUserIndex, currentPageIndex)
//      log.info(s"User at $currentUserIndex liking page at $currentPageIndex")
//      self ! "Start Unliking pages"
//
//
//      def UserLikingPages(registeredUsers: List[(String, String)], registeredPages: List[(String, String)], percent: Int, average: Int, currentUserIndex: Int, currentPageIndex: Int) {
//
//        val addUserLikedPageReq = AddUserLikedPageReq(
//          userId = registeredUsers(currentUserIndex)._2,
//          pageName = registeredPages(currentPageIndex)._1)
//
//        val entity = HttpEntity(contentType = ContentType(MediaTypes.`application/json`, HttpCharsets.`UTF-8`), addUserLikedPageReq.toJson.toString)
//
//        val pipeline: HttpRequest => Future[HttpResponse] = sendReceive
//
//        val future: Future[HttpResponse] = pipeline(Post("http://127.0.0.1:8080/like_this_page", entity))
//      }
//
//
//  }
//
//}
//
//class ActiveUsersSubActor extends Actor with ActorLogging {
//
//  import FbJsonProtocol._
//
//  implicit val system = ActorSystem()
//
//  import system.dispatcher
//
//  var activeUsersList: List[(String, String)] = _
//  var registereduserList: List[(String, String)] = _
//  var mySender: ActorRef = _
//  var photoCount: Int = 0
//
//  def receive = {
//
//    case ActiveUserStartActivities(activeUsers, registeredUsers) =>
//      log.info("Active Users starting activities")
//      mySender = sender
//      activeUsersList = activeUsers
//      registereduserList = registeredUsers
//
//      self ! "StartPostsForActiveUsers"
//
//    case "StartPostsForActiveUsers" =>
//      CreateAPostFromActiveUser(activeUsersList, registereduserList)
//      //      Thread.sleep(10)
//      self ! "StartPhotoPostsForActiveUsers"
//
//    case "StartPhotoPostsForActiveUsers" =>
//      PostAphotoFromActiveUser(activeUsersList, registereduserList)
//      //      Thread.sleep(10)
//      self ! "ViewTimelineForActiveUsers"
//
//    case "ViewTimelineForActiveUsers" =>
//      ViewTimelineFromActiveUser(activeUsersList)
//      //      Thread.sleep(10)
//      self ! "StartPostsForActiveUsers"
//
//
//    case "CreateAlbumsFromActiveUsers" =>
//      CreateAlbumsFromActiveUser(activeUsersList)
//      self ! "StartPostsForActiveUsers"
//
//    case "PleaseKillYourself" =>
//      log.info(self.path.name + " says bye")
//      context.stop(self)
//  }
//
//  def CreateAPostFromActiveUser(activeUsers: List[(String, String)], registeredUsers: List[(String, String)]) {
//
//    val randomActiveUserIndex = Random.nextInt(activeUsers.length)
//
//    val now = Calendar.getInstance().getTime.toString
//
//    val postNode = PostNode(
//      id = "",
//      created_time = now,
//      description = "",
//      from = activeUsers(randomActiveUserIndex)._2,
//      message = activeUsers(randomActiveUserIndex)._1 + " posting a random number " + Random.nextInt(1000).toString + " at " + now,
//      to = List.empty,
//      updated_time = now)
//    //    log.info("Active User " + postNode.from + " is creating a post with the message " + postNode.message )
//    val createActiveUserPostReq = CreateUserPostReq(
//      userId = activeUsers(randomActiveUserIndex)._2,
//      post = postNode)
//
//    val entity = HttpEntity(contentType = ContentType(MediaTypes.`application/json`, HttpCharsets.`UTF-8`), createActiveUserPostReq.toJson.toString)
//
//    val pipeline: HttpRequest => Future[HttpResponse] = sendReceive
//
//    val future: Future[HttpResponse] = pipeline(Post("http://127.0.0.1:8080/user/post", entity))
//    //    val userPostFutureResult = Await.result(future, 5 second)
//    //    println(userPostFutureResult.entity.data.asString.parseJson.prettyPrint)
//
//  }
//
//  def PostAphotoFromActiveUser(activeUsers: List[(String, String)], registeredUsers: List[(String, String)]) {
//
//    val randomUserIndex = Random.nextInt(registeredUsers.length)
//    val randomActiveUserIndex = Random.nextInt(activeUsers.length)
//
//    val now = Calendar.getInstance().getTime.toString
//    photoCount = photoCount + 1
//
//    val photoNode = PhotoNode(
//      id = "",
//      album = "",
//      created_time = now,
//      from = activeUsers(randomActiveUserIndex)._2,
//      height = 1080,
//      name = activeUsers(randomActiveUserIndex)._1.toString + "_photo_" + photoCount.toString,
//      width = 1920,
//      caption = "tagging " + registeredUsers(randomUserIndex)._1
//    )
//    //    log.info(s"User " + photoNode.from + "is sharing " + photoNode.name + " and " + photoNode.caption )
//    val createActiveUserPhotoReq = CreateUserPhotoReq(
//      userId = activeUsers(randomActiveUserIndex)._2,
//      photo = photoNode
//    )
//    val entity = HttpEntity(contentType = ContentType(MediaTypes.`application/json`, HttpCharsets.`UTF-8`), createActiveUserPhotoReq.toJson.toString)
//
//    val pipeline: HttpRequest => Future[HttpResponse] = sendReceive
//
//    val future: Future[HttpResponse] = pipeline(Post("http://127.0.0.1:8080/user/photo", entity))
//        val photoFutureResult = Await.result(future, 5 second)
//    //    println(photoFutureResult.entity.data.asString.parseJson.prettyPrint)
//
//  }
//
//  def ViewTimelineFromActiveUser(activeUsers: List[(String, String)]) {
//
//    val randomActiveUserIndex = Random.nextInt(activeUsers.length)
//    val userId = activeUsers(randomActiveUserIndex)._2
//    //    log.info("User " + userId + " is viewing TimeLine")
//    val pipeline: HttpRequest => Future[HttpResponse] = sendReceive
//    val future: Future[HttpResponse] = pipeline(Get(s"http://127.0.0.1:8080/user/timeline/$userId"))
//    //    val timelineResult = Await.result(future, 5 second)
//
//    //    println(timelineResult.entity.data.asString.parseJson.prettyPrint)
//  }
//
//  def CreateAlbumsFromActiveUser(activeUsers: List[(String, String)]) {
//
//  }
//
//}
//
//class PassiveUsersSubActor extends Actor with ActorLogging {
//
//  import FbJsonProtocol._
//
//  implicit val system = ActorSystem()
//
//  import system.dispatcher
//
//  var passiveUsersList: List[(String, String)] = _
//  var registereduserList: List[(String, String)] = _
//  var mySender: ActorRef = _
//  var photoCount: Int = 0
//
//  def receive = {
//
//    case PassiveUserStartActivities(passiveUsers, registeredUsers) =>
//      log.info("Passive Users starting activities")
//      mySender = sender
//      passiveUsersList = passiveUsers
//      registereduserList = registeredUsers
//      self ! "ViewTimelineForPassiveiveUsers"
//
//
//    case "ViewTimelineForPassiveiveUsers" =>
//      ViewTimelineFromPassiveUser(passiveUsersList)
//      //      Thread.sleep(100)
//      self ! "ViewTimelineForPassiveiveUsers"
//
//    case "PleaseKillYourself" =>
//      log.info(self.path.name + " says bye")
//      context.stop(self)
//  }
//
//  //  def CreateAPostFromPassiveUser(passiveUsers: List[(String, String)],registeredUsers: List[(String, String)]) {
//  //
//  //    val randomPassiveUserIndex = Random.nextInt(passiveUsers.length)
//  //
//  //    val now = Calendar.getInstance().getTime.toString
//  //
//  //    val postNode = PostNode(
//  //      id = "",
//  //      created_time = now,
//  //      description = "",
//  //      from = passiveUsers(randomPassiveUserIndex)._2,
//  //      message = passiveUsers(randomPassiveUserIndex)._1 + " posting a random number " + Random.nextInt(1000).toString + " at " + now,
//  //      to = List.empty,
//  //      updated_time = now)
//  //    //    log.info("Active User " + postNode.from + " is creating a post with the message " + postNode.message )
//  //    val createPassiveUserPostReq = CreateUserPostReq(
//  //      userId = passiveUsers(randomPassiveUserIndex)._2,
//  //      post = postNode)
//  //
//  //    val entity = HttpEntity(contentType = ContentType(MediaTypes.`application/json`, HttpCharsets.`UTF-8`), createPassiveUserPostReq.toJson.toString)
//  //
//  //    val pipeline: HttpRequest => Future[HttpResponse] = sendReceive
//  //
//  //    val future: Future[HttpResponse] = pipeline(Post("http://127.0.0.1:8080/user/post", entity))
//  //    Await.result(future, 5 second)
//  //
//  //  }
//  //  def PostAphotoFromPassiveiveUser(passiveUsers: List[(String, String)],registeredUsers: List[(String, String)])  {
//  //
//  //    val randomUserIndex = Random.nextInt(registeredUsers.length)
//  //    val randomPassiveUserIndex = Random.nextInt(passiveUsers.length)
//  //
//  //    val now = Calendar.getInstance().getTime.toString
//  //    photoCount = photoCount+1
//  //
//  //    val photoNode = PhotoNode(
//  //      id = "",
//  //      album = "",
//  //      created_time = now,
//  //      from = passiveUsers(randomPassiveUserIndex)._2,
//  //      height = 1080,
//  //      name = passiveUsers(randomPassiveUserIndex)._1.toString + "_photo_" + photoCount.toString,
//  //      width = 1920,
//  //      caption = "tagging " + registeredUsers(randomUserIndex)._1
//  //    )
//  //    //    log.info(s"User " + photoNode.from + "is sharing " + photoNode.name + " and " + photoNode.caption )
//  //    val createPassiveUserPhotoReq = CreateUserPhotoReq (
//  //      userId = passiveUsers(randomPassiveUserIndex)._2,
//  //      photo  = photoNode
//  //    )
//  //    val entity = HttpEntity(contentType = ContentType(MediaTypes.`application/json`, HttpCharsets.`UTF-8`), createPassiveUserPhotoReq.toJson.toString)
//  //
//  //    val pipeline: HttpRequest => Future[HttpResponse] = sendReceive
//  //
//  //    val future: Future[HttpResponse] = pipeline(Post("http://127.0.0.1:8080/user/photo", entity))
//  //    Await.result(future, 5 second)
//  //
//  //  }
//  def ViewTimelineFromPassiveUser(passiveUsers: List[(String, String)]) {
//
//    val randomPassiveUserIndex = Random.nextInt(passiveUsers.length)
//    val userId = passiveUsers(randomPassiveUserIndex)._2
//    //    log.info("User " + userId + " is viewing TimeLine")
//    val pipeline: HttpRequest => Future[HttpResponse] = sendReceive
//    val future: Future[HttpResponse] = pipeline(Get(s"http://127.0.0.1:8080/user/timeline/$userId"))
//    //    Await.result(future, 5 second)
//
//  }
//
//}
//
//class PagesActivitiessSubActor extends Actor with ActorLogging {
//
//  import FbJsonProtocol._
//
//  implicit val system = ActorSystem()
//
//  import system.dispatcher
//
//  var registeredPages: List[(String, String)] = _
//  var registeredUsers: List[(String, String)] = _
//  var mySender: ActorRef = _
//  var photoCount: Int = 0
//
//  def receive = {
//
//    case StartPageActivities(registeredPagesIn, registeredUsersIn) =>
//      log.info("Pages starting to create posts and share photos")
//      mySender = sender
//      registeredPages = registeredPagesIn
//      registeredUsers = registeredUsersIn
//      self ! "StartPostsForPages"
//
//    case "StartPostsForPages" =>
//      makeAPostFromPage(registeredPages)
//      //      Thread.sleep(10)
//      self ! "StartPhotoPostsForPages"
//
//    case "StartPhotoPostsForPages" =>
//      PostAPhotoFromPage(registeredPages, registeredUsers)
//      //      Thread.sleep(10)
//      self ! "StartPostsForPages"
//
//    case "PleaseKillYourself" =>
//      log.info(self.path.name + " says bye")
//      context.stop(self)
//  }
//
//
//  def PostAPhotoFromPage(registeredPages: List[(String, String)], registeredUsers: List[(String, String)]) = {
//
//    val randomPageIndex = Random.nextInt(registeredPages.length)
//    val randomUserIndex = Random.nextInt(registeredUsers.length)
//
//    val now = Calendar.getInstance().getTime.toString
//    photoCount = photoCount + 1
//
//    val photoNode = PhotoNode(
//      id = "",
//      album = "",
//      created_time = now,
//      from = registeredPages(randomPageIndex)._2,
//      height = 1080,
//      name = registeredPages(randomPageIndex)._1.toString + "_photo_" + photoCount.toString,
//      width = 1920,
//      caption = "tagging " + registeredUsers(randomUserIndex)._1
//    )
//    //    log.info(s"Page " + photoNode.from + "is sharing " + photoNode.name + " and " + photoNode.caption )
//    val createPagePhotoReq = CreatePagePhotoReq(
//      pageId = registeredPages(randomPageIndex)._2,
//      photo = photoNode
//    )
//    val entity = HttpEntity(contentType = ContentType(MediaTypes.`application/json`, HttpCharsets.`UTF-8`), createPagePhotoReq.toJson.toString)
//
//    val pipeline: HttpRequest => Future[HttpResponse] = sendReceive
//
//    val future: Future[HttpResponse] = pipeline(Post("http://127.0.0.1:8080/page/photo", entity))
//    //    Await.result(future, 5 second)
//  }
//
//  def makeAPostFromPage(registeredPages: List[(String, String)]) = {
//    val randomPageIndex = Random.nextInt(registeredPages.length)
//
//    val now = Calendar.getInstance().getTime.toString
//
//    val postNode = PostNode(
//      id = "",
//      created_time = now,
//      description = "",
//      from = registeredPages(randomPageIndex)._2,
//      message = registeredPages(randomPageIndex)._1 + " posting a random number " + Random.nextInt(1000).toString + " at " + now,
//      to = List.empty,
//      updated_time = now)
//    //    log.info("Page " + postNode.from + " is creating a post with the message " + postNode.message )
//    val createPagePostReq = CreatePagePostReq(
//      pageId = registeredPages(randomPageIndex)._2,
//      post = postNode)
//
//    val entity = HttpEntity(contentType = ContentType(MediaTypes.`application/json`, HttpCharsets.`UTF-8`), createPagePostReq.toJson.toString)
//
//    val pipeline: HttpRequest => Future[HttpResponse] = sendReceive
//
//    val future: Future[HttpResponse] = pipeline(Post("http://127.0.0.1:8080/page/post", entity))
//    //    Await.result(future, 5 second)
//  }
//}
//
//class UsersLikingPagesSubActor extends Actor with ActorLogging {
//
//  import FbJsonProtocol._
//
//  implicit val system = ActorSystem()
//
//  import system.dispatcher
//
//  var percentageOfUsersWhoClickLike: Int = 0
//  var averageNumberOfPagesLikedByAUser: Int = 0
//
//  def receive = {
//    case StartUsersLikingPages(registeredUsers, registeredPages, percent, average) =>
//      val mySender = sender
//
//      percentageOfUsersWhoClickLike = percent
//      averageNumberOfPagesLikedByAUser = average
//      val totalNumberOfLikesToDo = registeredUsers.length * percentageOfUsersWhoClickLike / 100 * averageNumberOfPagesLikedByAUser
//
//      (0 until totalNumberOfLikesToDo).foreach(i => {
//        val addUserLikedPageReq = AddUserLikedPageReq(
//          userId = registeredUsers(Random.nextInt(registeredUsers.length * percentageOfUsersWhoClickLike / 100))._2,
//          pageName = registeredPages(Random.nextInt(registeredPages.length))._1)
//
//        val entity = HttpEntity(contentType = ContentType(MediaTypes.`application/json`, HttpCharsets.`UTF-8`), addUserLikedPageReq.toJson.toString)
//
//        val pipeline: HttpRequest => Future[HttpResponse] = sendReceive
//
//        val future: Future[HttpResponse] = pipeline(Post("http://127.0.0.1:8080/like_this_page", entity))
//        future onComplete {
//          case Success(response) =>
//            mySender ! DoneUserLikingPage
//
//          case Failure(error) =>
//            println("Some error has occurred: " + error.getMessage)
//        }
//      })
//
//    case "PleaseKillYourself" =>
//      log.info(self.path.name + " says bye")
//      context.stop(self)
//  }
//}
//
//class CreatePagesSubActor extends Actor with ActorLogging {
//
//  import FbJsonProtocol._
//
//  implicit val system = ActorSystem()
//
//  import system.dispatcher
//
//  def receive = {
//    case StartCreatingPages(numberOfPages) =>
//      val mySender = sender
//      log.info(s"Creating $numberOfPages pages")
//
//      (0 until numberOfPages).foreach(i => {
//
//        val pageNode = PageNode(
//          id = "",
//          about = "Something about page" + i.toString,
//          description = "page" + i.toString + "'s description",
//          name = "page" + i.toString,
//          likes = 0)
//
//        val entity = HttpEntity(contentType = ContentType(MediaTypes.`application/json`, HttpCharsets.`UTF-8`), pageNode.toJson.toString)
//
//        val pipeline: HttpRequest => Future[HttpResponse] = sendReceive
//
//        val future: Future[HttpResponse] = pipeline(Post("http://127.0.0.1:8080/page/create", entity))
//        future onComplete {
//          case Success(response) =>
//            mySender ! DoneCreatingPage(pageNode.name, response.entity.asString.parseJson.convertTo[CreateFbNodeRsp].id)
//
//          case Failure(error) =>
//            println("Some error has occurred: " + error.getMessage)
//        }
//      })
//
//    case "PleaseKillYourself" =>
//      log.info(self.path.name + " says bye")
//      context.stop(self)
//  }
//}
//
//class CreateUsersSubActor extends Actor with ActorLogging {
//
//  import FbJsonProtocol._
//
//  implicit val system = ActorSystem()
//
//  import system.dispatcher
//
//  def receive = {
//    case StartCreatingUsers(numberOfUsers) =>
//      val mySender = sender
//      log.info(s"Creating $numberOfUsers users")
//
//      (0 until numberOfUsers).foreach(i => {
//
//        val userNode = UserNode("",
//          "Something about " + "user" + i.toString,
//          "01/01/1988",
//          "user" + i.toString + "@ufl.edu",
//          "user" + i.toString,
//          "public_key"
//        )
//
//        val entity = HttpEntity(contentType = ContentType(MediaTypes.`application/json`, HttpCharsets.`UTF-8`), userNode.toJson.toString)
//
//        val pipeline: HttpRequest => Future[HttpResponse] = sendReceive
//
//        val future: Future[HttpResponse] = pipeline(Post("http://127.0.0.1:8080/user/create", entity))
//        future onComplete {
//          case Success(response) =>
//            mySender ! DoneCreatingUser(userNode.first_name, response.entity.asString.parseJson.convertTo[CreateFbNodeRsp].id)
//
//          case Failure(error) =>
//            println("Some error has occurred: " + error.getMessage)
//        }
//      })
//
//    case "PleaseKillYourself" =>
//      log.info(self.path.name + " says bye")
//      context.stop(self)
//  }
//}
//
//object FbSimulator {
//
//  def main(args: Array[String]) {
//    ActorSystem("dos-project-4").actorOf(Props(new Master), "Master") ! "Init"
//  }
//
//}
