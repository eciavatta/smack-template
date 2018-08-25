package smack.commons.utils

import akka.actor.ActorContext
import com.typesafe.config.Config

import scala.util.Try

object Helpers {

  def getEnvironment(implicit config: Config): String = root(config).getString("smack.environment")

  def isProductionEnvironment(implicit config: Config): Boolean = getEnvironment(config) == "production"

  def isDevelopmentEnvironment(implicit config: Config): Boolean = getEnvironment(config) == "development"

  def isTestingEnvironment(implicit config: Config): Boolean = getEnvironment(config) == "testing"

  def isDebugEnabled(implicit config: Config): Boolean = root(config).getBoolean("smack.debug")

  def getApplicationName(implicit config: Config): String = root(config).getString("smack.name")

  def getDateFormat(implicit config: Config): String = root(config).getString("smack.dateFormat")

  def actorConfig(implicit context: ActorContext): Config = config(context)

  def getLanguage(implicit config: Config): String = root(config).getString("smack.language")

  def getString(key: String)(implicit config: Config): Option[String] = Try(root(config).getString(s"strings.$getLanguage.$key")).fold(_ => None, Some(_))

  def getError(key: String)(implicit config: Config): Option[String] = getString(s"errors.$key")

  private def config(context: ActorContext): Config = context.system.settings.config

  private def root(config: Config): Config = config.root.toConfig

}
