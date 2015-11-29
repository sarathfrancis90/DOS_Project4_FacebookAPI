package server.facebook

import java.security.MessageDigest
import java.util.Calendar

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
  var usersOwnAlbums: mutable.HashMap[String, ListBuffer[String]] = new mutable.HashMap[String, ListBuffer[String]]()
  var usersLikedPages: mutable.HashMap[String, ListBuffer[String]] = new mutable.HashMap[String, ListBuffer[String]]()
  var usersTimeline: mutable.HashMap[String, ListBuffer[(String, String)]] = new mutable.HashMap[String, ListBuffer[(String, String)]]()

  var pages: mutable.HashMap[String, Node] = new mutable.HashMap[String, Node]()
  var pagesLikedUsers: mutable.HashMap[String, ListBuffer[String]] = new mutable.HashMap[String, ListBuffer[String]]()
  var pagesOwnPosts: mutable.HashMap[String, ListBuffer[String]] = new mutable.HashMap[String, ListBuffer[String]]()
  var pagesOwnPhotos: mutable.HashMap[String, ListBuffer[String]] = new mutable.HashMap[String, ListBuffer[String]]()

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
            userNode.id = getShaOf(userNode.first_name)
          sender ! addToDb(users, userNode.id, userNode)

        case "page" =>
          val pageNode = node.asInstanceOf[PageNode]
          if (pageNode.id.isEmpty)
            pageNode.id = getShaOf(pageNode.name)
          sender ! addToDb(pages, pageNode.id, pageNode)

        case "post" =>
          val postNode = node.asInstanceOf[PostNode]
          if (postNode.id.isEmpty)
            postNode.id = getShaOf(postNode.from + postNode.message + postNode.created_time)
          sender ! addToDb(posts, postNode.id, postNode)

        case "photo" =>
          val photoNode = node.asInstanceOf[PhotoNode]
          if (photoNode.id.isEmpty)
            photoNode.id = getShaOf(photoNode.name + photoNode.created_time)
          sender ! addToDb(photos, photoNode.id, photoNode)

        case "album" =>
          val albumNode = node.asInstanceOf[AlbumNode]
          if (albumNode.id.isEmpty)
            albumNode.id = getShaOf(albumNode.from + albumNode.name)
          sender ! addToDb(albums, albumNode.id, albumNode)
      }

    case GetFbNodeReq(nodeType, nodeId) =>
      nodeType match {
        case "user" =>
          sender ! GetFbNodeRsp(users.get(nodeId).get)

        case "page" =>
          sender ! GetFbNodeRsp(pages.get(nodeId).get)

        case "post" =>
          sender ! GetFbNodeRsp(posts.get(nodeId).get)

        case "photo" =>
          sender ! GetFbNodeRsp(photos.get(nodeId).get)

        case "album" =>
          sender ! GetFbNodeRsp(albums.get(nodeId).get)
      }

    case UpdateUserTaggedPostNtf(userId, postId) =>
      if (!usersTaggedPosts.get(userId).isEmpty)
        usersTaggedPosts.get(userId).get.insert(0, postId)

    case UpdateUserTaggedPhotoNtf(userId, photoId) =>
      if (!usersTaggedPhotos.get(userId).isEmpty)
        usersTaggedPhotos.get(userId).get.insert(0, photoId)

    case UpdateAlbumPhotoNtf(albumId, photoId) =>
      if (!albumsPhotos.get(albumId).isEmpty) {
        albumsPhotos.get(albumId).get.insert(0, photoId)
        val album = albums.get(albumId).get.asInstanceOf[AlbumNode]
        album.count += 1
        album.updated_time = Calendar.getInstance().getTime.toString
        albums.put(albumId, album)
      }

    case UpdateUserAlbumNtf(userId, albumId) =>
      if (!usersOwnAlbums.get(userId).isEmpty)
        usersOwnAlbums.get(userId).get.insert(0, albumId)

    case UpdatePageLikedUserReq(action, pageId, userId) =>
      if (!pagesLikedUsers.get(pageId).isEmpty) {
        if ("add".equals(action)) {
          if (!pagesLikedUsers.get(pageId).get.contains(userId)) {
            pagesLikedUsers.get(pageId).get.insert(0, userId)
            val page = pages.get(pageId).get.asInstanceOf[PageNode]
            page.likes += 1
            pages.put(pageId, page)
          }
        }
        else {
          if (pagesLikedUsers.get(pageId).get.contains(userId)) {
            pagesLikedUsers.get(pageId).get.remove(pagesLikedUsers.get(pageId).get.indexWhere(x=>{x==userId}))
            val page = pages.get(pageId).get.asInstanceOf[PageNode]
            page.likes -= 1
            pages.put(pageId, page)
          }
        }
        sender ! UpdatePageLikedUserRsp(true)
      }
      else
        sender ! UpdatePageLikedUserRsp(false)

    case UpdateUserTimelineNtf(userId, eventType, eventId) =>
      if (!usersTimeline.get(userId).isEmpty) {
        val newEvent = (eventType, eventId)
        usersTimeline.get(userId).get.insert(0, newEvent)
      }

    case CreateUserPostReq(userId, post) =>
      post.from = userId
      createFbWorkerForUserActivities(sender) ! CreateUserPostReqToFbWorker(post, usersOwnPosts.get(userId).get)

    case CreateUserPostRspToFbServer(postId) =>
      getRequestor(sender) ! CreateUserPostRsp(postId)

    case GetUserFeedReq(userId, typeOfPosts, startFrom, limit) =>
      typeOfPosts match {
        case "own" =>
          createFbWorkerForUserActivities(sender) ! GetUserPostsReqToFbWorker(startFrom, limit=10, usersOwnPosts.get(userId).get)
        case "tagged" =>
          createFbWorkerForUserActivities(sender) ! GetUserPostsReqToFbWorker(startFrom, limit=10, usersTaggedPosts.get(userId).get)
      }

    case GetUserPostsRspToFbServer(posts) =>
      getRequestor(sender) ! GetUserFeedRsp(posts)

    case CreateUserPhotoReq(userId, photo) =>
      photo.from = userId
      createFbWorkerForUserActivities(sender) ! CreateUserPhotoReqToFbWorker(photo, usersOwnPhotos.get(userId).get)

    case CreateUserPhotoRspToFbServer(photoId) =>
      getRequestor(sender) ! CreateUserPhotoRsp(photoId)

    case GetUserPhotosReq(userId, typeOfPhotos, startFrom, limit) =>
      typeOfPhotos match {
        case "own" =>
          createFbWorkerForUserActivities(sender) ! GetUserPhotosReqToFbWorker(startFrom, limit=10, usersOwnPhotos.get(userId).get)
        case "tagged" =>
          createFbWorkerForUserActivities(sender) ! GetUserPhotosReqToFbWorker(startFrom, limit=10, usersTaggedPhotos.get(userId).get)
      }

    case GetUserPhotosRspToFbServer(photos) =>
      getRequestor(sender) ! GetUserPhotosRsp(photos)

    case GetUserAlbumsReq(userId, startFrom, limit) =>
      createFbWorkerForUserActivities(sender) ! GetUserAlbumsReqToFbWorker(startFrom, limit=10, usersOwnAlbums.get(userId).get)

    case GetUserAlbumsRspToFbServer(albums) =>
      getRequestor(sender) ! GetUserAlbumsRsp(albums)

    case CreateUserAlbumReq(userId, album) =>
      album.from = userId
      createFbWorkerForUserActivities(sender) ! CreateUserAlbumReqToFbWorker(album, usersOwnAlbums.get(userId).get)

    case CreateUserAlbumRspToFbServer(albumId) =>
      getRequestor(sender) ! CreateUserAlbumRsp(albumId)

    case GetAlbumPhotosReq(userId, albumId, startFrom, limit) =>
      if (!albums.get(albumId).isEmpty && albums.get(albumId).get.asInstanceOf[AlbumNode].from.equals(userId))
        createFbWorkerForUserActivities(sender) ! GetAlbumPhotosReqToFbWorker(startFrom, limit=10, albumsPhotos.get(albumId).get)
      else
        sender ! GetAlbumPhotosRsp(List.empty)

    case GetAlbumPhotosRspToFbServer(photos) =>
      getRequestor(sender) ! GetAlbumPhotosRsp(photos)

    case AddUserLikedPageReq(userId, pageName) =>
      createFbWorkerForUserActivities(sender) ! AddUserLikedPageReqToFbWorker(userId, pageName, usersLikedPages.get(userId).get)

    case AddUserLikedPageRspToFbServer(result) =>
      getRequestor(sender) ! AddUserLikedPageRsp(result)

    case GetUserLikedPagesReq(userId, startFrom, limit) =>
      createFbWorkerForUserActivities(sender) ! GetUserLikedPagesReqToFbWorker(startFrom, limit=10, usersLikedPages.get(userId).get)

    case GetUserLikedPagesRspToFbServer(pages) =>
      getRequestor(sender) ! GetUserLikedPagesRsp(pages)

    case GetPageLikedUsersReq(pageId, startFrom, limit) =>
      createFbWorkerForUserActivities(sender) ! GetPageLikedUsersReqToFbWorker(startFrom, limit=10, pagesLikedUsers.get(pageId).get)

    case GetPageLikedUsersRspToFbServer(users) =>
      getRequestor(sender) ! GetPageLikedUsersRsp(users)

    case CreatePagePostReq(pageId, post) =>
      post.from = pageId
      createFbWorkerForUserActivities(sender) ! CreatePagePostReqToFbWorker(post, pagesOwnPosts.get(pageId).get, pagesLikedUsers.get(pageId).get)

    case CreatePagePostRspToFbServer(postId) =>
      getRequestor(sender) ! CreatePagePostRsp(postId)

    case GetUserTimelineReq(userId, startFrom, limit) =>
      createFbWorkerForUserActivities(sender) ! GetUserTimelineReqToFbWorker(startFrom, limit=10, usersTimeline.get(userId).get)

    case GetUserTimelineRspToFbServer(events) =>
      getRequestor(sender) ! GetUserTimelineRsp(events)

    case CreatePagePhotoReq(pageId, photo) =>
      photo.from = pageId
      createFbWorkerForUserActivities(sender) ! CreatePagePhotoReqToFbWorker(photo, pagesOwnPhotos.get(pageId).get, pagesLikedUsers.get(pageId).get)

    case CreatePagePhotoRspToFbServer(photoId) =>
      getRequestor(sender) ! CreatePagePhotoRsp(photoId)

    case RemoveUserLikedPageReq(userId, pageId) =>
      createFbWorkerForUserActivities(sender) ! RemoveUserLikedPageReqToFbWorker(userId, pageId, usersLikedPages.get(userId).get)

    case RemoveUserLikedPageRspToFbServer(result) =>
      getRequestor(sender) ! RemoveUserLikedPageRsp(result)
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
        usersOwnAlbums.put(key, ListBuffer.empty)
        usersLikedPages.put(key, ListBuffer.empty)
        usersTimeline.put(key, ListBuffer.empty)
      }
      else if (db == albums) {
        albumsPhotos.put(key, ListBuffer.empty)
      } else if (db == pages) {
        pagesLikedUsers.put(key, ListBuffer.empty)
        pagesOwnPosts.put(key, ListBuffer.empty)
        pagesOwnPhotos.put(key, ListBuffer.empty)
      }
    }
    CreateFbNodeRsp(result, id)
  }

  def getShaOf(s: String): String = {
    val m = MessageDigest.getInstance("SHA-1").digest(s.getBytes("UTF-8"))
    m.map("%02x".format(_)).mkString
  }

  def createFbWorkerForUserActivities(sender: ActorRef): ActorRef = {
    mySubActorCount += 1
    val fbWorkerForUserActivities = context.system.actorOf(Props(new FbWorkerForUserActivities), name = 0.toString + "_FbWorker_" + mySubActorCount.toString)
    val forwardingPair = (sender, fbWorkerForUserActivities)
    forwardingMap += forwardingPair
    fbWorkerForUserActivities ! "Init"
    fbWorkerForUserActivities
  }

  def getRequestor(sender: ActorRef): ActorRef = {
    val forwardingMapPair = forwardingMap.remove(forwardingMap.indexWhere(x => {
      x._2 == sender
    }))
    forwardingMapPair._2 ! PleaseKillYourself
    forwardingMapPair._1
  }
}
