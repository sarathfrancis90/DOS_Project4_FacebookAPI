package server.facebook

import java.security.MessageDigest

import akka.actor.{Actor, ActorLogging}

import scala.collection.mutable

class FbServer extends Actor with ActorLogging {

  var users: mutable.HashMap[String, Node] = new mutable.HashMap[String, Node]()
  var pages: mutable.HashMap[String, Node] = new mutable.HashMap[String, Node]()
  var posts: mutable.HashMap[String, Node] = new mutable.HashMap[String, Node]()

  def receive = {
    case CreateFbNodeReq(nodeType, node) =>
      nodeType match {
        case "user" =>
          val userNode = node.asInstanceOf[UserNode]
          userNode.id = Sha256(userNode.email)
          sender ! addToDb(users, userNode.id, userNode)

        case "page" =>
          val pageNode = node.asInstanceOf[PageNode]
          pageNode.id = Sha256(pageNode.name)
          sender ! addToDb(pages, pageNode.id, pageNode)

        case "post" =>
          val postNode = node.asInstanceOf[PostNode]
          postNode.id = Sha256(postNode.message)
          sender ! addToDb(posts, postNode.id, postNode)
      }

  }

  def addToDb(db: mutable.HashMap[String, Node], key: String, value: Node): CreateFbNodeRsp = {
    var result = false
    var id: String = ""
    if (!db.contains(key)) {
      result = true
      id = key
      db.put(key, value)
    }
    CreateFbNodeRsp(result, id)
  }

  def Sha256(s: String): String = {
    val m = MessageDigest.getInstance("SHA-256").digest(s.getBytes("UTF-8"))
    m.map("%02x".format(_)).mkString
  }
}
