package smack

import akka.actor.ActorSystem
import akka.cluster.seed.ZookeeperClusterSeed
import com.typesafe.config.ConfigFactory
import smack.frontend.server.WebServer

object RunFrontend extends App {

  val config = ConfigFactory.load("frontend")
  implicit val system: ActorSystem = ActorSystem(config.getString("name"), config)
  ZookeeperClusterSeed(system).join()

  val server = new WebServer(system, config)
  server.start()

  sys.addShutdownHook {
    server.stop()
  }


}
