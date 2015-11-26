package server.facebook

import java.util.Calendar

import akka.actor._
import akka.pattern.ask
import akka.util.Timeout

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.language.postfixOps

object ServerTest {
  def main(args: Array[String]) {
    implicit val someTimeout = Timeout(5 seconds)
    val system = ActorSystem("ServerTest")
    val server00 = system.actorOf(Props(new FbServer), "FbServer00")

    val user00 = new UserNode("", "About user00", "1/1/1900", "user00@gmail.com", "User00")
    val user01 = new UserNode("", "About user01", "1/1/1900", "user01@gmail.com", "User01")

    var future: Future[CreateFbNodeRsp] = (server00 ? CreateFbNodeReq("user", user00)).mapTo[CreateFbNodeRsp]
    var createFbNodeRsp = Await.result(future, someTimeout.duration)
    if (createFbNodeRsp.result) {
      println("user00 added")
      user00.id = createFbNodeRsp.id
    }

    future = (server00 ? CreateFbNodeReq("user", user01)).mapTo[CreateFbNodeRsp]
    createFbNodeRsp = Await.result(future, someTimeout.duration)
    if (createFbNodeRsp.result) {
      println("user01 added")
      user01.id = createFbNodeRsp.id
    }

    val now = Calendar.getInstance().getTime.toString

    val post00 = new PostNode("", now, "post00 desc", "", "I'm tagging @User01@User02 in post00", List.empty, now)
    val post01 = new PostNode("", now, "post01 desc", "", "I'm tagging nobody in post01", List.empty, now)
    val post02 = new PostNode("", now, "post02 desc", "", "I'm tagging @User02@User01 in post02", List.empty, now)

    val futurePostRsp: Future[CreateUserPostRsp] = (server00 ? CreateUserPostReq(user00.id, post00)).mapTo[CreateUserPostRsp]
    val createUserPostRsp = Await.result(futurePostRsp, someTimeout.duration)
    println(createUserPostRsp.postId)

    val futurePostRsp01: Future[CreateUserPostRsp] = (server00 ? CreateUserPostReq(user00.id, post01)).mapTo[CreateUserPostRsp]
    val createUserPostRsp01 = Await.result(futurePostRsp01, someTimeout.duration)
    println(createUserPostRsp01.postId)

    val futurePostRsp02: Future[CreateUserPostRsp] = (server00 ? CreateUserPostReq(user00.id, post02)).mapTo[CreateUserPostRsp]
    val createUserPostRsp02 = Await.result(futurePostRsp02, someTimeout.duration)
    println(createUserPostRsp02.postId)

    val futureGetPostRsp: Future[GetUserFeedRsp] = (server00 ? GetUserFeedReq(user01.id, "tagged", "", 0)).mapTo[GetUserFeedRsp]
    val getUserPostRsp = Await.result(futureGetPostRsp, someTimeout.duration)
    getUserPostRsp.posts.foreach(post => {
      println(post)
    })

    val photo00 = new PhotoNode(id="",album="all", created_time=now, from="", height=0, name="photo00 name", width=1, caption="tagging @User01")
    val photo01 = new PhotoNode(id="",album="all", created_time=now, from="", height=0, name="photo01 name", width=1, caption="")
    val photo02 = new PhotoNode(id="",album="everything", created_time=now, from="", height=0, name="photo02 name", width=1, caption="tagging @User01")

    {
      println ("adding one album named all")
      val album00 = new AlbumNode(id="", count=0, cover_photo="", created_time=now, description="", from="", name="all", album_type="", updated_time=now)

      val futureAlbumRsp00: Future[CreateUserAlbumRsp] = (server00 ? CreateUserAlbumReq(user00.id, album00)).mapTo[CreateUserAlbumRsp]
      val createUserAlbumRsp00 = Await.result(futureAlbumRsp00, someTimeout.duration)
      println(createUserAlbumRsp00.albumId)
    }

    {
      println ("adding one album named everything")
      val album00 = new AlbumNode(id="", count=0, cover_photo="", created_time=now, description="", from="", name="everything", album_type="", updated_time=now)

      val futureAlbumRsp00: Future[CreateUserAlbumRsp] = (server00 ? CreateUserAlbumReq(user00.id, album00)).mapTo[CreateUserAlbumRsp]
      val createUserAlbumRsp00 = Await.result(futureAlbumRsp00, someTimeout.duration)
      println(createUserAlbumRsp00.albumId)
    }

    println ("getting all albums for this user")
    val futureGetAlbumsRsp00: Future[GetUserAlbumsRsp] = (server00 ? GetUserAlbumsReq(user00.id, "", 0)).mapTo[GetUserAlbumsRsp]
    val getUserAlbumsRsp00 = Await.result(futureGetAlbumsRsp00, someTimeout.duration)
    getUserAlbumsRsp00.albums.foreach(album => {
      println(album)
    })

    println ("adding three photos")
    val futurePhotoRsp: Future[CreateUserPhotoRsp] = (server00 ? CreateUserPhotoReq(user00.id, photo00)).mapTo[CreateUserPhotoRsp]
    val createUserPhotoRsp = Await.result(futurePhotoRsp, someTimeout.duration)
    println(createUserPhotoRsp.photoId)

    val futurePhotoRsp01: Future[CreateUserPhotoRsp] = (server00 ? CreateUserPhotoReq(user00.id, photo01)).mapTo[CreateUserPhotoRsp]
    val createUserPhotoRsp01 = Await.result(futurePhotoRsp01, someTimeout.duration)
    println(createUserPhotoRsp01.photoId)

    val futurePhotoRsp02: Future[CreateUserPhotoRsp] = (server00 ? CreateUserPhotoReq(user00.id, photo02)).mapTo[CreateUserPhotoRsp]
    val createUserPhotoRsp02 = Await.result(futurePhotoRsp02, someTimeout.duration)
    println(createUserPhotoRsp02.photoId)

    val futureGetPhotosRsp: Future[GetUserPhotosRsp] = (server00 ? GetUserPhotosReq(user01.id, "tagged", "", 0)).mapTo[GetUserPhotosRsp]
    val getUserPhotosRsp = Await.result(futureGetPhotosRsp, someTimeout.duration)
    getUserPhotosRsp.photos.foreach(photo => {
      println(photo)
    })

    println ("getting all albums for this user")
    val futureGetAlbumsRsp: Future[GetUserAlbumsRsp] = (server00 ? GetUserAlbumsReq(user00.id, "", 0)).mapTo[GetUserAlbumsRsp]
    val getUserAlbumsRsp = Await.result(futureGetAlbumsRsp, someTimeout.duration)
    getUserAlbumsRsp.albums.foreach(album => {
      println(album)
    })

  }
}