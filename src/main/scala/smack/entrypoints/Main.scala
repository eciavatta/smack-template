package smack.entrypoints

import akka.actor.ActorSystem
import akka.cluster.seed.ZookeeperClusterSeed
import com.typesafe.config.ConfigFactory
import smack.backend.BackendSupervisor
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
        system.actorOf(BackendSupervisor.props, BackendSupervisor.name)

        sys.addShutdownHook {
          system.terminate()
        }

      case _ =>
        system.log.error("Undefined role. Please specify node role as first argument.")
        system.terminate()
    }
  }

  def getConfigFile(role: String): String = role match {
    case "frontend" => "frontend"
    case "backend" => "backend"
    case _ => "application"
  }

}
