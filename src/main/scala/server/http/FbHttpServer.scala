package server.http

import _root_.server.facebook._
import akka.actor._
import akka.io.IO
import akka.pattern.ask
import akka.util.Timeout
import spray.can.Http
import spray.http.HttpMethods._
import spray.http._
import spray.httpx.SprayJsonSupport
import spray.json._

import scala.collection.mutable.ListBuffer
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.language.postfixOps

object FbJsonProtocol extends DefaultJsonProtocol {
  implicit val userNodeFormat = jsonFormat6(UserNode)
  implicit val pageNodeFormat = jsonFormat5(PageNode)
  implicit val postNodeFormat = jsonFormat7(PostNode)
  implicit val friendListNodeFormat = jsonFormat3(FriendListNode)
  implicit val photoNodeFormat = jsonFormat8(PhotoNode)
  implicit val albumNodeFormat = jsonFormat9(AlbumNode)
  implicit val createFbNodeRspFormat = jsonFormat2(CreateFbNodeRsp)
  implicit val addUserLikedPageReqFormat = jsonFormat2(AddUserLikedPageReq)
  implicit val addUserLikedPageRspFormat = jsonFormat1(AddUserLikedPageRsp)
  implicit val createPagePostReqFormat = jsonFormat2(CreatePagePostReq)
  implicit val createPagePostRspFormat = jsonFormat1(CreatePagePostRsp)
  implicit val createPagePhotoReqFormat = jsonFormat2(CreatePagePhotoReq)
  implicit val createPagePhotoRspFormat = jsonFormat1(CreatePagePhotoRsp)
  implicit val createUserPostReqFormat = jsonFormat2(CreateUserPostReq)
  implicit val createUserPostRspFormat = jsonFormat1(CreateUserPostRsp)
  implicit val createUserPhotoReqFormat = jsonFormat2(CreateUserPhotoReq)
  implicit val createUserPhotoRspFormat = jsonFormat1(CreateUserPhotoRsp)
  implicit val createUserAlbumReqFormat = jsonFormat2(CreateUserAlbumReq)
  implicit val createUserAlbumRspFormat = jsonFormat1(CreateUserAlbumRsp)
  implicit val removeUserLikedPageReqFormat = jsonFormat2(RemoveUserLikedPageReq)
  implicit val removeUserLikedPageRspFormat = jsonFormat1(RemoveUserLikedPageRsp)
  implicit val addFriendReqFormat = jsonFormat2(AddFriendReq)
  implicit val addFriendRspFormat = jsonFormat1(AddFriendRsp)
  implicit val getPendingInFriendsRsp = jsonFormat1(GetPendingInFriendsRsp)
  implicit val createUserRspFormat = jsonFormat2(CreateUserRsp)

  implicit object NodeFormat extends RootJsonFormat[Node] {
    def write(n: Node) = n match {
      case post: PostNode => post.toJson
      case photo: PhotoNode => photo.toJson
      case postV2: PostNodeV2 => postV2.toJson
      case user: UserNode => user.toJson


    }

    def read(value: JsValue) = value match {
      case _ =>
        throw new DeserializationException("Not supported")
    }
  }
  implicit val encryptedPrivateKeyFormat = jsonFormat2(EncryptedPrivateKey)
  implicit val postNodeV2Format = jsonFormat9(PostNodeV2)
}

class FbServerHttp extends Actor with ActorLogging with AdditionalFormats with SprayJsonSupport {

  import FbJsonProtocol._
  import context.dispatcher

  implicit val someTimeout = Timeout(5 seconds)

  val system = ActorSystem("HttpServerForFb")
  val fbServer = system.actorOf(Props(new FbServer), "FbServer00")
  fbServer ! "Init"

  def receive = {
    case _: Http.Connected =>
      sender ! Http.Register(self)

    case HttpRequest(GET, Uri.Path("/ping"), _, _, _) =>
      sender ! HttpResponse(entity = "PONG!")

    case HttpRequest(POST, Uri.Path("/user/create"), _, entity, _) =>
      val requestor = sender
      val userNode = entity.asString.parseJson.convertTo[UserNode]
      val future: Future[CreateUserRsp] = (fbServer ? CreateUserReq(userNode)).mapTo[CreateUserRsp]
      future.onSuccess {
        case result: CreateUserRsp =>
          requestor ! HttpResponse(entity = HttpEntity(ContentTypes.`application/json`, result.toJson.toString))
      }

    case HttpRequest(POST, Uri.Path("/page/create"), _, entity, _) =>
      val requestor = sender
      val pageNode = entity.asString.parseJson.convertTo[PageNode]
      val future: Future[CreateFbNodeRsp] = (fbServer ? CreateFbNodeReq("page", pageNode)).mapTo[CreateFbNodeRsp]
      future.onSuccess {
        case result: CreateFbNodeRsp =>
          requestor ! HttpResponse(entity = HttpEntity(ContentTypes.`application/json`, result.toJson.toString))
      }

    case HttpRequest(POST, Uri.Path("/like_this_page"), _, entity, _) =>
      val requestor = sender
      val addUserLikedPageReq = entity.asString.parseJson.convertTo[AddUserLikedPageReq]
      val future: Future[AddUserLikedPageRsp] = (fbServer ? addUserLikedPageReq).mapTo[AddUserLikedPageRsp]
      future.onSuccess {
        case result: AddUserLikedPageRsp =>
          requestor ! HttpResponse(entity = HttpEntity(ContentTypes.`application/json`, result.toJson.toString()))
      }

    case HttpRequest(POST, Uri.Path("/page/post"), _, entity, _) =>
      val requestor = sender
      val createPagePostReq = entity.asString.parseJson.convertTo[CreatePagePostReq]
      val future: Future[CreatePagePostRsp] = (fbServer ? createPagePostReq).mapTo[CreatePagePostRsp]
      future.onSuccess {
        case result: CreatePagePostRsp =>
          requestor ! HttpResponse(entity = HttpEntity(ContentTypes.`application/json`, result.toJson.toString))
      }
    case HttpRequest(POST, Uri.Path("/page/photo"), _, entity, _) =>
      val requestor = sender
      val createPagePhotoReq = entity.asString.parseJson.convertTo[CreatePagePhotoReq]
      val future: Future[CreatePagePhotoRsp] = (fbServer ? createPagePhotoReq).mapTo[CreatePagePhotoRsp]
      future.onSuccess {
        case result: CreatePagePhotoRsp =>
          requestor ! HttpResponse(entity = HttpEntity(ContentTypes.`application/json`, result.toJson.toString))
      }

    case HttpRequest(POST, Uri.Path("/user/post"), _, entity, _) =>
      val requestor = sender
      val createUserPostReq = entity.asString.parseJson.convertTo[CreateUserPostReq]
      val future: Future[CreateUserPostRsp] = (fbServer ? createUserPostReq).mapTo[CreateUserPostRsp]
      future.onSuccess {
        case result: CreateUserPostRsp =>
          requestor ! HttpResponse(entity = HttpEntity(ContentTypes.`application/json`, result.toJson.toString))
      }

    case HttpRequest(POST, Uri.Path("/user/photo"), _, entity, _) =>
      val requestor = sender
      val createUserPhotoReq = entity.asString.parseJson.convertTo[CreateUserPhotoReq]
      val future: Future[CreateUserPhotoRsp] = (fbServer ? createUserPhotoReq).mapTo[CreateUserPhotoRsp]
      future.onSuccess {
        case result: CreateUserPhotoRsp =>
          requestor ! HttpResponse(entity = HttpEntity(ContentTypes.`application/json`, result.toJson.toString))
      }

    case HttpRequest(POST, Uri.Path("/user/album"), _, entity, _) =>
      val requestor = sender
      val createUserAlbumReq = entity.asString.parseJson.convertTo[CreateUserAlbumReq]
      val future: Future[CreateUserAlbumRsp] = (fbServer ? createUserAlbumReq).mapTo[CreateUserAlbumRsp]
      future.onSuccess {
        case result: CreateUserAlbumRsp =>
          requestor ! HttpResponse(entity = HttpEntity(ContentTypes.`application/json`, result.toJson.toString))
      }

    case HttpRequest(POST, Uri.Path("/user/unlike_page"), _, entity, _) =>
      val requestor = sender
      val removeUserLikedPageReq = entity.asString.parseJson.convertTo[RemoveUserLikedPageReq]
      val future: Future[RemoveUserLikedPageRsp] = (fbServer ? removeUserLikedPageReq).mapTo[RemoveUserLikedPageRsp]
      future.onSuccess {
        case result: RemoveUserLikedPageRsp =>
          requestor ! HttpResponse(entity = HttpEntity(ContentTypes.`application/json`, result.toJson.toString))
      }

    case HttpRequest(POST, Uri.Path("/user/add_friend_request"), _, entity, _) =>
      val requestor = sender
      val addFriendReq = entity.asString.parseJson.convertTo[AddFriendReq]
      val future: Future[AddFriendRsp] = (fbServer ? addFriendReq).mapTo[AddFriendRsp]
      future.onSuccess {
        case result: AddFriendRsp =>
          requestor ! HttpResponse(entity = HttpEntity(ContentTypes.`application/json`, result.toJson.toString))
      }

    case HttpRequest(GET, Uri.Path(path), _, _, _) if path startsWith "/user/timeline" =>
      val requestor = sender
      val userId = path.split('/').last
      val getUserTimelineReq = GetUserTimelineReq(
        userId = userId,
        startFrom = "",
        limit = 0)
      val future: Future[GetUserTimelineRsp] = (fbServer ? getUserTimelineReq).mapTo[GetUserTimelineRsp]
      future.onSuccess {
        case result: GetUserTimelineRsp =>
          val eventsOnly: ListBuffer[Node] = new ListBuffer[Node]()
          result.events.foreach(event => {
            eventsOnly += event._2
          })
          requestor ! HttpResponse(entity = HttpEntity(ContentTypes.`application/json`, eventsOnly.toList.take(10).toJson.toString))
      }

    case HttpRequest(GET, Uri.Path(path), _, _, _) if path startsWith "/user/own_photos" =>
      val requestor = sender
      val userId = path.split('/').last
      val getUserPhotosReq = GetUserPhotosReq(
        userId = userId,
        typeOfPhotos = "own",
        startFrom = "",
        limit = 0)
      val future: Future[GetUserPhotosRsp] = (fbServer ? getUserPhotosReq).mapTo[GetUserPhotosRsp]
      future.onSuccess {
        case result: GetUserPhotosRsp =>
          val photos: ListBuffer[Node] = new ListBuffer[Node]()
          result.photos.foreach(photo => {
            photos += photo
          })
          requestor ! HttpResponse(entity = HttpEntity(ContentTypes.`application/json`, photos.toList.take(10).toJson.toString))
      }

    case HttpRequest(GET, Uri.Path(path), _, _, _) if path startsWith "/user/tagged_photos" =>
      val requestor = sender
      val userId = path.split('/').last
      val getUserPhotosReq = GetUserPhotosReq(
        userId = userId,
        typeOfPhotos = "tagged",
        startFrom = "",
        limit = 0)
      val future: Future[GetUserPhotosRsp] = (fbServer ? getUserPhotosReq).mapTo[GetUserPhotosRsp]
      future.onSuccess {
        case result: GetUserPhotosRsp =>
          val photos: ListBuffer[Node] = new ListBuffer[Node]()
          result.photos.foreach(photo => {
            photos += photo
          })
          requestor ! HttpResponse(entity = HttpEntity(ContentTypes.`application/json`, photos.toList.take(10).toJson.toString))
      }

    case HttpRequest(GET, Uri.Path(path), _, _, _) if path startsWith "/user/own_posts" =>
      val requestor = sender
      val userId = path.split('/').last
      val getUserFeedReq = GetUserFeedReq(
        userId = userId,
        typeOfPosts = "own",
        startFrom = "",
        limit = 0)
      val future: Future[GetUserFeedRsp] = (fbServer ? getUserFeedReq).mapTo[GetUserFeedRsp]
      future.onSuccess {
        case result: GetUserFeedRsp =>
          val posts: ListBuffer[Node] = new ListBuffer[Node]()
          result.posts.foreach(post => {
            posts += post
          })
          requestor ! HttpResponse(entity = HttpEntity(ContentTypes.`application/json`, posts.toList.take(10).toJson.toString))
      }

    case HttpRequest(GET, Uri.Path(path), _, _, _) if path startsWith "/user/tagged_posts" =>
      val requestor = sender
      val userId = path.split('/').last
      val getUserFeedReq = GetUserFeedReq(
        userId = userId,
        typeOfPosts = "tagged",
        startFrom = "",
        limit = 0)
      val future: Future[GetUserFeedRsp] = (fbServer ? getUserFeedReq).mapTo[GetUserFeedRsp]
      future.onSuccess {
        case result: GetUserFeedRsp =>
          val posts: ListBuffer[Node] = new ListBuffer[Node]()
          result.posts.foreach(post => {
            posts += post
          })
          requestor ! HttpResponse(entity = HttpEntity(ContentTypes.`application/json`, posts.toList.take(10).toJson.toString))
      }

    case HttpRequest(GET, Uri.Path(path), _, _, _) if path startsWith "/user/get_albums" =>
      val requestor = sender
      val userId = path.split('/').last
      val getUserAlbumsReq = GetUserAlbumsReq(
        userId = userId,
        startFrom = "",
        limit = 0)
      val future: Future[GetUserAlbumsRsp] = (fbServer ? getUserAlbumsReq).mapTo[GetUserAlbumsRsp]
      future.onSuccess {
        case result: GetUserAlbumsRsp =>
          val albums: ListBuffer[Node] = new ListBuffer[Node]()
          result.albums.foreach(album => {
            albums += album
          })
          requestor ! HttpResponse(entity = HttpEntity(ContentTypes.`application/json`, albums.toList.take(10).toJson.toString))
      }

    case HttpRequest(GET, Uri.Path(path), _, _, _) if path startsWith "/user/album_photos" =>
      val requestor = sender
      val urlSplit = path.split('/')
      val arrayLength = urlSplit.length
      val userId = urlSplit(arrayLength - 2)
      val albumId = urlSplit(arrayLength - 1)

      val getalbumPhotosReq = GetAlbumPhotosReq(
        userId = userId,
        albumId = albumId,
        startFrom = "",
        limit = 0)
      val future: Future[GetAlbumPhotosRsp] = (fbServer ? getalbumPhotosReq).mapTo[GetAlbumPhotosRsp]
      future.onSuccess {
        case result: GetAlbumPhotosRsp =>
          val photos: ListBuffer[Node] = new ListBuffer[Node]()
          result.photos.foreach(photo => {
            photos += photo
          })
          requestor ! HttpResponse(entity = HttpEntity(ContentTypes.`application/json`, photos.toList.take(10).toJson.toString))
      }

    case HttpRequest(GET, Uri.Path(path), _, _, _) if path startsWith "/user/liked_pages" =>
      val requestor = sender
      val userId = path.split('/').last
      val getUserLikedPagesReq = GetUserLikedPagesReq(
        userId = userId,
        startFrom = "",
        limit = 0)
      val future: Future[GetUserLikedPagesRsp] = (fbServer ? getUserLikedPagesReq).mapTo[GetUserLikedPagesRsp]
      future.onSuccess {
        case result: GetUserLikedPagesRsp =>
          val pages: ListBuffer[Node] = new ListBuffer[Node]()
          result.pages.foreach(page => {
            pages += page
          })
          requestor ! HttpResponse(entity = HttpEntity(ContentTypes.`application/json`, pages.toList.take(10).toJson.toString))
      }

    case HttpRequest(GET, Uri.Path(path), _, _, _) if path startsWith "/page/liked_users" =>
      val requestor = sender
      val pageId = path.split('/').last
      val getPageLikedUsersReq = GetPageLikedUsersReq(
        pageId = pageId,
        startFrom = "",
        limit = 0)
      val future: Future[GetPageLikedUsersRsp] = (fbServer ? getPageLikedUsersReq).mapTo[GetPageLikedUsersRsp]
      future.onSuccess {
        case result: GetPageLikedUsersRsp =>
          val users: ListBuffer[Node] = new ListBuffer[Node]()
          result.users.foreach(user => {
            users += user
          })
          requestor ! HttpResponse(entity = HttpEntity(ContentTypes.`application/json`, users.toList.take(10).toJson.toString))
      }

    case HttpRequest(GET, Uri.Path(path), _, _, _) if path startsWith "/user/get_friends" =>
      val requestor = sender
      val userId = path.split('/').last
      val getFriendsReq = GetFriendsReq(
        userId = userId)
      val future: Future[GetFriendsRsp] = (fbServer ? getFriendsReq).mapTo[GetFriendsRsp]
      future.onSuccess {
        case result: GetFriendsRsp =>
          val friends: ListBuffer[Node] = new ListBuffer[Node]()
          result.friends.foreach(friend => {
            friends += friend
          })
          requestor ! HttpResponse(entity = HttpEntity(ContentTypes.`application/json`, friends.toList.toJson.toString))
      }

    case HttpRequest(GET, Uri.Path(path), _, _, _) if path startsWith "/user/pending_in_friend_requests" =>
      val requestor = sender
      val userId = path.split('/').last
      val getPendingInFriendsReq = GetPendingInFriendsReq(
        userId = userId
      )
      val future: Future[GetPendingInFriendsRsp] = (fbServer ? getPendingInFriendsReq).mapTo[GetPendingInFriendsRsp]
      future.onSuccess {
        case result: GetPendingInFriendsRsp =>
          val pendingInFriendRequests: ListBuffer[String] = new ListBuffer[String]()
          result.inFriendNames.foreach(inFriendName => {
            pendingInFriendRequests += inFriendName
          })
          requestor ! HttpResponse(entity = HttpEntity(ContentTypes.`application/json`, pendingInFriendRequests.toList.toJson.toString))
      }

    case HttpRequest(GET, Uri.Path(path), _, _, _) if path startsWith "/user/pending_out_friend_requests" =>
      val requestor = sender
      val userId = path.split('/').last
      val getPendingOutFriendsReq = GetPendingOutFriendsReq(
        userId = userId
      )
      val future: Future[GetPendingOutFriendsRsp] = (fbServer ? getPendingOutFriendsReq).mapTo[GetPendingOutFriendsRsp]
      future.onSuccess {
        case result: GetPendingOutFriendsRsp =>
          val pendingOutFriendRequests: ListBuffer[String] = new ListBuffer[String]()
          result.outFriendNames.foreach(outFriendName => {
            pendingOutFriendRequests += outFriendName
          })
          requestor ! HttpResponse(entity = HttpEntity(ContentTypes.`application/json`, pendingOutFriendRequests.toList.take(10).toJson.toString))
      }
  }
}

object FbHttpServer extends App {
  implicit val system = ActorSystem()

  IO(Http) ! Http.Bind(system.actorOf(Props[FbServerHttp], name = "FbServerHttp"), interface = "localhost", port = 8080)
}