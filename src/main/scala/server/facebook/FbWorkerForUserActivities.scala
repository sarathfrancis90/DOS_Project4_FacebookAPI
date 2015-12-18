package server.facebook

import java.security.MessageDigest
import java.util.Calendar

import akka.actor.{Actor, ActorLogging, ActorRef}
import akka.pattern.ask
import akka.util.Timeout

import scala.collection.mutable.ListBuffer
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.language.postfixOps

class FbWorkerForUserActivities extends Actor with ActorLogging {
  implicit val someTimeout = Timeout(5 seconds)
  var myFbServerRef: ActorRef = _

  def receive = {
    case "Init" =>
      myFbServerRef = sender

    case CreateUserPostReqToFbWorker(post, ownPosts, ownFriends) =>

      val futureGetFbNodeRsp: Future[GetFbNodeRsp] = (myFbServerRef ? GetFbNodeReq("user", post.from)).mapTo[GetFbNodeRsp]
      val getFbNodeRsp = Await.result(futureGetFbNodeRsp, someTimeout.duration)
      post.from_name = getFbNodeRsp.node.asInstanceOf[UserNode].first_name

      if (!post.to_all_friends) {
        if (post.encrypted) {
          val to: ListBuffer[String] = new ListBuffer[String]()
          post.encrypted_secret_keys.foreach(encrypted_secret_key => {
            if (!encrypted_secret_key.to.equals("self"))
              to += encrypted_secret_key.to
          })
          post.to = to.toList
        }
        else post.to = parseTextAndFindUsernames(post.message)
      } else post.to = ownFriends.toList

      val future: Future[CreateFbNodeRsp] = (myFbServerRef ? CreateFbNodeReq("post", post)).mapTo[CreateFbNodeRsp]
      val createFbNodeRsp = Await.result(future, someTimeout.duration)
      if (createFbNodeRsp.result) {
        post.id = createFbNodeRsp.id

        ownPosts.insert(0, post.id)

        if (post.to_all_friends) {
          post.to.foreach(taggedUserId => {
            // please don't tag yourself
            myFbServerRef ! UpdateUserTaggedPostNtf(taggedUserId, post.id)
          })
        }
        else {
          post.to.foreach(taggedUser => {
            // please don't tag yourself
            myFbServerRef ! UpdateUserTaggedPostNtf(getShaOf(taggedUser), post.id)
          })
        }


        myFbServerRef ! CreateUserPostRspToFbServer(post.id)

      }
      else
        myFbServerRef ! CreateUserPostRspToFbServer("")

    case GetUserPostsReqToFbWorker(startFrom, limit, postIds: ListBuffer[String], userId) =>
      val futureGetFbNodeRsp: Future[GetFbNodeRsp] = (myFbServerRef ? GetFbNodeReq("user", userId)).mapTo[GetFbNodeRsp]
      val getFbNodeRsp = Await.result(futureGetFbNodeRsp, someTimeout.duration)
      val first_name = getFbNodeRsp.node.asInstanceOf[UserNode].first_name

      val posts: ListBuffer[PostNode] = new ListBuffer[PostNode]()
      postIds.foreach(postId => {
        val future: Future[GetFbNodeRsp] = (myFbServerRef ? GetFbNodeReq("post", postId)).mapTo[GetFbNodeRsp]
        val getFbNodeRsp = Await.result(future, someTimeout.duration)
        val post = getFbNodeRsp.node.asInstanceOf[PostNode].copy()
        post.encrypted_secret_keys = post.encrypted_secret_keys.filter(x => {
          x.to == "self" || x.to == first_name
        })
        posts += post
      })

      myFbServerRef ! GetUserPostsRspToFbServer(posts.toList)

    case CreateUserPhotoReqToFbWorker(photo, ownPhotos) =>
      photo.id = getShaOf(photo.from + photo.name + photo.created_time)
      val taggedUsers = parseTextAndFindUsernames(photo.caption)

      if (ownPhotos.isEmpty) {
        val now = Calendar.getInstance().getTime.toString
        val album = new AlbumNode(id = "", count = 0, cover_photo = "", created_time = now, description = "", from = photo.from, name = "all", album_type = "", updated_time = now)
        val future: Future[CreateFbNodeRsp] = (myFbServerRef ? CreateFbNodeReq("album", album)).mapTo[CreateFbNodeRsp]
        val createFbNodeRsp = Await.result(future, someTimeout.duration)
        if (createFbNodeRsp.result) {
          album.id = createFbNodeRsp.id
          myFbServerRef ! UpdateUserAlbumNtf(photo.from, album.id)
        }
      }

      val future: Future[CreateFbNodeRsp] = (myFbServerRef ? CreateFbNodeReq("photo", photo)).mapTo[CreateFbNodeRsp]
      val createFbNodeRsp = Await.result(future, someTimeout.duration)
      if (createFbNodeRsp.result) {

      }

      ownPhotos.insert(0, photo.id)

      taggedUsers.foreach(taggedUser => {
        // tagging self is okay?
        myFbServerRef ! UpdateUserTaggedPhotoNtf(getShaOf(taggedUser), photo.id)
      })

      myFbServerRef ! UpdateAlbumPhotoNtf(getShaOf(photo.from + photo.album), photo.id)

      if (!"all".equals(photo.album))
        myFbServerRef ! UpdateAlbumPhotoNtf(getShaOf(photo.from + "all"), photo.id)

      myFbServerRef ! CreateUserPhotoRspToFbServer(photo.id)

    case GetUserPhotosReqToFbWorker(startFrom, limit, photoIds) =>
      val photos: ListBuffer[PhotoNode] = new ListBuffer[PhotoNode]()
      photoIds.take(limit).foreach(photoId => {
        val future: Future[GetFbNodeRsp] = (myFbServerRef ? GetFbNodeReq("photo", photoId)).mapTo[GetFbNodeRsp]
        val getFbNodeRsp = Await.result(future, someTimeout.duration)
        photos += getFbNodeRsp.node.asInstanceOf[PhotoNode]
      })

      myFbServerRef ! GetUserPhotosRspToFbServer(photos.toList)

    case GetUserAlbumsReqToFbWorker(startFrom, limit, albumIds) =>
      val albums: ListBuffer[AlbumNode] = new ListBuffer[AlbumNode]()
      albumIds.take(limit).foreach(albumId => {
        val future: Future[GetFbNodeRsp] = (myFbServerRef ? GetFbNodeReq("album", albumId)).mapTo[GetFbNodeRsp]
        val getFbNodeRsp = Await.result(future, someTimeout.duration)
        albums += getFbNodeRsp.node.asInstanceOf[AlbumNode]
      })

      myFbServerRef ! GetUserAlbumsRspToFbServer(albums.toList)

    case CreateUserAlbumReqToFbWorker(album, ownAlbums) =>
      album.id = getShaOf(album.from + album.name)

      val future: Future[CreateFbNodeRsp] = (myFbServerRef ? CreateFbNodeReq("album", album)).mapTo[CreateFbNodeRsp]
      val createFbNodeRsp = Await.result(future, someTimeout.duration)
      if (createFbNodeRsp.result) {

      }

      ownAlbums.insert(0, album.id)

      myFbServerRef ! CreateUserAlbumRspToFbServer(album.id)

    case GetAlbumPhotosReqToFbWorker(startFrom, limit, photoIds) =>
      val photos: ListBuffer[PhotoNode] = new ListBuffer[PhotoNode]()
      photoIds.take(limit).foreach(photoId => {
        val future: Future[GetFbNodeRsp] = (myFbServerRef ? GetFbNodeReq("photo", photoId)).mapTo[GetFbNodeRsp]
        val getFbNodeRsp = Await.result(future, someTimeout.duration)
        photos += getFbNodeRsp.node.asInstanceOf[PhotoNode]
      })

      myFbServerRef ! GetAlbumPhotosRspToFbServer(photos.toList)

    case AddUserLikedPageReqToFbWorker(userId, pageName, ownLikedPages) =>
      val pageId = getShaOf(pageName)

      val future: Future[UpdatePageLikedUserRsp] = (myFbServerRef ? UpdatePageLikedUserReq("add", pageId, userId)).mapTo[UpdatePageLikedUserRsp]
      val updatePageLikedUserRsp = Await.result(future, someTimeout.duration)
      if (updatePageLikedUserRsp.result) {
        ownLikedPages.insert(0, pageId)
      }

      myFbServerRef ! AddUserLikedPageRspToFbServer(updatePageLikedUserRsp.result)

    case GetUserLikedPagesReqToFbWorker(startFrom, limit, pageIds) =>
      val pages: ListBuffer[PageNode] = new ListBuffer[PageNode]()
      pageIds.take(limit).foreach(pageId => {
        val future: Future[GetFbNodeRsp] = (myFbServerRef ? GetFbNodeReq("page", pageId)).mapTo[GetFbNodeRsp]
        val getFbNodeRsp = Await.result(future, someTimeout.duration)
        pages += getFbNodeRsp.node.asInstanceOf[PageNode]
      })

      myFbServerRef ! GetUserLikedPagesRspToFbServer(pages.toList)

    case GetPageLikedUsersReqToFbWorker(startFrom, limit, likedUserIds) =>
      val users: ListBuffer[UserNode] = new ListBuffer[UserNode]()
      likedUserIds.take(0).foreach(likedUserId => {
        val future: Future[GetFbNodeRsp] = (myFbServerRef ? GetFbNodeReq("user", likedUserId)).mapTo[GetFbNodeRsp]
        val getFbNodeRsp = Await.result(future, someTimeout.duration)
        users += getFbNodeRsp.node.asInstanceOf[UserNode]
      })

      myFbServerRef ! GetPageLikedUsersRspToFbServer(users.toList)

    case CreatePagePostReqToFbWorker(post, ownPosts, likedUsers) =>
      val future: Future[CreateFbNodeRsp] = (myFbServerRef ? CreateFbNodeReq("post", post)).mapTo[CreateFbNodeRsp]
      val createFbNodeRsp = Await.result(future, someTimeout.duration)
      if (createFbNodeRsp.result) {
        post.id = createFbNodeRsp.id
        ownPosts.insert(0, post.id)

        likedUsers.foreach(likedUser => {
          myFbServerRef ! UpdateUserTimelineNtf(likedUser, "post", post.id)
        })

        myFbServerRef ! CreatePagePostRspToFbServer(post.id)
      }
      else
        myFbServerRef ! CreatePagePostRspToFbServer("")

    case GetUserTimelineReqToFbWorker(startFrom, limit, timelineEvents) =>
      val events: ListBuffer[(String, Node)] = new ListBuffer[(String, Node)]()
      timelineEvents.take(limit).foreach(timelineEvent => {
        val future: Future[GetFbNodeRsp] = (myFbServerRef ? GetFbNodeReq(timelineEvent._1, timelineEvent._2)).mapTo[GetFbNodeRsp]
        val getFbNodeRsp = Await.result(future, someTimeout.duration)
        val event = (timelineEvent._1, getFbNodeRsp.node)
        events += event
      })

      myFbServerRef ! GetUserTimelineRspToFbServer(events.toList)

    case CreatePagePhotoReqToFbWorker(photo, ownPhotos, likedUsers) =>
      val future: Future[CreateFbNodeRsp] = (myFbServerRef ? CreateFbNodeReq("photo", photo)).mapTo[CreateFbNodeRsp]
      val createFbNodeRsp = Await.result(future, someTimeout.duration)
      if (createFbNodeRsp.result) {
        photo.id = createFbNodeRsp.id
        ownPhotos.insert(0, photo.id)

        likedUsers.foreach(likedUser => {
          myFbServerRef ! UpdateUserTimelineNtf(likedUser, "photo", photo.id)
        })

        myFbServerRef ! CreatePagePhotoRspToFbServer(photo.id)
      }
      else
        myFbServerRef ! CreatePagePhotoRspToFbServer("")

    case RemoveUserLikedPageReqToFbWorker(userId, pageId, ownLikedPages) =>
      val future: Future[UpdatePageLikedUserRsp] = (myFbServerRef ? UpdatePageLikedUserReq("remove", pageId, userId)).mapTo[UpdatePageLikedUserRsp]
      val updatePageLikedUserRsp = Await.result(future, someTimeout.duration)
      if (updatePageLikedUserRsp.result) {
        ownLikedPages.remove(ownLikedPages.indexWhere(x => {
          x == pageId
        }))
      }

      myFbServerRef ! RemoveUserLikedPageRspToFbServer(updatePageLikedUserRsp.result)

    case AddFriendReqToFbWorker(userId, friendName, ownFriends, ownInFriends, ownOutFriends) =>
      val friendId = getShaOf(friendName)

      if (ownFriends.contains(friendId)) {
        myFbServerRef ! AddFriendRspToFbServer(s"Already friends with $friendName")
        // bookkeeping
        if (ownInFriends.contains(friendId))
          myFbServerRef ! Update_InFriends_Remove(userId, friendId)

        if (ownOutFriends.contains(friendId))
          myFbServerRef ! Update_OutFriends_Remove(userId, friendId)
      }
      else if (ownInFriends.contains(friendId)) {
        myFbServerRef ! Update_InFriends_Remove(userId, friendId)
        myFbServerRef ! Update_Friends_Insert(userId, friendId)
        myFbServerRef ! UpdateFriendNtf(friendId, userId)
        myFbServerRef ! AddFriendRspToFbServer(s"Now friends with $friendName")
        // bookkeeping
        if (ownOutFriends.contains(friendId))
          myFbServerRef ! Update_OutFriends_Remove(userId, friendId)
      }
      else if (ownOutFriends.contains(friendId)) {
        myFbServerRef ! AddFriendRspToFbServer(s"Friend request was sent already to $friendName")
      }
      else {
        myFbServerRef ! Update_OutFriends_Insert(userId, friendId)
        myFbServerRef ! UpdateFriendNtf(friendId, userId)
        myFbServerRef ! AddFriendRspToFbServer(s"Friend request sent to $friendName")
      }

    case GetPendingInFriendsReqToFbWorker(inFriends) =>
      val friendNames: ListBuffer[String] = new ListBuffer[String]()
      inFriends.foreach(inFriend => {
        val future: Future[GetFbNodeRsp] = (myFbServerRef ? GetFbNodeReq("user", inFriend)).mapTo[GetFbNodeRsp]
        val getFbNodeRsp = Await.result(future, someTimeout.duration)
        friendNames += getFbNodeRsp.node.asInstanceOf[UserNode].first_name
      })

      myFbServerRef ! GetPendingInFriendsRspToFbServer(friendNames.toList)

    case GetFriendsReqToFbWorker(ownFriends, friendsSpecialKeys) =>
      val friends: ListBuffer[UserNode] = new ListBuffer[UserNode]()
      ownFriends.foreach(ownFriend => {
        val future: Future[GetFbNodeRsp] = (myFbServerRef ? GetFbNodeReq("user", ownFriend)).mapTo[GetFbNodeRsp]
        val getFbNodeRsp = Await.result(future, someTimeout.duration)
        val friend = getFbNodeRsp.node.asInstanceOf[UserNode].copy()
        friend.encrypted_special_key = friendsSpecialKeys.find(x => {
          x == friend.id
        }).getOrElse(("", ""))._2
        friends += friend
      })

      myFbServerRef ! GetFriendsRspToFbServer(friends.toList)

    case CreateUserReqToFbWorker(user) =>
      val publicKeyNode = PublicKeyNode("", user.public_key)
      val future: Future[CreateFbNodeRsp] = (myFbServerRef ? CreateFbNodeReq("publickey", publicKeyNode)).mapTo[CreateFbNodeRsp]
      val createFbNodeRsp = Await.result(future, someTimeout.duration)
      if (createFbNodeRsp.result) {
        val future: Future[CreateFbNodeRsp] = (myFbServerRef ? CreateFbNodeReq("user", user)).mapTo[CreateFbNodeRsp]
        val createFbNodeRsp = Await.result(future, someTimeout.duration)
        if (createFbNodeRsp.result) {
          myFbServerRef ! CreateUserRspToFbServer(true, createFbNodeRsp.id)
        }
        else
          myFbServerRef ! CreateUserRspToFbServer(false, "")
      }
      else
        myFbServerRef ! CreateUserRspToFbServer(false, "")

    case GetFriendDetailsReqToFbWorker(ownFriends, friendName, friendsSpecialKeys) =>
      val friendId = getShaOf(friendName)
      var friendNode = None: Option[UserNode]


      if (ownFriends.contains(friendId)) {
        val future: Future[GetFbNodeRsp] = (myFbServerRef ? GetFbNodeReq("user", friendId)).mapTo[GetFbNodeRsp]
        val getFbNodeRsp = Await.result(future, someTimeout.duration)
        val friend = getFbNodeRsp.node.asInstanceOf[UserNode].copy()
        friend.encrypted_special_key = friendsSpecialKeys.find(x => {
          x._1.equals(friend.id)
        }).getOrElse(("", ""))._2
        friendNode = Some(friend)
      }


      myFbServerRef ! GetFriendDetailsRspToFbServer(friendNode)

    case PleaseKillYourself =>
      context.stop(self)
  }

  def getShaOf(s: String): String = {
    val m = MessageDigest.getInstance("SHA-1").digest(s.getBytes("UTF-8"))
    m.map("%02x".format(_)).mkString
  }

  def parseTextAndFindUsernames(message: String): List[String] = {
    val usernames: ListBuffer[String] = new ListBuffer[String]()
    val usernamePattern = "@([A-Za-z0-9_]+)".r
    val matches = usernamePattern.findAllIn(message)
    matches.foreach(aMatch => {
      usernames += aMatch.drop(1)
    })
    usernames.toList
  }
}