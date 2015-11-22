package server.facebook

import java.security.MessageDigest

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
      post.id = Sha256(post.message + post.from)
      post.to = parseMessageAndFindUsernames(post.message)

      // create the post on server that will host it
      val future: Future[CreateFbNodeRsp] = (myFbServerRef ? CreateFbNodeReq("post", post)).mapTo[CreateFbNodeRsp]
      val createFbNodeRsp = Await.result(future, someTimeout.duration)
      if (createFbNodeRsp.result) {

      }

      ownPosts.insert(0, post.id)

      post.to.foreach(taggedUser => {
        // please don't tag yourself
        myFbServerRef ! UpdateUserTaggedPostNtf(taggedUser, post.id)
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

    case PleaseKillYourself =>
      context.stop(self)
  }

  def Sha256(s: String): String = {
    val m = MessageDigest.getInstance("SHA-1").digest(s.getBytes("UTF-8"))
    m.map("%02x".format(_)).mkString
  }

  def parseMessageAndFindUsernames(message: String): List[String] = {
    val usernames: ListBuffer[String] = new ListBuffer[String]()
    val usernamePattern = "@([A-Za-z0-9_]+)".r
    val matches = usernamePattern.findAllIn(message)
    matches.foreach(aMatch => {
      usernames += aMatch.drop(1)
    })
    usernames.toList
  }
}