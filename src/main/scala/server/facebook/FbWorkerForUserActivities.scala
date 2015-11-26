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

    case CreateUserPostReqToFbWorker(post, ownPosts) =>
      // make proper post with id
      post.id = getShaOf(post.message + post.from)
      post.to = parseTextAndFindUsernames(post.message)

      // create the post on server that will host it
      val future: Future[CreateFbNodeRsp] = (myFbServerRef ? CreateFbNodeReq("post", post)).mapTo[CreateFbNodeRsp]
      val createFbNodeRsp = Await.result(future, someTimeout.duration)
      if (createFbNodeRsp.result) {

      }

      ownPosts.insert(0, post.id)

      post.to.foreach(taggedUser => {
        // please don't tag yourself
        myFbServerRef ! UpdateUserTaggedPostNtf(getShaOf(taggedUser), post.id)
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

    case CreateUserPhotoReqToFbWorker(photo, ownPhotos) =>
      photo.id = getShaOf(photo.name + photo.created_time)
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
      photoIds.foreach(photoId => {
        val future: Future[GetFbNodeRsp] = (myFbServerRef ? GetFbNodeReq("photo", photoId)).mapTo[GetFbNodeRsp]
        val getFbNodeRsp = Await.result(future, someTimeout.duration)
        photos += getFbNodeRsp.node.asInstanceOf[PhotoNode]
      })

      myFbServerRef ! GetUserPhotosRspToFbServer(photos.toList)

    case GetUserAlbumsReqToFbWorker(startFrom, limit, albumIds) =>
      val albums: ListBuffer[AlbumNode] = new ListBuffer[AlbumNode]()
      albumIds.foreach(albumId => {
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
      photoIds.foreach(photoId => {
        val future: Future[GetFbNodeRsp] = (myFbServerRef ? GetFbNodeReq("photo", photoId)).mapTo[GetFbNodeRsp]
        val getFbNodeRsp = Await.result(future, someTimeout.duration)
        photos += getFbNodeRsp.node.asInstanceOf[PhotoNode]
      })

      myFbServerRef ! GetAlbumPhotosRspToFbServer(photos.toList)

    case AddUserLikedPageReqToFbWorker(userId, pageName, ownLikedPages) =>
      val pageId = getShaOf(pageName)

      val future: Future[UpdatePageLikedUserRsp] = (myFbServerRef ? UpdatePageLikedUserReq(pageId, userId)).mapTo[UpdatePageLikedUserRsp]
      val updatePageLikedUserRsp = Await.result(future, someTimeout.duration)
      if (updatePageLikedUserRsp.result) {
        ownLikedPages.insert(0, pageId)
      }

      myFbServerRef ! AddUserLikedPageRspToFbServer(updatePageLikedUserRsp.result)


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