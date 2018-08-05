package smack

import akka.actor.Actor
import akka.event.Logging._
import io.sentry.Sentry
import io.sentry.event.Event.Level
import io.sentry.event.EventBuilder

class SentryLogger extends Actor {

  import System._

  private val config = context.system.settings.config
  private val release = s"${BuildInfo.name}-${BuildInfo.version}"
  private val environment = s"scalaVersion: ${BuildInfo.scalaVersion} - sbtVersion: ${BuildInfo.sbtVersion}"
  private val platform = s"${getProperty("os.name")}_${getProperty("os.version")} - ${getProperty("java.vendor")}_${getProperty("java.version")}"
  private val serverInstance = s"${config.getString("akka.remote.netty.tcp.hostname")}:${config.getString("akka.remote.netty.tcp.port")}"

  override def preStart(): Unit = {
    Sentry.init(config.getString("sentry.dns"))
  }

  override def postStop(): Unit = {
    Sentry.close()
  }

  def receive: Receive = {
    case InitializeLogger(_) => sender() ! LoggerInitialized
    case Error(cause, _, _, _) => Sentry.capture(cause)
    case Warning(logSource, _, message) => logMessage(logSource, message, Level.WARNING)
    case Info(logSource, _, message) => logMessage(logSource, message, Level.INFO)
    case Debug(logSource, _, message) => logMessage(logSource, message, Level.DEBUG)
  }

  private def logMessage(logger: String, message: Any, level: Level): Unit = {
    Sentry.capture(new EventBuilder()
      .withLevel(level)
      .withLogger(logger)
      .withMessage(message.toString)
      .withRelease(release)
      .withEnvironment(environment)
      .withPlatform(platform)
      .withServerName(serverInstance)
      .build())
  }
}
