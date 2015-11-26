package server.facebook

import scala.collection.mutable.ListBuffer

case class CreateFbNodeReq(nodeType: String, node: Node)

case class CreateFbNodeRsp(result: Boolean, id: String)

case class GetFbNodeReq(nodeType: String, nodeId: String)

case class GetFbNodeRsp(node: Node)

case object PleaseKillYourself

case class CreateUserPostReqToFbWorker(post: PostNode, ownPosts: ListBuffer[String])

case class CreateUserPostRspToFbServer(postId: String)

case class GetUserPostsReqToFbWorker(startFrom: String, limit: Integer, postIds: ListBuffer[String])

case class GetUserPostsRspToFbServer(posts: List[PostNode])

case class CreateUserPhotoReqToFbWorker(photo: PhotoNode, ownPhotos: ListBuffer[String])

case class CreateUserPhotoRspToFbServer(photoId: String)

case class GetUserPhotosReqToFbWorker(startFrom: String, limit: Integer, photoIds: ListBuffer[String])

case class GetUserPhotosRspToFbServer(photos: List[PhotoNode])

case class GetUserAlbumsReqToFbWorker(startFrom: String, limit: Integer, albumIds: ListBuffer[String])

case class GetUserAlbumsRspToFbServer(albums: List[AlbumNode])

case class CreateUserAlbumReqToFbWorker(album: AlbumNode, ownAlbums: ListBuffer[String])

case class CreateUserAlbumRspToFbServer(albumId: String)

case class GetAlbumPhotosReqToFbWorker(startFrom: String, limit: Integer, photoIds: ListBuffer[String])

case class GetAlbumPhotosRspToFbServer(photos: List[PhotoNode])

case class AddUserLikedPageReqToFbWorker(userId: String, pageName: String, ownLikedPages: ListBuffer[String])

case class AddUserLikedPageRspToFbServer(result: Boolean)

case class UpdateUserTaggedPostNtf(userId: String, postId: String)

case class UpdateUserTaggedPhotoNtf(userId: String, photoId: String)

case class UpdateAlbumPhotoNtf(albumId: String, photoId: String)

case class UpdateUserAlbumNtf(userId: String, albumId: String)

case class UpdatePageLikedUserReq(pageId: String, userId: String)

case class UpdatePageLikedUserRsp(result: Boolean)

case class GetUserAlbumsReq(userId: String, startFrom: String, limit: Integer)

case class GetUserAlbumsRsp(albums: List[AlbumNode])

case class CreateUserAlbumReq(userId: String, album: AlbumNode)

case class CreateUserAlbumRsp(albumId: String)

case class GetUserLikesReq(userId: String, startFrom: String, limit: Integer)

case class GetUserLikesRsp(pages: List[PageNode])

case class GetUserPhotosReq(userId: String, typeOfPhotos: String, startFrom: String, limit: Integer)

case class GetUserPhotosRsp(photos: List[PhotoNode])

case class CreateUserPhotoReq(userId: String, photo: PhotoNode)

case class CreateUserPhotoRsp(photoId: String)

case class GetUserFriendsReq(userId: String, startFrom: String, limit: Integer)

case class GetUserFriendsRsp(users: List[UserNode])

case class IsUserFriendsWithReq(userId: String, anotherUserId: String)

case class IsUserFriendsWithRsp(user: UserNode)

case class GetUserFeedReq(userId: String, typeOfPosts: String, startFrom: String, limit: Integer)

case class GetUserFeedRsp(posts: List[PostNode])

case class CreateUserPostReq(userId: String, post: PostNode)

case class CreateUserPostRsp(postId: String)

case class GetAlbumPhotosReq(userId: String, albumId: String, startFrom: String, limit: Integer)

case class GetAlbumPhotosRsp(photos: List[PhotoNode])

case class AddUserLikedPageReq(userId: String, pageName: String)

case class AddUserLikedPageRsp(result: Boolean)

case class GetPageAlbumsReq()

case class GetPageAlbumsRsp()

case class CreatePageAlbumReq()

case class CreatePageAlbumRsp()

case class GetPagePhotosReq()

case class GetPagePhotosRsp()

case class CreatePagePhotoReq()

case class CreatePagePhotoRsp()

case class GetPageFeedReq()

case class GetPageFeedRsp()

case class CreatePagePostReq()

case class CreatePagePostRsp()
