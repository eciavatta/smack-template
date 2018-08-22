package smack.common.utils

import akka.actor.{ActorContext, ActorRef}
import akka.routing.RoundRobinGroup
import com.typesafe.config.Config
import smack.cassandra.CassandraDatabase
import smack.kafka.KafkaProducer

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

  def createKafkaProducerPool(topic: String)(implicit context: ActorContext): ActorRef = {
    val paths = (0 until config(context).getInt(s"smack.topics.$topic.kafka-partitions")) map { i =>
      context.actorOf(KafkaProducer.props(topic, i), KafkaProducer.name(topic, i)).path.toStringWithoutAddress
    }

    context.actorOf(RoundRobinGroup(paths).props(), s"${topic}KafkaProducerRouter")
  }

  def getLanguage(implicit config: Config): String = root(config).getString("smack.language")

  def getString(key: String)(implicit config: Config): Option[String] = Try(root(config).getString(s"strings.$getLanguage.$key")).fold(_ => None, Some(_))

  def getError(key: String)(implicit config: Config): Option[String] = getString(s"errors.$key")

  def createCassandraDatabaseActor()(implicit context: ActorContext): ActorRef = {
    val keyspace = config(context).getString(s"smack.database.migrations.keyspaceName")
    context.actorOf(CassandraDatabase.props(keyspace), CassandraDatabase.name(keyspace))
  }

  private def config(context: ActorContext): Config = context.system.settings.config
  private def root(config: Config): Config = config.root.toConfig

}
