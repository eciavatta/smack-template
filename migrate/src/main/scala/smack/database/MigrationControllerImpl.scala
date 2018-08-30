package smack.database

import java.util.UUID

import akka.actor.ActorSystem
import akka.event.Logging
import com.datastax.driver.core.{Cluster, Session}
import com.typesafe.config.Config
import smack.commons.utils.Helpers

import scala.collection.JavaConverters._
import scala.util.Try

private[database] class MigrationControllerImpl(system: ActorSystem, migrationSeq: Seq[Migration]) extends MigrationController {

  private val config: Config = system.settings.config
  private val migrationConfig: Config = config.getConfig(s"smack.database.migrations")
  private val keyspaceName = migrationConfig.getString("keyspaceName")
  private val log = Logging(system, "MigrationController")

  def migrate(force: Boolean, createKeyspace: Boolean = false): Try[Int] = Try {
    checkForceInProduction(force)

    if (createKeyspace) {
      val session = createCluster().connect()
      session.execute(createKeyspaceQuery)
      session.close()
    }

    val session = createCluster().connect(keyspaceName)
    session.execute(createMigrationTableQuery)
    var migrationRecords = retrieveMigrationRecords(session)
    var migrationList = migrationSeq.toList
    val batch = if (migrationRecords.isEmpty) 0 else migrationRecords.reverse.head._3 + 1

    if (migrationRecords.size > migrationList.size) throw new IllegalStateException("The migration table is corrupted: some migrations are deleted")
    while (migrationRecords.nonEmpty) {
      if (migrationRecords.head._2 != migrationList.head.tag) throw new IllegalStateException(s"The migration table is corrupted: some migrations are changed")
      migrationRecords = migrationRecords.tail
      migrationList = migrationList.tail
    }

    if (migrationList.isEmpty) throw new IllegalStateException("The database schema is already at the last version. No migration needed")
    migrationList.foreach { m =>
      log.info(s"Executing migration for [${m.tag}]..")
      session.execute(m.up)
      session.execute(s"INSERT INTO migrations(id, tag, batch_num) VALUES(now(), '${m.tag}', $batch);")
      log.info(s"Migration for [${m.tag}] executed successfully.")
    }

    session.close()
    migrationList.size
  }

  def rollback(force: Boolean, steps: Int = 1): Try[Int] = Try {
    checkForceInProduction(force)
    val session = createCluster().connect(keyspaceName)
    var migrationRecords = retrieveMigrationRecords(session).reverse
    var migrationList = migrationSeq.reverse.toList
    var stepValue = steps

    if (migrationRecords.isEmpty) throw new IllegalStateException("Cannot rollback: the migrations table is empty")
    migrationList = migrationList.dropWhile(_.tag != migrationRecords.head._2)
    if (migrationRecords.size != migrationList.size) throw new IllegalStateException("The migration table is corrupted: some migrations are deleted")
    var currentBatch = migrationRecords.head._3
    var migration = 0

    while (migrationRecords.nonEmpty && _checkStepsAndBatch) {
      val record = migrationRecords.head
      if (record._2 != migrationList.head.tag) throw new IllegalStateException("The migration table is corrupted: some migrations are changed")

      log.info(s"Executing rollback for [${record._2}]..")
      session.execute(migrationList.head.down)
      session.execute(s"DELETE FROM migrations WHERE id=${record._1.toString}")
      log.info(s"Rollback for [${record._2}] executed successfully.")
      migrationRecords = migrationRecords.tail
      migrationList = migrationList.tail
      migration += 1
    }

    def _checkStepsAndBatch: Boolean = {
      if (currentBatch != migrationRecords.head._3) {
        stepValue -= 1
        currentBatch = migrationRecords.head._3
      }
      stepValue > 0
    }

    session.close()
    migration
  }

  def reset(force: Boolean): Try[Int] = rollback(force,Int.MaxValue)

  private def checkForceInProduction(force: Boolean): Unit =
    if (Helpers.isProductionEnvironment(config) && !force) throw new IllegalStateException("Run with force flag in production environment")

  private def retrieveMigrationRecords(session: Session): Seq[(UUID, String, Int)] =
    session.execute("SELECT id, toTimestamp(id) as timeId, tag, batch_num FROM migrations")
      .all().asScala
      .sortBy(_.getTimestamp("timeId"))
      .map(r => (r.getUUID("id"), r.getString("tag"), r.getInt("batch_num")))
      .toList

  private def createCluster(): Cluster = Cluster.builder
    .addContactPointsWithPorts(Helpers.getCassandraContactsPoints(config))
    .build

  private val createKeyspaceQuery: String =
    s"""
       |CREATE KEYSPACE IF NOT EXISTS $keyspaceName WITH replication = {
       |  'class': '${migrationConfig.getString("keyspaceClass")}',
       |  'replication_factor': '${migrationConfig.getInt("keyspaceReplicationFactor")}'
       |};
    """.stripMargin

  private val createMigrationTableQuery: String =
    s"""
       |CREATE TABLE IF NOT EXISTS migrations (
       |  id TIMEUUID PRIMARY KEY,
       |  tag TEXT,
       |  batch_num INT );
     """.stripMargin

}
