package server.facebook

import java.security.MessageDigest

import akka.actor._

import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.language.postfixOps

class FbServer extends Actor with ActorLogging {

  var users: mutable.HashMap[String, Node] = new mutable.HashMap[String, Node]()
  var usersOwnPosts: mutable.HashMap[String, ListBuffer[String]] = new mutable.HashMap[String, ListBuffer[String]]()
  var usersTaggedPosts: mutable.HashMap[String, ListBuffer[String]] = new mutable.HashMap[String, ListBuffer[String]]()
  var usersOwnPhotos: mutable.HashMap[String, ListBuffer[String]] = new mutable.HashMap[String, ListBuffer[String]]()
  var usersTaggedPhotos: mutable.HashMap[String, ListBuffer[String]] = new mutable.HashMap[String, ListBuffer[String]]()

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
            userNode.id = getShaOf(userNode.email)
          sender ! addToDb(users, userNode.id, userNode)

        case "page" =>
          val pageNode = node.asInstanceOf[PageNode]
          pageNode.id = getShaOf(pageNode.name)
          sender ! addToDb(pages, pageNode.id, pageNode)

        case "post" =>
          val postNode = node.asInstanceOf[PostNode]
          if (postNode.id.isEmpty)
            postNode.id = getShaOf(postNode.message)
          sender ! addToDb(posts, postNode.id, postNode)

        case "photo" =>
          val photoNode = node.asInstanceOf[PhotoNode]
          if (photoNode.id.isEmpty)
            photoNode.id = getShaOf(photoNode.name+photoNode.created_time)
          sender ! addToDb(photos, photoNode.id, photoNode)
      }

    case GetFbNodeReq(nodeType, nodeId) =>
      nodeType match {
        case "user" =>

        case "post" =>
          sender ! GetFbNodeRsp(posts.get(nodeId).get)

        case "photo" =>
          sender ! GetFbNodeRsp(photos.get(nodeId).get)
      }

    case UpdateUserTaggedPostNtf(userId, postId) =>
      if (!usersTaggedPosts.get(userId).isEmpty)
        usersTaggedPosts.get(userId).get.insert(0, postId)

    case UpdateUserTaggedPhotoNtf(userId, photoId) =>
      if (!usersTaggedPhotos.get(userId).isEmpty)
        usersTaggedPhotos.get(userId).get.insert(0, photoId)

    case UpdateAlbumPhotoNtf(albumName, photoId) =>
      val defaultAlbumId = getShaOf("default")
      val albumId = getShaOf(albumName)
      if (!albumsPhotos.get(albumId).isEmpty)
        albumsPhotos.get(albumId).get.insert(0, photoId)
      else
        albumsPhotos.get(defaultAlbumId).get.insert(0, photoId)

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

    case CreateUserPhotoReq(userId, photo) =>
      mySubActorCount += 1
      val fbWorkerForUserActivities = context.system.actorOf(Props(new FbWorkerForUserActivities), name = 0.toString + "_FbWorker_" + mySubActorCount.toString)
      val forwardingPair = (sender, fbWorkerForUserActivities)
      forwardingMap += forwardingPair
      fbWorkerForUserActivities ! "Init"
      fbWorkerForUserActivities ! CreateUserPhotoReqToFbWorker(photo, usersOwnPhotos.get(userId).get)

    case CreateUserPhotoRspToFbServer(photoId) =>
      val forwardingMapPair = forwardingMap.remove(forwardingMap.indexWhere(x => {
        x._2 == sender
      }))
      forwardingMapPair._1 ! CreateUserPhotoRsp(photoId)
      forwardingMapPair._2 ! PleaseKillYourself

    case GetUserPhotosReq(userId, typeOfPhotos, startFrom, limit) =>
      mySubActorCount += 1
      val fbWorkerForUserActivities = context.system.actorOf(Props(new FbWorkerForUserActivities), name = 0.toString + "_FbWorker_" + mySubActorCount.toString)
      val forwardingPair = (sender, fbWorkerForUserActivities)
      forwardingMap += forwardingPair
      fbWorkerForUserActivities ! "Init"
      typeOfPhotos match {
        case "own" =>
          fbWorkerForUserActivities ! GetUserPhotosReqToFbWorker(startFrom, limit, usersOwnPhotos.get(userId).get)
        case "tagged" =>
          fbWorkerForUserActivities ! GetUserPhotosReqToFbWorker(startFrom, limit, usersTaggedPhotos.get(userId).get)
      }

    case GetUserPhotosRspToFbServer(photos) =>
      val forwardingMapPair = forwardingMap.remove(forwardingMap.indexWhere(x => {
        x._2 == sender
      }))
      forwardingMapPair._1 ! GetUserPhotosRsp(photos)
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
        usersOwnPhotos.put(key, ListBuffer.empty)
        usersTaggedPhotos.put(key, ListBuffer.empty)
      }
    }
    CreateFbNodeRsp(result, id)
  }

  def getShaOf(s: String): String = {
    val m = MessageDigest.getInstance("SHA-1").digest(s.getBytes("UTF-8"))
    m.map("%02x".format(_)).mkString
  }
}
