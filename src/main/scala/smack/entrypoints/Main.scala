package smack.entrypoints

import akka.actor.ActorSystem
import akka.cluster.seed.ZookeeperClusterSeed
import com.typesafe.config.ConfigFactory
import kamon.Kamon
import kamon.datadog.{DatadogAPIReporter, DatadogAgentReporter}
import smack.BuildInfo
import smack.backend.{BackendSupervisor, ServiceSupervisor}
import smack.database.MigrationController
import smack.frontend.server.WebServer

import scala.util.matching.Regex

object Main {

  private val configRoles: Set[String] = Set("frontend", "backend", "service")
  private val addressPattern: Regex = "^([\\w-\\.]{3,}):(\\d{3,5})$".r
  private val zooPattern: Regex = "^([\\w-\\.]{3,}):(\\d{3,5})(\\/\\w+)$$".r

  private val defaultLogger: String = "akka.event.Logging$DefaultLogger"
  private val sentryLogger: String = "smack.common.utils.SentryLogger"

  def main(args: Array[String]): Unit = {
    val params = checkAndGetConfig(args)

    val akkaZooRegex = zooPattern.findFirstMatchIn(params.akkaZoo).get
    val datadogAgentRegex = addressPattern.findFirstMatchIn(params.datadogAgent).get
    val kafkaRegex = addressPattern.findFirstMatchIn(params.kafka).get
    val cassandraRegex = addressPattern.findFirstMatchIn(params.cassandra).get

    var config = ConfigFactory.parseString(
      s"""
         |akka.loggers = ["$defaultLogger"${if (params.sentryDns.isDefined) ", \"" + sentryLogger + "\"" else ""}]
         |akka.loglevel = "${params.logLevel.toUpperCase}"
         |akka.cluster.seed.zookeeper.url = "${akkaZooRegex.group(1)}:${akkaZooRegex.group(2)}"
         |akka.cluster.seed.zookeeper.path = "${akkaZooRegex.group(3)}"
         |kamon.datadog.agent.hostname = "${datadogAgentRegex.group(1)}"
         |kamon.datadog.agent.port = "${datadogAgentRegex.group(2)}"
         |${params.debug.fold("")(debug => s"smack.debug = ${if (debug) "on" else "off"}")}
         |smack.kafka.consumer.bootstrap-server = "${kafkaRegex.group(1)}:${kafkaRegex.group(2)}"
         |smack.kafka.producer.bootstrap-server = "${kafkaRegex.group(1)}:${kafkaRegex.group(2)}"
         |smack.cassandra.contact-point.host = "${cassandraRegex.group(1)}"
         |smack.cassandra.contact-point.port = ${cassandraRegex.group(2)}
         |smack.sentry.dns = "${params.sentryDns.fold("")(identity)}"
       """.stripMargin)

    if (params.role.isDefined) {
      config = config.withFallback(ConfigFactory.parseResources(s"${params.role.get}.conf"))
      config = config.withFallback(ConfigFactory.parseResources("application.conf"))
    }
    config = config.withFallback(ConfigFactory.parseResources(s"smack-${params.environment}.conf")).resolve()

    val system: ActorSystem = ActorSystem(config.getString("smack.name"), config)

    if (params.migrate.isDefined) {
      val migrationController = MigrationController.createMigrationController(system)

      params.migrate.get match {
        case "migrate" => migrationController.migrate(params.migrateForce, params.createKeyspace)
        case "rollback" => migrationController.rollback(params.migrateForce, params.rollbackSteps)
        case "reset" => migrationController.reset(params.migrateForce)
      }

      sys.exit(0)
    }

    ZookeeperClusterSeed(system).join()

    if (params.datadogAgentEnabled) {
      Kamon.addReporter(new DatadogAgentReporter())
    }

    if (params.datadogApiEnabled) {
      Kamon.addReporter(new DatadogAPIReporter())
    }

    params.role.get match {
      case "frontend" =>
        val server = WebServer.create(system)
        server.start()
        system.registerOnTermination(server.stop())
      case "backend" =>
        system.actorOf(BackendSupervisor.props, BackendSupervisor.name)
      case "service" =>
        system.actorOf(ServiceSupervisor.props, ServiceSupervisor.name)
    }
  }

  private def argumentParser = new scopt.OptionParser[Config](BuildInfo.name) {
    head(BuildInfo.name, BuildInfo.version)

    opt[String]('l',"loglevel").optional()
      .action((level, config) => config.copy(logLevel = level))
      .validate(level => if (Seq("error", "warning", "info", "debug", "off").contains(level.toLowerCase)) success else failure("undefined loglevel"))
      .text("...")

    opt[String]('a', "akka-zookeeper").optional()
      .action((zoo, config) => config.copy(akkaZoo = zoo))
      .validate(zoo => zooPattern.findFirstIn(zoo).fold(failure("invalid zookeeper url"))(_ => success))
      .text("...")

    opt[String]("datadog-agent").optional()
      .action((agent, config) => config.copy(datadogAgent = agent, datadogAgentEnabled = true))
      .validate(agent => addressPattern.findFirstIn(agent).fold(failure("invalid datadog agent address"))(_ => success))
      .text("...")

    opt[String]("datadog-api").optional()
      .action((api, config) => config.copy(datadogApi = api, datadogApiEnabled = true))
      .text("...")

    opt[String]("sentry-dns").optional()
      .action((dns, config) => config.copy(sentryDns = Some(dns)))
      .text("...")

    opt[String]('k', "kafka-bootstrap").optional()
      .action((kafka, config) => config.copy(kafka = kafka))
      .validate(kafka => addressPattern.findFirstIn(kafka).fold(failure("invalid kafka address"))(_ => success))
      .text("...")

    opt[String]('c', "cassandra-bootstrap").optional()
      .action((cassandra, config) => config.copy(cassandra = cassandra))
      .validate(cassandra => addressPattern.findFirstIn(cassandra).fold(failure("invalid cassandra address"))(_ => success))
      .text("...")

    opt[Boolean]('d', "debug").optional()
      .action((debug, config) => config.copy(debug = Some(debug)))
      .text("...")

    opt[String]('e', "environment").optional()
      .action((environment, config) => config.copy(environment = environment))
      .validate(environment => if (Seq("development", "production").contains(environment)) success else failure("undefined environment"))
      .text("...")

    arg[String]("<role>").optional()
      .action((role, config) => config.copy(role = Some(role)))
      .validate(role => if (configRoles.contains(role)) success else failure("undefined role"))
      .text("...")

    cmd("migrate")
      .action((_, c) => c.copy(migrate = Some("migrate")))
      .children(
        opt[Boolean]("force")
          .action((force, config) => config.copy(migrateForce = force))
          .text("..."),
        opt[Unit]("create-keyspace").optional()
        .action((_, config) => config.copy(createKeyspace = true))
        .text("...")
      )
      .text("...")

    cmd("migrate:rollback")
      .action((_, c) => c.copy(migrate = Some("rollback")))
      .children(
        opt[Boolean]("force").optional()
          .action((force, config) => config.copy(migrateForce = force))
          .text("..."),
        opt[Int]("steps").optional()
          .action((steps, config) => config.copy(rollbackSteps = steps))
          .validate(steps => if (steps > 0) success else failure("steps must be greater than 0"))
          .text("...")
      )
      .text("...")

    cmd("migrate:reset")
      .action((_, c) => c.copy(migrate = Some("reset")))
      .children(
        opt[Boolean]("force")
          .action((force, config) => config.copy(migrateForce = force))
          .text("...")
      )
      .text("...")

    checkConfig {
      case config if config.migrate.isDefined && config.role.isDefined => failure("invalid param <role>")
      case config if config.migrate.isEmpty && config.role.isEmpty => failure("undefined param <role>")
      case _ => success
    }

    note("...")
  }

  private def checkAndGetConfig(args: Array[String]): Config = argumentParser.parse(args, Config()) match {
    case Some(config) => config
    case None => sys.exit(1)
  }

  private case class Config(akkaZoo: String = "127.0.0.1:2181/akka",
                            cassandra: String = "127.0.0.1:9042",
                            datadogAgent: String = "127.0.0.1:8126",
                            datadogAgentEnabled: Boolean = false,
                            datadogApi: String = "",
                            datadogApiEnabled: Boolean = false,
                            debug: Option[Boolean] = None,
                            environment: String = "development",
                            kafka: String = "127.0.0.1:9092",
                            logLevel: String = "INFO",
                            role: Option[String] = None,
                            sentryDns: Option[String] = None,
                            migrate: Option[String] = None,
                            migrateForce: Boolean = false,
                            rollbackSteps: Int = 1,
                            createKeyspace: Boolean = false)

}
