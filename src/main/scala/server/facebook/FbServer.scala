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
  var usersFriends: mutable.HashMap[String, ListBuffer[String]] = new mutable.HashMap[String, ListBuffer[String]]()
  var usersInFriends: mutable.HashMap[String, ListBuffer[String]] = new mutable.HashMap[String, ListBuffer[String]]()
  var usersOutFriends: mutable.HashMap[String, ListBuffer[String]] = new mutable.HashMap[String, ListBuffer[String]]()
  var usersFriendsSpecialKeys: mutable.HashMap[String, ListBuffer[(String, String)]] = new mutable.HashMap[String, ListBuffer[(String, String)]]()

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

  var publicKeys: mutable.HashMap[String, Node] = new mutable.HashMap[String, Node]()

  var forwardingMap: ListBuffer[(ActorRef, ActorRef)] = new ListBuffer[(ActorRef, ActorRef)]()
  var mySubActorCount: Int = 0

  val system = ActorSystem("HttpServerTest")
  val statsServerRef = system.actorOf(Props(new StatsServer), "StatsServer")

  def receive = {
    case "Init" =>
      statsServerRef ! "Init"

    case CreateFbNodeReq(nodeType, node) =>
      nodeType match {
        case "user" =>
          val userNode = node.asInstanceOf[UserNode]
          if (userNode.id.isEmpty)
            userNode.id = getShaOf(userNode.first_name)
          sender ! addToDb(users, userNode.id, userNode)

        case "page" =>
          statsServerRef ! "CreateFbNodeReqPage"
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
            photoNode.id = getShaOf(photoNode.from + photoNode.name + photoNode.created_time)
          sender ! addToDb(photos, photoNode.id, photoNode)

        case "album" =>
          val albumNode = node.asInstanceOf[AlbumNode]
          if (albumNode.id.isEmpty)
            albumNode.id = getShaOf(albumNode.from + albumNode.name)
          sender ! addToDb(albums, albumNode.id, albumNode)

        case "publickey" =>
          val publicKeyNode = node.asInstanceOf[PublicKeyNode]
          if (publicKeyNode.id.isEmpty)
            publicKeyNode.id = getShaOf(publicKeyNode.public_Key)
          sender ! addToDb(publicKeys, publicKeyNode.id, publicKeyNode)

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
            pagesLikedUsers.get(pageId).get.remove(pagesLikedUsers.get(pageId).get.indexWhere(x => {
              x == userId
            }))
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

    case UpdateFriendNtf(userId, friendId) =>
      if (users.get(userId).isDefined) {
        if (usersFriends.get(userId).get.contains(friendId)) {
          // bookkeeping
          if (usersInFriends.get(userId).get.contains(friendId))
            usersInFriends.get(userId).get.remove(usersInFriends.get(userId).get.indexWhere(x => {
              x == friendId
            }))
          if (usersOutFriends.get(userId).get.contains(friendId))
            usersOutFriends.get(userId).get.remove(usersOutFriends.get(userId).get.indexWhere(x => {
              x == friendId
            }))
        }
        else if (usersOutFriends.get(userId).get.contains(friendId)) {
          usersOutFriends.get(userId).get.remove(usersOutFriends.get(userId).get.indexWhere(x => {
            x == friendId
          }))
          usersFriends.get(userId).get.insert(0, friendId)
          // bookkeeping
          if (usersInFriends.get(userId).get.contains(friendId))
            usersInFriends.get(userId).get.remove(usersInFriends.get(userId).get.indexWhere(x => {
              x == friendId
            }))
        }
        else if (usersInFriends.get(userId).get.contains(friendId)) {

        }
        else {
          usersInFriends.get(userId).get.insert(0, friendId)
        }
      }

    case Update_InFriends_Remove(userId, friendId) =>
      if (usersInFriends.get(userId).get.contains(friendId))
        usersInFriends.get(userId).get.remove(usersInFriends.get(userId).get.indexWhere(x => {
          x == friendId
        }))

    case Update_OutFriends_Remove(userId, friendId) =>
      if (usersOutFriends.get(userId).get.contains(friendId))
        usersOutFriends.get(userId).get.remove(usersOutFriends.get(userId).get.indexWhere(x => {
          x == friendId
        }))

    case Update_OutFriends_Insert(userId, friendId) =>
      if (!usersOutFriends.get(userId).get.contains(friendId))
        usersOutFriends.get(userId).get.insert(0, friendId)

    case Update_Friends_Insert(userId, friendId) =>
      if (!usersFriends.get(userId).get.contains(friendId))
        usersFriends.get(userId).get.insert(0, friendId)

    case CreateUserPostReq(userId, post) =>
      statsServerRef ! "CreateUserPostReq"
      post.from = userId
      createFbWorkerForUserActivities(sender) ! CreateUserPostReqToFbWorker(post, usersOwnPosts.get(userId).get)

    case CreateUserPostRspToFbServer(postId) =>
      statsServerRef ! "CreateUserPostRsp"
      getRequestor(sender) ! CreateUserPostRsp(postId)

    case GetUserFeedReq(userId, typeOfPosts, startFrom, limit) =>
      statsServerRef ! "GetUserFeedReq"
      typeOfPosts match {
        case "own" =>
          createFbWorkerForUserActivities(sender) ! GetUserPostsReqToFbWorker(startFrom, limit = 10, usersOwnPosts.get(userId).get)
        case "tagged" =>
          createFbWorkerForUserActivities(sender) ! GetUserPostsReqToFbWorker(startFrom, limit = 10, usersTaggedPosts.get(userId).get)
      }

    case GetUserPostsRspToFbServer(posts) =>
      statsServerRef ! "GetUserFeedRsp"
      getRequestor(sender) ! GetUserFeedRsp(posts)

    case CreateUserPhotoReq(userId, photo) =>
      statsServerRef ! "CreateUserPhotoReq"
      photo.from = userId
      createFbWorkerForUserActivities(sender) ! CreateUserPhotoReqToFbWorker(photo, usersOwnPhotos.get(userId).get)

    case CreateUserPhotoRspToFbServer(photoId) =>
      statsServerRef ! "CreateUserPhotoRsp"
      getRequestor(sender) ! CreateUserPhotoRsp(photoId)

    case GetUserPhotosReq(userId, typeOfPhotos, startFrom, limit) =>
      statsServerRef ! "GetUserPhotosReq"
      typeOfPhotos match {
        case "own" =>
          createFbWorkerForUserActivities(sender) ! GetUserPhotosReqToFbWorker(startFrom, limit = 10, usersOwnPhotos.get(userId).get)
        case "tagged" =>
          createFbWorkerForUserActivities(sender) ! GetUserPhotosReqToFbWorker(startFrom, limit = 10, usersTaggedPhotos.get(userId).get)
      }

    case GetUserPhotosRspToFbServer(photos) =>
      statsServerRef ! "GetUserPhotosRsp"
      getRequestor(sender) ! GetUserPhotosRsp(photos)

    case GetUserAlbumsReq(userId, startFrom, limit) =>
      statsServerRef ! "GetUserAlbumsReq"
      createFbWorkerForUserActivities(sender) ! GetUserAlbumsReqToFbWorker(startFrom, limit = 10, usersOwnAlbums.get(userId).get)

    case GetUserAlbumsRspToFbServer(albums) =>
      statsServerRef ! "GetUserAlbumsRsp"
      getRequestor(sender) ! GetUserAlbumsRsp(albums)

    case CreateUserAlbumReq(userId, album) =>
      statsServerRef ! "CreateUserAlbumReq"
      album.from = userId
      createFbWorkerForUserActivities(sender) ! CreateUserAlbumReqToFbWorker(album, usersOwnAlbums.get(userId).get)

    case CreateUserAlbumRspToFbServer(albumId) =>
      statsServerRef ! "CreateUserAlbumRsp"
      getRequestor(sender) ! CreateUserAlbumRsp(albumId)

    case GetAlbumPhotosReq(userId, albumId, startFrom, limit) =>
      statsServerRef ! "GetAlbumPhotosReq"
      if (!albums.get(albumId).isEmpty && albums.get(albumId).get.asInstanceOf[AlbumNode].from.equals(userId))
        createFbWorkerForUserActivities(sender) ! GetAlbumPhotosReqToFbWorker(startFrom, limit = 10, albumsPhotos.get(albumId).get)
      else
        sender ! GetAlbumPhotosRsp(List.empty)

    case GetAlbumPhotosRspToFbServer(photos) =>
      statsServerRef ! "GetAlbumPhotosRsp"
      getRequestor(sender) ! GetAlbumPhotosRsp(photos)

    case AddUserLikedPageReq(userId, pageName) =>
      statsServerRef ! "AddUserLikedPageReq"
      createFbWorkerForUserActivities(sender) ! AddUserLikedPageReqToFbWorker(userId, pageName, usersLikedPages.get(userId).get)

    case AddUserLikedPageRspToFbServer(result) =>
      statsServerRef ! "AddUserLikedPageRsp"
      getRequestor(sender) ! AddUserLikedPageRsp(result)

    case GetUserLikedPagesReq(userId, startFrom, limit) =>
      statsServerRef ! "GetUserLikedPagesReq"
      createFbWorkerForUserActivities(sender) ! GetUserLikedPagesReqToFbWorker(startFrom, limit = 10, usersLikedPages.get(userId).get)

    case GetUserLikedPagesRspToFbServer(pages) =>
      statsServerRef ! "GetUserLikedPagesRsp"
      getRequestor(sender) ! GetUserLikedPagesRsp(pages)

    case GetPageLikedUsersReq(pageId, startFrom, limit) =>
      statsServerRef ! "GetPageLikedUsersReq"
      createFbWorkerForUserActivities(sender) ! GetPageLikedUsersReqToFbWorker(startFrom, limit = 10, pagesLikedUsers.get(pageId).get)

    case GetPageLikedUsersRspToFbServer(users) =>
      statsServerRef ! "GetPageLikedUsersRsp"
      getRequestor(sender) ! GetPageLikedUsersRsp(users)

    case CreatePagePostReq(pageId, post) =>
      statsServerRef ! "CreatePagePostReq"
      post.from = pageId
      createFbWorkerForUserActivities(sender) ! CreatePagePostReqToFbWorker(post, pagesOwnPosts.get(pageId).get, pagesLikedUsers.get(pageId).get)

    case CreatePagePostRspToFbServer(postId) =>
      statsServerRef ! "CreatePagePostRsp"
      getRequestor(sender) ! CreatePagePostRsp(postId)

    case GetUserTimelineReq(userId, startFrom, limit) =>
      statsServerRef ! "GetUserTimelineReq"
      createFbWorkerForUserActivities(sender) ! GetUserTimelineReqToFbWorker(startFrom, limit = 10, usersTimeline.get(userId).get)

    case GetUserTimelineRspToFbServer(events) =>
      statsServerRef ! "GetUserTimelineRsp"
      getRequestor(sender) ! GetUserTimelineRsp(events)

    case CreatePagePhotoReq(pageId, photo) =>
      statsServerRef ! "CreatePagePhotoReq"
      photo.from = pageId
      createFbWorkerForUserActivities(sender) ! CreatePagePhotoReqToFbWorker(photo, pagesOwnPhotos.get(pageId).get, pagesLikedUsers.get(pageId).get)

    case CreatePagePhotoRspToFbServer(photoId) =>
      statsServerRef ! "CreatePagePhotoRsp"
      getRequestor(sender) ! CreatePagePhotoRsp(photoId)

    case RemoveUserLikedPageReq(userId, pageId) =>
      statsServerRef ! "RemoveUserLikedPageReq"
      createFbWorkerForUserActivities(sender) ! RemoveUserLikedPageReqToFbWorker(userId, pageId, usersLikedPages.get(userId).get)

    case RemoveUserLikedPageRspToFbServer(result) =>
      statsServerRef ! "RemoveUserLikedPageRsp"
      getRequestor(sender) ! RemoveUserLikedPageRsp(result)

    case AddFriendReq(userId, friendName) =>
      statsServerRef ! "AddFriendReq"
      val addFriendReqToFbWorker = AddFriendReqToFbWorker(
        userId,
        friendName,
        usersFriends.get(userId).get,
        usersInFriends.get(userId).get,
        usersOutFriends.get(userId).get)

      createFbWorkerForUserActivities(sender) ! addFriendReqToFbWorker

    case AddFriendRspToFbServer(result) =>
      statsServerRef ! "AddFriendRsp"
      getRequestor(sender) ! AddFriendRsp(result)

    case GetPendingInFriendsReq(userId) =>
      statsServerRef ! "GetPendingInFriendsReq"
      createFbWorkerForUserActivities(sender) ! GetPendingInFriendsReqToFbWorker(usersInFriends.get(userId).get)

    case GetPendingInFriendsRspToFbServer(inFriendName) =>
      statsServerRef ! "GetPendingInFriendsRsp"
      getRequestor(sender) ! GetPendingInFriendsRsp(inFriendName)

    case GetFriendsReq(userId) =>
      statsServerRef ! "GetFriendsReq"
      createFbWorkerForUserActivities(sender) ! GetFriendsReqToFbWorker(usersFriends.get(userId).get, usersFriendsSpecialKeys.get(userId).get)

    case GetFriendsRspToFbServer(friends) =>
      statsServerRef ! "GetFriendsRsp"
      getRequestor(sender) ! GetFriendsRsp(friends)

    case CreateUserReq(user) =>
      statsServerRef ! "CreateFbNodeReqUser"
      createFbWorkerForUserActivities(sender) ! CreateUserReqToFbWorker(user)

    case CreateUserRspToFbServer(result, id) =>
      getRequestor(sender) ! CreateUserRsp(result, id)

    case GetFriendDetailsReq(userId, friendName) =>
      val getFriendDetailsReqToFbWorker = GetFriendDetailsReqToFbWorker(usersFriends.get(userId).get, friendName, usersFriendsSpecialKeys.get(userId).get)
      createFbWorkerForUserActivities(sender) ! getFriendDetailsReqToFbWorker

    case GetFriendDetailsRspToFbServer(friendNode) =>
      getRequestor(sender) ! GetFriendDetailsRsp(friendNode)

    case AddSpecialKeyToFriendReq(userId, friendName, encrypted_special_key) =>
      val specialKey = (userId, encrypted_special_key)
      val friendId = getShaOf(friendName)
      if (usersFriendsSpecialKeys.get(friendId).get.find(x => {
        x._1 == userId
      }).isEmpty)
        usersFriendsSpecialKeys.get(friendId).get.insert(0, specialKey)
      sender ! AddSpecialKeyToFriendRsp(true)
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
        usersFriends.put(key, ListBuffer.empty)
        usersInFriends.put(key, ListBuffer.empty)
        usersOutFriends.put(key, ListBuffer.empty)
        usersFriendsSpecialKeys.put(key, ListBuffer.empty)
      }
      else if (db == albums) {
        albumsPhotos.put(key, ListBuffer.empty)
      }
      else if (db == pages) {
        pagesLikedUsers.put(key, ListBuffer.empty)
        pagesOwnPosts.put(key, ListBuffer.empty)
        pagesOwnPhotos.put(key, ListBuffer.empty)
      }
      else if (db == publicKeys) {
        // nothing for now, hopefully nothing forever
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
