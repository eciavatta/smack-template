package smack.commons.utils

import akka.actor.Actor
import akka.event.Logging._
import io.sentry.event.Event.Level
import io.sentry.event.EventBuilder
import io.sentry.{SentryClient, SentryClientFactory}

class SentryLogger extends Actor {

  import System._

  private var sentry: SentryClient = _
  private val config = Helpers.actorConfig
  private val version = s"${config.getString("smack.name")}-${config.getString("smack.version")}"
  private val environment = s"scalaVersion: ${config.getString("smack.scala-version")} - sbtVersion: ${config.getString("smack.sbt-version")}"
  private val platform = s"${getProperty("os.name")}_${getProperty("os.version")} - ${getProperty("java.vendor")}_${getProperty("java.version")}"
  private val serverInstance = s"${config.getString("akka.remote.netty.tcp.hostname")}:${config.getString("akka.remote.netty.tcp.port")}"

  override def preStart(): Unit = {
    sentry = SentryClientFactory.sentryClient(config.getString("smack.sentry.dns"))
    sentry.addTag("environment", environment)
    sentry.addTag("version", version)
    sentry.addTag("platform", platform)
    sentry.addTag("server_instance", serverInstance)
  }

  override def postStop(): Unit = {
    sentry.closeConnection()
  }

  def receive: Receive = {
    case InitializeLogger(_) => sender() ! LoggerInitialized
    case Error(cause, logSource, _, _) => sendException(cause, logSource)
    case Warning(logSource, _, message) => sendLog(logSource, message, Level.WARNING)
    case Info(logSource, _, message) => sendLog(logSource, message, Level.INFO)
    case Debug(logSource, _, message) => sendLog(logSource, message, Level.DEBUG)
  }

  private def sendException(cause: Throwable, logSource: String): Unit = {
    sentry.addTag("logger", logSource)
    sentry.sendException(cause)
  }

  private def sendLog(logger: String, message: Any, level: Level): Unit = {
    sentry.addTag("server_instance", serverInstance)
    sentry.sendEvent(new EventBuilder()
      .withLevel(level)
      .withLogger(logger)
      .withMessage(message.toString)
      .withTag("environment", environment)
      .withTag("platform", platform)
      .withTag("version", version)
      .withTag("server_instance", serverInstance)
      .build())
  }

}
