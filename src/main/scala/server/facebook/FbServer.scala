package server.facebook

import java.security.MessageDigest
import java.util.Calendar

import akka.actor._
import akka.pattern.ask
import akka.util.Timeout

import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.language.postfixOps

class FbServer extends Actor with ActorLogging {

  var users: mutable.HashMap[String, Node] = new mutable.HashMap[String, Node]()
  var usersOwnPosts: mutable.HashMap[String, ListBuffer[String]] = new mutable.HashMap[String, ListBuffer[String]]()
  var usersTaggedPosts: mutable.HashMap[String, ListBuffer[String]] = new mutable.HashMap[String, ListBuffer[String]]()

  var pages: mutable.HashMap[String, Node] = new mutable.HashMap[String, Node]()

  var posts: mutable.HashMap[String, Node] = new mutable.HashMap[String, Node]()

  var albums: mutable.HashMap[String, Node] = new mutable.HashMap[String, Node]()
  var albumsPhotos: mutable.HashMap[String, ListBuffer[String]] = new mutable.HashMap[String, ListBuffer[String]]()

  var photos: mutable.HashMap[String, Node] = new mutable.HashMap[String, Node]()
  var photosTags: mutable.HashMap[String, ListBuffer[String]] = new mutable.HashMap[String, ListBuffer[String]]()
  var photosLikes: mutable.HashMap[String, ListBuffer[String]] = new mutable.HashMap[String, ListBuffer[String]]()


  var forwardingMap: ListBuffer[(ActorRef, ActorRef)] = new ListBuffer[(ActorRef, ActorRef)]()
  var mySubActorCount: Int = 0

  def receive = {
    case CreateFbNodeReq(nodeType, node) =>
      nodeType match {
        case "user" =>
          val userNode = node.asInstanceOf[UserNode]
          if (userNode.id.isEmpty)
            userNode.id = Sha256(userNode.email)
          sender ! addToDb(users, userNode.id, userNode)

        case "page" =>
          val pageNode = node.asInstanceOf[PageNode]
          pageNode.id = Sha256(pageNode.name)
          sender ! addToDb(pages, pageNode.id, pageNode)

        case "post" =>
          val postNode = node.asInstanceOf[PostNode]
          if (postNode.id.isEmpty)
            postNode.id = Sha256(postNode.message)
          sender ! addToDb(posts, postNode.id, postNode)
      }

    case GetFbNodeReq(nodeType, nodeId) =>
      nodeType match {
        case "user" =>


        case "post" =>
          sender ! GetFbNodeRsp(posts.get(nodeId).get)
      }

    case UpdateUserTaggedPostNtf(userId, postId) =>
      if (!usersTaggedPosts.get(userId).isEmpty)
        usersTaggedPosts.get(userId).get.insert(0, postId)

    case CreateUserPostReq(userId, post) =>
      mySubActorCount += 1
      val fbWorkerForUserActivities = context.system.actorOf(Props(new FbWorkerForUserActivities), name = 0.toString + "_FbWorker_" + mySubActorCount.toString)
      val forwardingPair = (sender, fbWorkerForUserActivities)
      forwardingMap += forwardingPair
      fbWorkerForUserActivities ! "Init"
      fbWorkerForUserActivities ! CreateUserPostReqToFbWorker(post, usersOwnPosts.get(userId).get)

    case CreateUserPostRspToFbServer(postId) =>
      val forwardingMapPair = forwardingMap.remove(forwardingMap.indexWhere(x => {
        x._2 == sender
      }))
      forwardingMapPair._1 ! CreateUserPostRsp(postId)
      forwardingMapPair._2 ! PleaseKillYourself

    case GetUserFeedReq(userId, typeOfPosts, startFrom, limit) =>
      mySubActorCount += 1
      val fbWorkerForUserActivities = context.system.actorOf(Props(new FbWorkerForUserActivities), name = 0.toString + "_FbWorker_" + mySubActorCount.toString)
      val forwardingPair = (sender, fbWorkerForUserActivities)
      forwardingMap += forwardingPair
      fbWorkerForUserActivities ! "Init"
      typeOfPosts match {
        case "own" =>
          fbWorkerForUserActivities ! GetUserPostsReqToFbWorker(startFrom, limit, usersOwnPosts.get(userId).get)
        case "tagged" =>
          fbWorkerForUserActivities ! GetUserPostsReqToFbWorker(startFrom, limit, usersTaggedPosts.get(userId).get)
      }

    case GetUserPostsRspToFbServer(posts) =>
      val forwardingMapPair = forwardingMap.remove(forwardingMap.indexWhere(x => {
        x._2 == sender
      }))
      forwardingMapPair._1 ! GetUserFeedRsp(posts)
      forwardingMapPair._2 ! PleaseKillYourself
  }

  def addToDb(db: mutable.HashMap[String, Node], key: String, value: Node): CreateFbNodeRsp = {
    var result = false
    var id: String = ""
    if (!db.contains(key)) {
      result = true
      id = key
      db.put(key, value)
      if (db == users) {
        usersOwnPosts.put(key, ListBuffer.empty)
        usersTaggedPosts.put(key, ListBuffer.empty)
      }
    }
    CreateFbNodeRsp(result, id)
  }

  def Sha256(s: String): String = {
    val m = MessageDigest.getInstance("SHA-1").digest(s.getBytes("UTF-8"))
    m.map("%02x".format(_)).mkString
  }
}

class FbWorkerForUserActivities extends Actor with ActorLogging {
  implicit val someTimeout = Timeout(5 seconds)
  var myFbServerRef: ActorRef = _

  def receive = {
    case "Init" =>
      myFbServerRef = sender

    case CreateUserPostReqToFbWorker(post, ownPosts) =>
      // make proper post with id
      post.id = Sha256(post.message + post.from)
      post.to = parseMessageAndFindUsernames(post.message)

      // create the post on server that will host it
      val future: Future[CreateFbNodeRsp] = (myFbServerRef ? CreateFbNodeReq("post", post)).mapTo[CreateFbNodeRsp]
      val createFbNodeRsp = Await.result(future, someTimeout.duration)
      if (createFbNodeRsp.result) {

      }

      ownPosts.insert(0, post.id)

      post.to.foreach(taggedUser => {
        // please don't tag yourself
        myFbServerRef ! UpdateUserTaggedPostNtf(taggedUser, post.id)
      })

      myFbServerRef ! CreateUserPostRspToFbServer(post.id)

    case GetUserPostsReqToFbWorker(startFrom: String, limit: Integer, postIds: ListBuffer[String]) =>
      val posts: ListBuffer[PostNode] = new ListBuffer[PostNode]()
      postIds.foreach(postId => {
        val future: Future[GetFbNodeRsp] = (myFbServerRef ? GetFbNodeReq("post", postId)).mapTo[GetFbNodeRsp]
        val getFbNodeRsp = Await.result(future, someTimeout.duration)
        posts += getFbNodeRsp.node.asInstanceOf[PostNode]
      })

      myFbServerRef ! GetUserPostsRspToFbServer(posts.toList)

    case PleaseKillYourself =>
      context.stop(self)
  }

  def Sha256(s: String): String = {
    val m = MessageDigest.getInstance("SHA-1").digest(s.getBytes("UTF-8"))
    m.map("%02x".format(_)).mkString
  }

  def parseMessageAndFindUsernames(message: String): List[String] = {
    val usernames: ListBuffer[String] = new ListBuffer[String]()
    val usernamePattern = "@([A-Za-z0-9_]+)".r
    val matches = usernamePattern.findAllIn(message)
    matches.foreach(aMatch => {
      usernames += aMatch.drop(1)
    })
    usernames.toList
  }
}

object ServerTest {
  def main(args: Array[String]) {
    implicit val someTimeout = Timeout(5 seconds)
    val system = ActorSystem("ServerTest")
    val server00 = system.actorOf(Props(new FbServer), "FbServer00")

    val user00 = new UserNode(0.toString, "About user00", "1/1/1900", "user00@gmail.com", "User00")
    val user01 = new UserNode(1.toString, "About user01", "1/1/1900", "user01@gmail.com", "User01")

    var future: Future[CreateFbNodeRsp] = (server00 ? CreateFbNodeReq("user", user00)).mapTo[CreateFbNodeRsp]
    var createFbNodeRsp = Await.result(future, someTimeout.duration)
    if (createFbNodeRsp.result) {
      println("user00 added")
    }

    future = (server00 ? CreateFbNodeReq("user", user01)).mapTo[CreateFbNodeRsp]
    createFbNodeRsp = Await.result(future, someTimeout.duration)
    if (createFbNodeRsp.result) {
      println("user01 added")
    }

    val now = Calendar.getInstance().getTime.toString

    val post00 = new PostNode("", now, "post00 desc", "user00", "I'm tagging @1@2 in post00", List.empty, now)
    val post01 = new PostNode("", now, "post01 desc", "user00", "I'm tagging nobody in post01", List.empty, now)
    val post02 = new PostNode("", now, "post02 desc", "user00", "I'm tagging @2@1 in post02", List.empty, now)

    val futurePostRsp: Future[CreateUserPostRsp] = (server00 ? CreateUserPostReq(0.toString, post00)).mapTo[CreateUserPostRsp]
    val createUserPostRsp = Await.result(futurePostRsp, someTimeout.duration)
    println(createUserPostRsp.postId)

    val futurePostRsp01: Future[CreateUserPostRsp] = (server00 ? CreateUserPostReq(0.toString, post01)).mapTo[CreateUserPostRsp]
    val createUserPostRsp01 = Await.result(futurePostRsp01, someTimeout.duration)
    println(createUserPostRsp01.postId)

    val futurePostRsp02: Future[CreateUserPostRsp] = (server00 ? CreateUserPostReq(0.toString, post02)).mapTo[CreateUserPostRsp]
    val createUserPostRsp02 = Await.result(futurePostRsp02, someTimeout.duration)
    println(createUserPostRsp02.postId)

    val futureGetPostRsp: Future[GetUserFeedRsp] = (server00 ? GetUserFeedReq(1.toString, "tagged", "", 0)).mapTo[GetUserFeedRsp]
    val getUserPostRsp = Await.result(futureGetPostRsp, someTimeout.duration)
    getUserPostRsp.posts.foreach(post => {
      println(post)
    })

    system.awaitTermination()
  }
}