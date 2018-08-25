package smack.commons.utils

import com.datastax.driver.core.{Cluster, Session}
import com.typesafe.config.Config

object DatabaseUtils {

  def getTestKeyspaceName(implicit config: Config): String = config.getString("smack.database.migrations.keyspaceName")

  def createCluster()(implicit config: Config): Cluster = Cluster.builder
    .addContactPoint(config.getString("smack.cassandra.contact-point.host"))
    .withPort(config.getInt("smack.cassandra.contact-point.port"))
    .build()

  def createEmptySession()(implicit config: Config): Session = createCluster().connect()
  def createTestSession()(implicit config: Config): Session = createCluster().connect(getTestKeyspaceName)
  def createCustomSession(keyspace: String)(implicit config: Config): Session = createCluster().connect(keyspace)

  def createTestKeyspace()(implicit config: Config): Unit = createEmptySession().execute(
    s"""
       |CREATE KEYSPACE $getTestKeyspaceName WITH replication = {
       |  'class': 'SimpleStrategy',
       |  'replication_factor': '1'
       |};
      """.stripMargin
  )

  def dropTestKeyspace()(implicit config: Config): Unit = createEmptySession().execute(s"DROP KEYSPACE IF EXISTS $getTestKeyspaceName;")

}
