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

    val user00 = new UserNode(0.toString, "About user00", "1/1/1900", "user00@gmail.com", "User00")
    val user01 = new UserNode(1.toString, "About user01", "1/1/1900", "user01@gmail.com", "User01")

    var future: Future[CreateFbNodeRsp] = (server00 ? CreateFbNodeReq("user", user00)).mapTo[CreateFbNodeRsp]
    var createFbNodeRsp = Await.result(future, someTimeout.duration)
    if (createFbNodeRsp.result) {
      println("user00 added")
    }

    future = (server00 ? CreateFbNodeReq("user", user01)).mapTo[CreateFbNodeRsp]
    createFbNodeRsp = Await.result(future, someTimeout.duration)
    if (createFbNodeRsp.result) {
      println("user01 added")
    }

    val now = Calendar.getInstance().getTime.toString

    val post00 = new PostNode("", now, "post00 desc", "user00", "I'm tagging @1@2 in post00", List.empty, now)
    val post01 = new PostNode("", now, "post01 desc", "user00", "I'm tagging nobody in post01", List.empty, now)
    val post02 = new PostNode("", now, "post02 desc", "user00", "I'm tagging @2@1 in post02", List.empty, now)

    val futurePostRsp: Future[CreateUserPostRsp] = (server00 ? CreateUserPostReq(0.toString, post00)).mapTo[CreateUserPostRsp]
    val createUserPostRsp = Await.result(futurePostRsp, someTimeout.duration)
    println(createUserPostRsp.postId)

    val futurePostRsp01: Future[CreateUserPostRsp] = (server00 ? CreateUserPostReq(0.toString, post01)).mapTo[CreateUserPostRsp]
    val createUserPostRsp01 = Await.result(futurePostRsp01, someTimeout.duration)
    println(createUserPostRsp01.postId)

    val futurePostRsp02: Future[CreateUserPostRsp] = (server00 ? CreateUserPostReq(0.toString, post02)).mapTo[CreateUserPostRsp]
    val createUserPostRsp02 = Await.result(futurePostRsp02, someTimeout.duration)
    println(createUserPostRsp02.postId)

    val futureGetPostRsp: Future[GetUserFeedRsp] = (server00 ? GetUserFeedReq(1.toString, "tagged", "", 0)).mapTo[GetUserFeedRsp]
    val getUserPostRsp = Await.result(futureGetPostRsp, someTimeout.duration)
    getUserPostRsp.posts.foreach(post => {
      println(post)
    })
  }
}