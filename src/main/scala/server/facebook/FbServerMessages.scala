package server.facebook

import scala.collection.mutable.ListBuffer

case class CreateFbNodeReq(nodeType: String, node: Node)
case class CreateFbNodeRsp(result: Boolean, id: String)

case class GetFbNodeReq(nodeType: String, nodeId: String)
case class GetFbNodeRsp(node: Node)

// messages to workers
case object PleaseKillYourself

// first degree requests
case class CreateUserPostReqToFbWorker(post: PostNode, ownPosts: ListBuffer[String])
case class CreateUserPostRspToFbServer(postId: String)

case class GetUserPostsReqToFbWorker(startFrom: String, limit: Integer, posts: ListBuffer[String])
case class GetUserPostsRspToFbServer(posts: List[PostNode])

// second degree requests
case class UpdateUserTaggedPostNtf(userId: String, postId: String)


// Node - User, Edge - albums
case class GetUserAlbumsReq(userId: String, startFrom: String, limit: Integer)
case class GetUserAlbumsRsp(albums: List[AlbumNode])

case class CreateUserAlbumReq(userId: String, album: AlbumNode)
case class CreateUserAlbumRsp(albumId: String)

// Node - User, Edge - likes
case class GetUserLikesReq(userId: String, startFrom: String, limit: Integer)
case class GetUserLikesRsp(pages: List[PageNode])

// Node - User, Edge - photos
case class GetUserPhotosReq(userId: String, typeOfPhotos: String, startFrom: String, limit: Integer)
case class GetUserPhotosRsp(photos: List[PhotoNode])

case class CreateUserPhotoReq(userId: String, photo: PhotoNode)
case class CreateUserPhotoRsp(photoId: String)

// Node - User, Edge - friends
case class GetUserFriendsReq(userId: String, startFrom: String, limit: Integer)
case class GetUserFriendsRsp(users: List[UserNode])

case class IsUserFriendsWithReq(userId: String, anotherUserId: String)
case class IsUserFriendsWithRsp(user: UserNode)

// Node - User, Edge - feed, posts, tagged
case class GetUserFeedReq(userId: String, typeOfPosts: String, startFrom: String, limit: Integer)
case class GetUserFeedRsp(posts: List[PostNode])

case class CreateUserPostReq(userId: String, post: PostNode)
case class CreateUserPostRsp(postId: String)

//
case class GetPageAlbumsReq()
case class GetPageAlbumsRsp()

case class CreatePageAlbumReq()
case class CreatePageAlbumRsp()

//
case class GetPagePhotosReq()
case class GetPagePhotosRsp()

case class CreatePagePhotoReq()
case class CreatePagePhotoRsp()

//
case class GetPageFeedReq()
case class GetPageFeedRsp()

case class CreatePagePostReq()
case class CreatePagePostRsp()


