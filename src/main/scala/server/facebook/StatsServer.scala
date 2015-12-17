package server.facebook

import java.util.Calendar

import akka.actor.{Actor, ActorLogging}

import scala.concurrent.duration.Duration

class StatsServer extends Actor with ActorLogging {

  import context._

  var statsCreateFbNodeReqUser = 0
  var statsCreateFbNodeReqPage = 0
  var statsCreateUserPostReq = 0
  var statsCreateUserPostRsp = 0
  var statsGetUserFeedReq = 0
  var statsGetUserFeedRsp = 0
  var statsCreateUserPhotoReq = 0
  var statsCreateUserPhotoRsp = 0
  var statsGetUserPhotosReq = 0
  var statsGetUserPhotosRsp = 0
  var statsGetUserAlbumsReq = 0
  var statsGetUserAlbumsRsp = 0
  var statsCreateUserAlbumReq = 0
  var statsCreateUserAlbumRsp = 0
  var statsGetAlbumPhotosReq = 0
  var statsGetAlbumPhotosRsp = 0
  var statsAddUserLikedPageReq = 0
  var statsAddUserLikedPageRsp = 0
  var statsGetUserLikedPagesReq = 0
  var statsGetUserLikedPagesRsp = 0
  var statsGetPageLikedUsersReq = 0
  var statsGetPageLikedUsersRsp = 0
  var statsCreatePagePostReq = 0
  var statsCreatePagePostRsp = 0
  var statsGetUserTimelineReq = 0
  var statsGetUserTimelineRsp = 0
  var statsCreatePagePhotoReq = 0
  var statsCreatePagePhotoRsp = 0
  var statsRemoveUserLikedPageReq = 0
  var statsRemoveUserLikedPageRsp = 0
  var statsAddFriendReq = 0
  var statsAddFriendRsp = 0
  var statsGetPendingInFriendsReq = 0
  var statsGetPendingInFriendsRsp = 0
  var statsGetFriendsReq = 0
  var statsGetFriendsRsp = 0

  def printStats() = {
    val now = Calendar.getInstance().getTime.toString
    print("\033[H\033[2J")
    println("--- Stats at " + now + " ---")
    println("Create user requests - " + statsCreateFbNodeReqUser.toString + " in the last 5 seconds (" + statsCreateFbNodeReqUser.toDouble / 5 + "/second)")
    println("Create page requests - " + statsCreateFbNodeReqPage.toString + " in the last 5 seconds (" + statsCreateFbNodeReqPage.toDouble / 5 + "/second)")
    println("Create user post requests - " + statsCreateUserPostReq.toString + " in the last 5 seconds (" + statsCreateUserPostReq.toDouble / 5 + "/second)")
    println("Get user posts requests - " + statsGetUserFeedReq.toString + " in the last 5 seconds (" + statsGetUserFeedReq.toDouble / 5 + "/second)")
    println("Create user photo requests - " + statsCreateUserPhotoReq.toString + " in the last 5 seconds (" + statsCreateUserPhotoReq.toDouble / 5 + "/second)")
    println("Get user photos requests - " + statsGetUserPhotosReq.toString + " in the last 5 seconds (" + statsGetUserPhotosReq.toDouble / 5 + "/second)")
    println("Get user albums requests - " + statsGetUserAlbumsReq.toString + " in the last 5 seconds (" + statsGetUserAlbumsReq.toDouble / 5 + "/second)")
    println("Create user album requests - " + statsCreateUserAlbumReq.toString + " in the last 5 seconds (" + statsCreateUserAlbumReq.toDouble / 5 + "/second)")
    println("Get album photos requests - " + statsGetAlbumPhotosReq.toString + " in the last 5 seconds (" + statsGetAlbumPhotosReq.toDouble / 5 + "/second)")
    println("User like requests - " + statsAddUserLikedPageReq.toString + " in the last 5 seconds (" + statsAddUserLikedPageReq.toDouble / 5 + "/second)")
    println("Get likes (user) requests - " + statsGetUserLikedPagesReq.toString + " in the last 5 seconds (" + statsGetUserLikedPagesReq.toDouble / 5 + "/second)")
    println("Get likes (page) requests - " + statsGetPageLikedUsersReq.toString + " in the last 5 seconds (" + statsGetPageLikedUsersReq.toDouble / 5 + "/second)")
    println("Create page post requests - " + statsCreatePagePostReq.toString + " in the last 5 seconds (" + statsCreatePagePostReq.toDouble / 5 + "/second)")
    println("Get user timeline requests - " + statsGetUserTimelineReq.toString + " in the last 5 seconds (" + statsGetUserTimelineReq.toDouble / 5 + "/second)")
    println("Create page photo requests - " + statsCreatePagePhotoReq.toString + " in the last 5 seconds (" + statsCreatePagePhotoReq.toDouble / 5 + "/second)")
    println("User un-like requests - " + statsRemoveUserLikedPageReq.toString + " in the last 5 seconds (" + statsRemoveUserLikedPageReq.toDouble / 5 + "/second)")
    println("Add friend requests - " + statsAddFriendReq.toString + " in the last 5 seconds (" + statsAddFriendReq.toDouble / 5 + "/second)")
    println("Get incoming friend requests - " + statsGetPendingInFriendsReq.toString + " in the last 5 seconds (" + statsGetPendingInFriendsReq.toDouble / 5 + "/second)")
    println("Get friends requests - " + statsGetFriendsReq.toString + " in the last 5 seconds (" + statsGetFriendsReq.toDouble / 5 + "/second)")
    println("")
    println("")
  }

  def resetStats() = {
    statsCreateFbNodeReqUser = 0
    statsCreateFbNodeReqPage = 0
    statsCreateUserPostReq = 0
    statsCreateUserPostRsp = 0
    statsGetUserFeedReq = 0
    statsGetUserFeedRsp = 0
    statsCreateUserPhotoReq = 0
    statsCreateUserPhotoRsp = 0
    statsGetUserPhotosReq = 0
    statsGetUserPhotosRsp = 0
    statsGetUserAlbumsReq = 0
    statsGetUserAlbumsRsp = 0
    statsCreateUserAlbumReq = 0
    statsCreateUserAlbumRsp = 0
    statsGetAlbumPhotosReq = 0
    statsGetAlbumPhotosRsp = 0
    statsAddUserLikedPageReq = 0
    statsAddUserLikedPageRsp = 0
    statsGetUserLikedPagesReq = 0
    statsGetUserLikedPagesRsp = 0
    statsGetPageLikedUsersReq = 0
    statsGetPageLikedUsersRsp = 0
    statsCreatePagePostReq = 0
    statsCreatePagePostRsp = 0
    statsGetUserTimelineReq = 0
    statsGetUserTimelineRsp = 0
    statsCreatePagePhotoReq = 0
    statsCreatePagePhotoRsp = 0
    statsRemoveUserLikedPageReq = 0
    statsRemoveUserLikedPageRsp = 0
    statsAddFriendReq = 0
    statsAddFriendRsp = 0
    statsGetPendingInFriendsReq = 0
    statsGetPendingInFriendsRsp = 0
    statsGetFriendsReq = 0
    statsGetFriendsRsp = 0
  }

  def receive = {
    case "Init" =>
      context.system.scheduler.scheduleOnce(Duration(5000, "millis"), self, "PrintAndReset")

    case "PrintAndReset" =>
      printStats()
      resetStats()
      context.system.scheduler.scheduleOnce(Duration(5000, "millis"), self, "PrintAndReset")

    case "CreateFbNodeReqUser" =>
      statsCreateFbNodeReqUser += 1

    case "CreateFbNodeReqPage" =>
      statsCreateFbNodeReqPage += 1

    case "CreateUserPostReq" =>
      statsCreateUserPostReq += 1

    case "CreateUserPostRsp" =>
      statsCreateUserPostRsp += 1

    case "GetUserFeedReq" =>
      statsGetUserFeedReq += 1

    case "GetUserFeedRsp" =>
      statsGetUserFeedRsp += 1

    case "CreateUserPhotoReq" =>
      statsCreateUserPhotoReq += 1

    case "CreateUserPhotoRsp" =>
      statsCreateUserPhotoRsp += 1

    case "GetUserPhotosReq" =>
      statsGetUserPhotosReq += 1

    case "GetUserPhotosRsp" =>
      statsGetUserPhotosRsp += 1

    case "GetUserAlbumsReq" =>
      statsGetUserAlbumsReq += 1

    case "GetUserAlbumsRsp" =>
      statsGetUserAlbumsRsp += 1

    case "CreateUserAlbumReq" =>
      statsCreateUserAlbumReq += 1

    case "CreateUserAlbumRsp" =>
      statsCreateUserAlbumRsp += 1

    case "GetAlbumPhotosReq" =>
      statsGetAlbumPhotosReq += 1

    case "GetAlbumPhotosRsp" =>
      statsGetAlbumPhotosRsp += 1

    case "AddUserLikedPageReq" =>
      statsAddUserLikedPageReq += 1

    case "AddUserLikedPageRsp" =>
      statsAddUserLikedPageRsp += 1

    case "GetUserLikedPagesReq" =>
      statsGetUserLikedPagesReq += 1

    case "GetUserLikedPagesRsp" =>
      statsGetUserLikedPagesRsp += 1

    case "GetPageLikedUsersReq" =>
      statsGetPageLikedUsersReq += 1

    case "GetPageLikedUsersRsp" =>
      statsGetPageLikedUsersRsp += 1

    case "CreatePagePostReq" =>
      statsCreatePagePostReq += 1

    case "CreatePagePostRsp" =>
      statsCreatePagePostRsp += 1

    case "GetUserTimelineReq" =>
      statsGetUserTimelineReq += 1

    case "GetUserTimelineRsp" =>
      statsGetUserTimelineRsp += 1

    case "CreatePagePhotoReq" =>
      statsCreatePagePhotoReq += 1

    case "CreatePagePhotoRsp" =>
      statsCreatePagePhotoRsp += 1

    case "RemoveUserLikedPageReq" =>
      statsRemoveUserLikedPageReq += 1

    case "RemoveUserLikedPageRsp" =>
      statsRemoveUserLikedPageRsp += 1

    case "AddFriendReq" =>
      statsAddFriendReq += 1

    case "AddFriendRsp" =>
      statsAddFriendRsp += 1

    case "GetPendingInFriendsReq" =>
      statsGetPendingInFriendsReq += 1

    case "GetPendingInFriendsRsp" =>
      statsGetPendingInFriendsRsp += 1

    case "GetFriendsReq" =>
      statsGetFriendsReq += 1

    case "GetFriendsRsp" =>
      statsGetFriendsRsp += 1

    case _ =>
      println("Unknown stats")
  }
}
