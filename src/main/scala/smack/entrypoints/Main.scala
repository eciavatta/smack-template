package smack.entrypoints

import akka.actor.ActorSystem
import com.typesafe.config.ConfigFactory
import kamon.Kamon
import kamon.datadog.{DatadogAPIReporter, DatadogAgentReporter}
import scopt.OptionParser
import smack.BuildInfo
import smack.backend.{BackendSupervisor, ServiceSupervisor}
import smack.commons.abstracts.EntryPoint
import smack.frontend.server.WebServer

object Main extends EntryPoint[Main] {

  private val configRoles: Set[String] = Set("frontend", "backend", "service", "seed")

  def main(args: Array[String]): Unit = {
    var params = checkAndGetConfig(args, Main())

    if (params.cassandraContactPoints.isEmpty) {
      params = params.copy(cassandraContactPoints = Seq("127.0.0.1:9042"))
    }
    if (params.kafkaBootstrapServers.isEmpty) {
      params = params.copy(kafkaBootstrapServers = Seq("127.0.0.1:9092"))
    }

    val datadogAgentRegex = addressPattern.findFirstMatchIn(params.datadogAgent).get

    val env = params.environment match {
      case "development" => "dev"
      case "production" => "prod"
      case "testing" => "test"
    }

    val name = s"${BuildInfo.name}-$env"
    val config = ConfigFactory.parseString(
      s"""
         |akka.cluster.seed-nodes = [${params.akkaSeeds.map(akka => s""""akka://$name@$akka"""").mkString(",")}]
         |${params.debug.fold("")(debug => s"smack.debug = ${if (debug) "on" else "off"}")}
         |akka.loggers = ["$defaultLogger"${if (params.sentryDns.isDefined) ", \"" + sentryLogger + "\"" else ""}]
         |akka.loglevel = "${params.logLevel.toUpperCase}"
         |kamon.datadog.agent.hostname = "${datadogAgentRegex.group(1)}"
         |kamon.datadog.agent.port = "${datadogAgentRegex.group(2)}"
         |smack.kafka.consumer.bootstrap-servers = [${params.kafkaBootstrapServers.map(kafka => s""""$kafka"""").mkString(",")}]
         |smack.kafka.producer.bootstrap-servers = [${params.kafkaBootstrapServers.map(kafka => s""""$kafka"""").mkString(",")}]
         |smack.cassandra.contact-points = [${params.cassandraContactPoints.map(cassandra => s""""$cassandra"""").mkString(",")}]
         |smack.sentry.dns = "${params.sentryDns.fold("")(identity)}"
         |smack.name = "${BuildInfo.name}-$env"
         |smack.version = "${BuildInfo.version}"
         |smack.scala-version = "${BuildInfo.scalaVersion}"
         |smack.sbt-version = "${BuildInfo.sbtVersion}"
       """.stripMargin)
      .withFallback(ConfigFactory.parseResources(s"${params.role}.conf"))
      .withFallback(ConfigFactory.parseResources("application.conf"))
      .withFallback(ConfigFactory.parseResources(s"commons-${params.environment}.conf")).resolve()

    val system: ActorSystem = ActorSystem(name, config)

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
      case "seed" => // none
    }
  }

  override protected def argumentParser: OptionParser[Main] = new scopt.OptionParser[Main](BuildInfo.name) {
    head(BuildInfo.name, BuildInfo.version)

    opt[String]('a', "akka-seeds").required().unbounded()
      .action((akka, config) => config.copy(akkaSeeds = config.akkaSeeds :+ akka))
      .validate(akka => addressPattern.findFirstIn(akka).fold(failure("invalid akka-seeds address"))(_ => success))
      .text("...")

    opt[String]('c', "cassandra-contact-points").optional().unbounded()
      .action((cassandra, config) => config.copy(cassandraContactPoints = config.cassandraContactPoints :+ cassandra))
      .validate(cassandra => addressPattern.findFirstIn(cassandra).fold(failure("invalid cassandra-contact-points address"))(_ => success))
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
      .validate(environment => if (Seq("development", "production", "testing").contains(environment)) success else failure("undefined environment"))
      .text("...")

    opt[String]('k', "kafka-bootstraps").optional().unbounded()
      .action((kafka, config) => config.copy(kafkaBootstrapServers = config.kafkaBootstrapServers :+ kafka))
      .validate(kafka => addressPattern.findFirstIn(kafka).fold(failure("invalid kafka-bootstraps address"))(_ => success))
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

case class Main(akkaSeeds: Seq[String] = Seq(), cassandraContactPoints: Seq[String] = Seq(), datadogAgent: String = "127.0.0.1:8126",
                datadogAgentEnabled: Boolean = false, datadogApi: String = "", datadogApiEnabled: Boolean = false, debug: Option[Boolean] = None,
                environment: String = "development", kafkaBootstrapServers: Seq[String] = Seq(), logLevel: String = "INFO", role: String = "",
                sentryDns: Option[String] = None)
