package smack

import akka.actor.{ActorSystem, Props}
import akka.cluster.seed.ZookeeperClusterSeed
import com.typesafe.config.ConfigFactory
import smack.backend.Backend
import smack.frontend.server.WebServer

object Main {

  def main(args: Array[String]): Unit = {
    val role = if (args.isEmpty) "undefined" else args(0)
    val config = ConfigFactory.load(getConfigFile(role))
    implicit val system: ActorSystem = ActorSystem(config.getString("name"), config)

    role match {
      case "frontend" =>
        ZookeeperClusterSeed(system).join()

        val server = new WebServer(system, config)
        server.start()

        sys.addShutdownHook {
          server.stop()
        }

      case "backend" =>
        ZookeeperClusterSeed(system).join()
        system.actorOf(Props[Backend], name = "backendWorker")

      case _ =>
        system.log.error("Undefined role. Please specify node role as first argument.")
        system.terminate()
    }
  }

  def getConfigFile(role: String): String = role match {
    case "frontend" => "frontend"
    case _ => "application"
  }

}
