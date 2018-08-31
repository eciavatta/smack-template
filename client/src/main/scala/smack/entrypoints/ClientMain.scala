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
      .text("0 for infinite")

    opt[Boolean]('d', "debug").optional()
      .action((debug, config) => config.copy(debug = Some(debug)))
      .text("...")

    opt[String]('h', "host").optional()
      .action((host, config) => config.copy(host = host))
      .text("...")

    opt[String]('l',"loglevel").optional()
      .action((level, config) => config.copy(logLevel = level))
      .validate(level => if (Seq("error", "warning", "info", "debug", "off").contains(level.toLowerCase)) success else failure("undefined loglevel"))
      .text("...")

    opt[Int]('r', "parallelism").optional()
      .action((parallelism, config) => config.copy(parallelism = parallelism))
      .text("...")

    opt[Int]('p', "port").optional()
      .action((port, config) => config.copy(port = port))
      .text("...")

    opt[Int]('n', "requests-per-second").optional()
      .action((requestsPerSecond, config) => config.copy(requestsPerSecond = requestsPerSecond))
      .text("...")

    opt[String]("sentry-dns").optional()
      .action((dns, config) => config.copy(sentryDns = Some(dns)))
      .text("...")

    note("...")
  }

}

case class ClientMain(count: Long = 0, debug: Option[Boolean] = None, host: String = "127.0.0.1", logLevel: String = "INFO", parallelism: Int = 2,
                      port: Int = ClientMain.defaultClientPort, requestsPerSecond: Int = ClientMain.defaultClientRequestsPerSecond,
                      sentryDns: Option[String] = None)
