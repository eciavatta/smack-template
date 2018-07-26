package smack.backend

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import smack.backend.server.WebServer

object Main extends App {

  implicit val system: ActorSystem = ActorSystem()
  implicit val materializer: ActorMaterializer = ActorMaterializer()

  val server = new WebServer
  server.start()

  sys.addShutdownHook {
    server.stop()
  }

}
