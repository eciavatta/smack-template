package smack.entrypoints

import akka.actor.ActorSystem
import akka.cluster.seed.ZookeeperClusterSeed
import com.typesafe.config.ConfigFactory
import kamon.Kamon
import kamon.datadog.{DatadogAPIReporter, DatadogAgentReporter}
import scopt.OptionParser
import smack.BuildInfo
import smack.backend.{BackendSupervisor, ServiceSupervisor}
import smack.commons.abstracts.EntryPoint
import smack.frontend.server.WebServer

object Main extends EntryPoint[Main] {

  private val configRoles: Set[String] = Set("frontend", "backend", "service")

  def main(args: Array[String]): Unit = {
    val params = checkAndGetConfig(args, Main())

    val akkaZooRegex = zooPattern.findFirstMatchIn(params.akkaZoo).get
    val cassandraRegex = addressPattern.findFirstMatchIn(params.cassandra).get
    val datadogAgentRegex = addressPattern.findFirstMatchIn(params.datadogAgent).get
    val kafkaRegex = addressPattern.findFirstMatchIn(params.kafka).get

    val config = ConfigFactory.parseString(
      s"""
         |${params.debug.fold("")(debug => s"smack.debug = ${if (debug) "on" else "off"}")}
         |akka.loggers = ["$defaultLogger"${if (params.sentryDns.isDefined) ", \"" + sentryLogger + "\"" else ""}]
         |akka.loglevel = "${params.logLevel.toUpperCase}"
         |akka.cluster.seed.zookeeper.url = "${akkaZooRegex.group(1)}:${akkaZooRegex.group(2)}"
         |akka.cluster.seed.zookeeper.path = "${akkaZooRegex.group(3)}"
         |kamon.datadog.agent.hostname = "${datadogAgentRegex.group(1)}"
         |kamon.datadog.agent.port = "${datadogAgentRegex.group(2)}"
         |smack.kafka.consumer.bootstrap-server = "${kafkaRegex.group(1)}:${kafkaRegex.group(2)}"
         |smack.kafka.producer.bootstrap-server = "${kafkaRegex.group(1)}:${kafkaRegex.group(2)}"
         |smack.cassandra.contact-point.host = "${cassandraRegex.group(1)}"
         |smack.cassandra.contact-point.port = ${cassandraRegex.group(2)}
         |smack.sentry.dns = "${params.sentryDns.fold("")(identity)}"
       """.stripMargin)
      .withFallback(ConfigFactory.parseResources(s"${params.role}.conf"))
      .withFallback(ConfigFactory.parseResources("application.conf"))
      .withFallback(ConfigFactory.parseResources(s"smack-${params.environment}.conf")).resolve()

    val system: ActorSystem = ActorSystem(config.getString("smack.name"), config)
    ZookeeperClusterSeed(system).join()

    if (params.datadogAgentEnabled) {
      Kamon.addReporter(new DatadogAgentReporter())
    }

    if (params.datadogApiEnabled) {
      Kamon.addReporter(new DatadogAPIReporter())
    }

    params.role match {
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

  override protected def argumentParser: OptionParser[Main] = new scopt.OptionParser[Main](BuildInfo.name) {
    head(BuildInfo.name, BuildInfo.version)

    opt[String]('a', "akka-zookeeper").optional()
      .action((zoo, config) => config.copy(akkaZoo = zoo))
      .validate(zoo => zooPattern.findFirstIn(zoo).fold(failure("invalid zookeeper url"))(_ => success))
      .text("...")

    opt[String]('c', "cassandra-bootstrap").optional()
      .action((cassandra, config) => config.copy(cassandra = cassandra))
      .validate(cassandra => addressPattern.findFirstIn(cassandra).fold(failure("invalid cassandra address"))(_ => success))
      .text("...")

    opt[String]("datadog-agent").optional()
      .action((agent, config) => config.copy(datadogAgent = agent, datadogAgentEnabled = true))
      .validate(agent => addressPattern.findFirstIn(agent).fold(failure("invalid datadog agent address"))(_ => success))
      .text("...")

    opt[String]("datadog-api").optional()
      .action((api, config) => config.copy(datadogApi = api, datadogApiEnabled = true))
      .text("...")

    opt[Boolean]('d', "debug").optional()
      .action((debug, config) => config.copy(debug = Some(debug)))
      .text("...")

    opt[String]('e', "environment").optional()
      .action((environment, config) => config.copy(environment = environment))
      .validate(environment => if (Seq("development", "production").contains(environment)) success else failure("undefined environment"))
      .text("...")

    opt[String]('k', "kafka-bootstrap").optional()
      .action((kafka, config) => config.copy(kafka = kafka))
      .validate(kafka => addressPattern.findFirstIn(kafka).fold(failure("invalid kafka address"))(_ => success))
      .text("...")

    opt[String]('l',"loglevel").optional()
      .action((level, config) => config.copy(logLevel = level))
      .validate(level => if (Seq("error", "warning", "info", "debug", "off").contains(level.toLowerCase)) success else failure("undefined loglevel"))
      .text("...")

    opt[String]("sentry-dns").optional()
      .action((dns, config) => config.copy(sentryDns = Some(dns)))
      .text("...")

    arg[String]("<role>").required()
      .action((role, config) => config.copy(role = role))
      .validate(role => if (configRoles.contains(role)) success else failure("undefined role"))
      .text("...")

    note("...")
  }

}

case class Main(akkaZoo: String = "127.0.0.1:2181/akka", cassandra: String = "127.0.0.1:9042", datadogAgent: String = "127.0.0.1:8126",
                datadogAgentEnabled: Boolean = false, datadogApi: String = "", datadogApiEnabled: Boolean = false, debug: Option[Boolean] = None,
                environment: String = "development", kafka: String = "127.0.0.1:9092", logLevel: String = "INFO", role: String = "",
                sentryDns: Option[String] = None)
