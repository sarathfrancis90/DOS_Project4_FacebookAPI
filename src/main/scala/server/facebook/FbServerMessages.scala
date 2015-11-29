package server.facebook

import scala.collection.mutable.ListBuffer

case class CreateFbNodeReq(nodeType: String, node: Node)

case class CreateFbNodeRsp(result: Boolean, id: String)

case class GetFbNodeReq(nodeType: String, nodeId: String)

case class GetFbNodeRsp(node: Node)

case object PleaseKillYourself

case class CreateUserPostReqToFbWorker(post: PostNode, ownPosts: ListBuffer[String])

case class CreateUserPostRspToFbServer(postId: String)

case class GetUserPostsReqToFbWorker(startFrom: String, limit: Int, postIds: ListBuffer[String])

case class GetUserPostsRspToFbServer(posts: List[PostNode])

case class CreateUserPhotoReqToFbWorker(photo: PhotoNode, ownPhotos: ListBuffer[String])

case class CreateUserPhotoRspToFbServer(photoId: String)

case class GetUserPhotosReqToFbWorker(startFrom: String, limit: Int, photoIds: ListBuffer[String])

case class GetUserPhotosRspToFbServer(photos: List[PhotoNode])

case class GetUserAlbumsReqToFbWorker(startFrom: String, limit: Int, albumIds: ListBuffer[String])

case class GetUserAlbumsRspToFbServer(albums: List[AlbumNode])

case class CreateUserAlbumReqToFbWorker(album: AlbumNode, ownAlbums: ListBuffer[String])

case class CreateUserAlbumRspToFbServer(albumId: String)

case class GetAlbumPhotosReqToFbWorker(startFrom: String, limit: Int, photoIds: ListBuffer[String])

case class GetAlbumPhotosRspToFbServer(photos: List[PhotoNode])

case class AddUserLikedPageReqToFbWorker(userId: String, pageName: String, ownLikedPages: ListBuffer[String])

case class AddUserLikedPageRspToFbServer(result: Boolean)

case class GetUserLikedPagesReqToFbWorker(startFrom: String, limit: Int, pageIds: ListBuffer[String])

case class GetUserLikedPagesRspToFbServer(pages: List[PageNode])

case class GetPageLikedUsersReqToFbWorker(startFrom: String, limit: Int, userIds: ListBuffer[String])

case class GetPageLikedUsersRspToFbServer(users: List[UserNode])

case class CreatePagePostReqToFbWorker(post: PostNode, ownPosts: ListBuffer[String], likedUsers: ListBuffer[String])

case class CreatePagePostRspToFbServer(postId: String)

case class GetUserTimelineReqToFbWorker(startFrom: String, limit: Int, timeline: ListBuffer[(String, String)])

case class GetUserTimelineRspToFbServer(events: List[(String, Node)])

case class CreatePagePhotoReqToFbWorker(photo: PhotoNode, ownPhotos: ListBuffer[String], likedUsers: ListBuffer[String])

case class CreatePagePhotoRspToFbServer(photoId: String)

case class RemoveUserLikedPageReqToFbWorker(userId: String, pageId: String, ownLikedPages: ListBuffer[String])

case class RemoveUserLikedPageRspToFbServer(result: Boolean)

case class UpdateUserTaggedPostNtf(userId: String, postId: String) /* TODO: simulate */

case class UpdateUserTaggedPhotoNtf(userId: String, photoId: String) /* TODO: simulate */

case class UpdateAlbumPhotoNtf(albumId: String, photoId: String) /* TODO: simulate */

case class UpdateUserAlbumNtf(userId: String, albumId: String) /* TODO: simulate */

case class UpdatePageLikedUserReq(action: String, pageId: String, userId: String) /* TODO: simulate */

case class UpdatePageLikedUserRsp(result: Boolean) /* TODO: simulate */

case class UpdateUserTimelineNtf(userId: String, eventType: String, eventId: String) /* TODO: simulate */

case class GetUserAlbumsReq(userId: String, startFrom: String, limit: Int) /* TODO: simulate */

case class GetUserAlbumsRsp(albums: List[AlbumNode]) /* TODO: simulate */

case class CreateUserAlbumReq(userId: String, album: AlbumNode) /* TODO: simulate */

case class CreateUserAlbumRsp(albumId: String) /* TODO: simulate */

case class GetUserPhotosReq(userId: String, typeOfPhotos: String, startFrom: String, limit: Int) /* TODO: simulate */

case class GetUserPhotosRsp(photos: List[PhotoNode]) /* TODO: simulate */

case class CreateUserPhotoReq(userId: String, photo: PhotoNode)

case class CreateUserPhotoRsp(photoId: String)

case class GetUserFriendsReq(userId: String, startFrom: String, limit: Int) /* TODO: simulate */

case class GetUserFriendsRsp(users: List[UserNode]) /* TODO: simulate */

case class IsUserFriendsWithReq(userId: String, anotherUserId: String) /* TODO: simulate */

case class IsUserFriendsWithRsp(user: UserNode) /* TODO: simulate */

case class GetUserFeedReq(userId: String, typeOfPosts: String, startFrom: String, limit: Int)  /* TODO: simulate */

case class GetUserFeedRsp(posts: List[PostNode]) /* TODO: simulate */

case class CreateUserPostReq(userId: String, post: PostNode)

case class CreateUserPostRsp(postId: String)

case class GetAlbumPhotosReq(userId: String, albumId: String, startFrom: String, limit: Int)  /* TODO: simulate */

case class GetAlbumPhotosRsp(photos: List[PhotoNode]) /* TODO: simulate */

case class AddUserLikedPageReq(userId: String, pageName: String)

case class AddUserLikedPageRsp(result: Boolean)

case class GetUserLikedPagesReq(userId: String, startFrom: String, limit: Int) /* TODO: simulate */

case class GetUserLikedPagesRsp(pages: List[PageNode])  /* TODO: simulate */

case class GetPageLikedUsersReq(pageId: String, startFrom: String, limit: Int) /* TODO: simulate */

case class GetPageLikedUsersRsp(users: List[UserNode]) /* TODO: simulate */

case class CreatePagePostReq(pageId: String, post: PostNode)

case class CreatePagePostRsp(postId: String)

case class GetUserTimelineReq(userId: String, startFrom: String, limit: Int)

case class GetUserTimelineRsp(events: List[(String, Node)])

case class CreatePagePhotoReq(pageId: String, photo: PhotoNode)

case class CreatePagePhotoRsp(photoId: String)

case class RemoveUserLikedPageReq(userId: String, pageId: String) /* TODO: simulate */

case class RemoveUserLikedPageRsp(result: Boolean) /* TODO: simulate */