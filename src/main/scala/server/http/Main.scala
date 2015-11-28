package server.http

import akka.actor._
import akka.io.IO
import spray.can._

object Main extends App {

  implicit val system = ActorSystem()

  val handler = system.actorOf(Props[FbHttpServer], name = "handler")

  IO(Http) ! Http.Bind(handler, interface = "localhost", port = 8080)

}
