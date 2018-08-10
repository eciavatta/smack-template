package smack.entrypoints

import akka.actor.ActorSystem
import akka.cluster.seed.ZookeeperClusterSeed
import com.typesafe.config.ConfigFactory
import kamon.Kamon
import kamon.datadog.{DatadogAPIReporter, DatadogAgentReporter}
import smack.BuildInfo
import smack.backend.BackendSupervisor
import smack.frontend.server.WebServer

object Main {

  private val configRoles = Map("frontend" -> "frontend", "backend" -> "backend")

  def main(args: Array[String]): Unit = {
    val params = checkAndGetConfig(args)
    val config = ConfigFactory.load(configRoles(params.role))
    val system: ActorSystem = ActorSystem(config.getString("name"), config)
    ZookeeperClusterSeed(system).join()

    if (params.withDatadogAgent) {
      Kamon.addReporter(new DatadogAgentReporter())
    }

    if (params.withDatadogApi) {
      Kamon.addReporter(new DatadogAPIReporter())
    }

    params.role match {
      case "frontend" =>
        val server = new WebServer(system, config)
        server.start()

        sys.addShutdownHook {
          server.stop()
        }
      case "backend" =>
        system.actorOf(BackendSupervisor.props, BackendSupervisor.name)
    }
  }

  private def argumentParser = new scopt.OptionParser[Config](BuildInfo.name) {
    head(BuildInfo.name, BuildInfo.version)

    opt[Unit]("with-datadog-agent").optional().action((_, c) =>
      c.copy(withDatadogAgent = true)).text("...")

    opt[Unit]("with-datadog-api").optional().action((_, c) =>
      c.copy(withDatadogApi = true)).text("...")

    arg[String]("<role>").required().action((x, c) =>
      c.copy(role = x)).validate(x => if (configRoles.isDefinedAt(x)) success else failure("undefined role")).text("...")

    note("...")
  }

  private def checkAndGetConfig(args: Array[String]) = argumentParser.parse(args, Config()) match {
    case Some(config) => config
    case None => sys.exit(1)
  }

  private case class Config(withDatadogAgent: Boolean = false, withDatadogApi: Boolean = false, role: String = "")

}
