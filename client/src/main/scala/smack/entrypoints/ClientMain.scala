package smack.entrypoints

import akka.actor.ActorSystem
import com.typesafe.config.ConfigFactory
import scopt.OptionParser
import smack.client.{BuildInfo, WebClient}
import smack.commons.abstracts.EntryPoint

object ClientMain extends EntryPoint[ClientMain] {

  private val defaultClientPort = 80
  private val defaultClientRequestsPerSecond = 20

  def main(args: Array[String]): Unit = {
    val params = checkAndGetConfig(args, ClientMain())

    val config = ConfigFactory.parseString(
      s"""
         |${params.debug.fold("")(debug => s"smack.debug = ${if (debug) "on" else "off"}")}
         |akka.loggers = ["$defaultLogger"${if (params.sentryDns.isDefined) ", \"" + sentryLogger + "\"" else ""}]
         |akka.loglevel = "${params.logLevel.toUpperCase}"
         |smack.client.host = "${params.host}"
         |smack.client.port = ${params.port}
         |smack.client.parallelism = ${params.parallelism}
         |smack.client.requests-per-second = ${params.requestsPerSecond}
         |smack.client.count = ${params.count}
         |smack.sentry.dns = "${params.sentryDns.fold("")(identity)}"
         |smack.name = "${BuildInfo.name}"
         |smack.version = "${BuildInfo.version}"
         |smack.scala-version = "${BuildInfo.scalaVersion}"
         |smack.sbt-version = "${BuildInfo.sbtVersion}"
       """.stripMargin)

    val system: ActorSystem = ActorSystem("smack-client", config)
    system.actorOf(WebClient.props, WebClient.name)
  }

  override protected def argumentParser: OptionParser[ClientMain] = new scopt.OptionParser[ClientMain](BuildInfo.name) {
    head(BuildInfo.name, BuildInfo.version)

    opt[Long]('c', "count").optional()
      .action((count, config) => config.copy(count = count))
      .valueName("<num>")
      .text("The number of requests to do before stop (0 for infinite)")

    opt[Boolean]('d', "debug").optional()
      .action((debug, config) => config.copy(debug = Some(debug)))
      .valueName("<bool>")
      .text("True if debug should be enabled")

    opt[String]('h', "host").optional()
      .action((host, config) => config.copy(host = host))
      .valueName("<addr>")
      .text("The host of the service to test (default: localhost)")

    opt[String]('l',"loglevel").optional()
      .action((level, config) => config.copy(logLevel = level))
      .validate(level => if (Seq("error", "warning", "info", "debug", "off").contains(level.toLowerCase)) success else failure("undefined loglevel"))
      .valueName("<level>")
      .text("The log level used by standard output and (optionally) by sentry logger (default: info)")

    opt[Int]('r', "parallelism").optional()
      .action((parallelism, config) => config.copy(parallelism = parallelism))
      .valueName("<num>")
      .text("The number of actors to start (default: 2)")

    opt[Int]('p', "port").optional()
      .action((port, config) => config.copy(port = port))
      .valueName("<port>")
      .text(s"The port of the service to test (default: $defaultClientPort)")

    opt[Int]('n', "requests-per-second").optional()
      .action((requestsPerSecond, config) => config.copy(requestsPerSecond = requestsPerSecond))
      .valueName("<num>")
      .text(s"The number of requests per second that each actor should send (default: $defaultClientRequestsPerSecond)")

    opt[String]("sentry-dns").optional()
      .action((dns, config) => config.copy(sentryDns = Some(dns)))
      .valueName("<key>")
      .text("If defined, every logs are sent to sentry servers and can be viewed on Sentry.io. The standard output remain unchanged")

    help("help").text("Display help")

    note("Tool that can be used to stress test the smack system.")
  }

}

case class ClientMain(count: Long = 0, debug: Option[Boolean] = None, host: String = "127.0.0.1", logLevel: String = "INFO", parallelism: Int = 2,
                      port: Int = ClientMain.defaultClientPort, requestsPerSecond: Int = ClientMain.defaultClientRequestsPerSecond,
                      sentryDns: Option[String] = None)
