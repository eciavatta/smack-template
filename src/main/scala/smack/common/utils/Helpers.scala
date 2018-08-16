package smack.common.utils

import akka.actor.{ActorContext, ActorRef}
import akka.routing.RoundRobinGroup
import com.typesafe.config.Config
import smack.cassandra.CassandraDatabase
import smack.kafka.KafkaProducer

object Helpers {

  def getEnvironment(implicit config: Config): String = root(config).getString("smack.environment")
  def getEnvironment(implicit context: ActorContext): String = getEnvironment(config(context))

  def isProductionEnvironment(implicit config: Config): Boolean = getEnvironment(config) == "production"
  def isProductionEnvironment(implicit context: ActorContext): Boolean = isProductionEnvironment(config(context))

  def isDevelopmentEnvironment(implicit config: Config): Boolean = !isProductionEnvironment(config)
  def isDevelopmentEnvironment(implicit context: ActorContext): Boolean = isDevelopmentEnvironment(config(context))

  def isDebugEnabled(implicit config: Config): Boolean = root(config).getBoolean("smack.debug")
  def isDebugEnabled(implicit context: ActorContext): Boolean = isDebugEnabled(config(context))

  def getApplicationName(implicit config: Config): String = root(config).getString("smack.name")
  def getApplicationName(implicit context: ActorContext): String = getApplicationName(config(context))

  def getDateFormat(implicit config: Config): String = root(config).getString("smack.dateFormat")
  def getDateFormat(implicit context: ActorContext): String = getDateFormat(config(context))

  def actorConfig(implicit context: ActorContext): Config = config(context)

  def createKafkaProducerPool(topic: String)(implicit context: ActorContext): ActorRef = {
    val paths = (1 to config(context).getInt(s"smack.entities.$topic.kafka-partitions")) map { i =>
      context.actorOf(KafkaProducer.props(topic, i), KafkaProducer.name(topic, i)).path.toStringWithoutAddress
    }

    context.actorOf(RoundRobinGroup(paths).props(), s"${topic}KafkaProducerRouter")
  }

  def createCassandraDatabaseActor()(implicit context: ActorContext): ActorRef = {
    val keyspace = config(context).getString(s"smack.database.migrations.${getEnvironment(context)}.keyspaceName")
    context.actorOf(CassandraDatabase.props(keyspace), CassandraDatabase.name(keyspace))
  }

  private def config(context: ActorContext): Config = context.system.settings.config
  private def root(config: Config): Config = config.root.toConfig

}
