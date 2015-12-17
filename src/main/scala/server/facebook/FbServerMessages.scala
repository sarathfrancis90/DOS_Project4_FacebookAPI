package server.facebook

import scala.collection.mutable.ListBuffer

case class CreateFbNodeReq(nodeType: String, node: Node)

case class CreateFbNodeRsp(result: Boolean, id: String)

case class GetFbNodeReq(nodeType: String, nodeId: String)

case class GetFbNodeRsp(node: Node)

case object PleaseKillYourself

case class CreateUserPostReqToFbWorker(post: PostNode, ownPosts: ListBuffer[String], ownFriends: ListBuffer[String])

case class CreateUserPostRspToFbServer(postId: String)

case class GetUserPostsReqToFbWorker(startFrom: String, limit: Int, postIds: ListBuffer[String], userId: String)

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

case class UpdateUserTaggedPostNtf(userId: String, postId: String)

case class UpdateUserTaggedPhotoNtf(userId: String, photoId: String)

case class UpdateAlbumPhotoNtf(albumId: String, photoId: String)

case class UpdateUserAlbumNtf(userId: String, albumId: String)

case class UpdatePageLikedUserReq(action: String, pageId: String, userId: String)

case class UpdatePageLikedUserRsp(result: Boolean)

case class UpdateUserTimelineNtf(userId: String, eventType: String, eventId: String)

case class GetUserAlbumsReq(userId: String, startFrom: String, limit: Int)

case class GetUserAlbumsRsp(albums: List[AlbumNode])

case class CreateUserAlbumReq(userId: String, album: AlbumNode)

case class CreateUserAlbumRsp(albumId: String)

case class GetUserPhotosReq(userId: String, typeOfPhotos: String, startFrom: String, limit: Int)

case class GetUserPhotosRsp(photos: List[PhotoNode])

case class CreateUserPhotoReq(userId: String, photo: PhotoNode)

case class CreateUserPhotoRsp(photoId: String)

case class GetUserFriendsReq(userId: String, startFrom: String, limit: Int)

case class GetUserFriendsRsp(users: List[UserNode])

case class IsUserFriendsWithReq(userId: String, anotherUserId: String)

case class IsUserFriendsWithRsp(user: UserNode)

case class GetUserFeedReq(userId: String, typeOfPosts: String, startFrom: String, limit: Int)

case class GetUserFeedRsp(posts: List[PostNode])

case class CreateUserPostReq(userId: String, post: PostNode)

case class CreateUserPostRsp(postId: String)

case class GetAlbumPhotosReq(userId: String, albumId: String, startFrom: String, limit: Int)

case class GetAlbumPhotosRsp(photos: List[PhotoNode])

case class AddUserLikedPageReq(userId: String, pageName: String)

case class AddUserLikedPageRsp(result: Boolean)

case class GetUserLikedPagesReq(userId: String, startFrom: String, limit: Int)

case class GetUserLikedPagesRsp(pages: List[PageNode])

case class GetPageLikedUsersReq(pageId: String, startFrom: String, limit: Int)

case class GetPageLikedUsersRsp(users: List[UserNode])

case class CreatePagePostReq(pageId: String, post: PostNode)

case class CreatePagePostRsp(postId: String)

case class GetUserTimelineReq(userId: String, startFrom: String, limit: Int)

case class GetUserTimelineRsp(events: List[(String, Node)])

case class CreatePagePhotoReq(pageId: String, photo: PhotoNode)

case class CreatePagePhotoRsp(photoId: String)

case class RemoveUserLikedPageReq(userId: String, pageId: String)

case class RemoveUserLikedPageRsp(result: Boolean)

case class UpdateFriendNtf(userId: String, friendId: String)

case class AddFriendReq(userId: String, friendName: String)

case class AddFriendReqToFbWorker(userId: String, friendName: String, ownFriends: ListBuffer[String], ownInFriends: ListBuffer[String], ownOutFriends: ListBuffer[String])

case class AddFriendRspToFbServer(result: String)

case class AddFriendRsp(result: String)

case class GetPendingInFriendsReq(userId: String)

case class GetPendingInFriendsReqToFbWorker(ownInFriends: ListBuffer[String])

case class GetPendingInFriendsRspToFbServer(inFriendNames: List[String])

case class GetPendingInFriendsRsp(inFriendNames: List[String])

case class GetPendingOutFriendsReq(userId: String)

case class GetPendingOutFriendsReqToFbWorker(outFriends: ListBuffer[String])

case class GetPendingOutFriendsRspToFbServer(outFriendNames: List[String])

case class GetPendingOutFriendsRsp(outFriendNames: List[String])

case class GetFriendsReq(userId: String)

case class GetFriendsReqToFbWorker(ownFriends: ListBuffer[String], friendsSpecialKeys: ListBuffer[(String, String)])

case class GetFriendsRspToFbServer(friends: List[UserNode])

case class GetFriendsRsp(friends: List[UserNode])

case class CreateUserReq(user: UserNode)

case class CreateUserReqToFbWorker(user: UserNode)

case class CreateUserRspToFbServer(result: Boolean, id: String)

case class CreateUserRsp(result: Boolean, id: String)

case class GetFriendDetailsReq(userId: String, friendName: String)

case class GetFriendDetailsReqToFbWorker(ownFriends: ListBuffer[String], friendName: String, friendsSpecialKeys: ListBuffer[(String, String)])

case class GetFriendDetailsRspToFbServer(friendNode: Option[UserNode])

case class GetFriendDetailsRsp(friendNode: UserNode)

case class Update_InFriends_Remove(userId: String, friendId: String)

case class Update_OutFriends_Remove(userId: String, friendId: String)

case class Update_OutFriends_Insert(userId: String, friendId: String)

case class Update_Friends_Insert(userId: String, friendId: String)

case class AddSpecialKeyToFriendReq(userId: String, friendName: String, encrypted_special_key: String)

case class AddSpecialKeyToFriendRsp(result: Boolean)