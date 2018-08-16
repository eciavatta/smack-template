package smack.database

import java.util.UUID

import akka.Done
import akka.actor.ActorSystem
import akka.event.Logging
import com.datastax.driver.core.utils.UUIDs
import com.datastax.driver.core.{Cluster, Session}
import com.typesafe.config.Config
import smack.common.utils.Helpers

import scala.collection.JavaConverters._
import scala.util.Try

class MigrationController(system: ActorSystem) {

  private val smackConfig: Config = system.settings.config.getConfig("smack")
  private val migrationConfig: Config = smackConfig.getConfig(s"database.migrations.${Helpers.getEnvironment(smackConfig)}")
  private val keyspaceName = migrationConfig.getString("keyspaceName")
  private val log = Logging(system, "MigrationController")

  def migrate(force: Boolean, createKeyspace: Boolean = false): Try[Done] = Try {
    checkForceInProduction(force)

    if (createKeyspace) {
      val session = createCluster().connect()
      session.execute(createKeyspaceQuery)
      session.close()
    }

    val session = createCluster().connect(keyspaceName)
    session.execute(createMigrationTableQuery)
    var migrationRecords = retrieveMigrationRecords(session)
    var migrationSeq = MigrateSequence.list
    val batch = if (migrationRecords.isEmpty) 0 else migrationRecords.reverse.head._3

    if (migrationRecords.size > migrationSeq.size) throw new IllegalStateException("The migration table is corrupted: some migrations are deleted")
    while (migrationRecords.nonEmpty) {
      if (migrationRecords.head._2 != migrationSeq.head.tag) throw new IllegalStateException("The migration table is corrupted: some migrations are changed")
      migrationRecords = migrationRecords.tail
      migrationSeq = migrationSeq.tail
    }

    migrationSeq.foreach { m =>
      log.info(s"Executing migration for [${m.tag}]..")
      session.execute(
        s"""
           |BEGIN BATCH
           |  ${m.up}
           |  INSERT INTO migrations(id, tag, batch) VALUES(${UUIDs.timeBased()}, ${m.tag}, $batch)
           |APPLY BATCH;
         """.stripMargin
      )
      log.info(s"Migration for [${m.tag}] executed successfully.")
    }

    Done
  }

  def rollback(force: Boolean, step: Option[Int] = None): Try[Done] = Try {
    checkForceInProduction(force)
    val session = createCluster().connect(keyspaceName)
    var migrationRecords = retrieveMigrationRecords(session).reverse
    var migrationSeq = MigrateSequence.list.reverse
    var stepValue = step.getOrElse(1)

    if (migrationRecords.isEmpty) throw new IllegalStateException("Cannot rollback: the migrations table is empty")
    migrationSeq = migrationSeq.dropWhile(_.tag != migrationRecords.head._1)
    if (migrationRecords.size != migrationSeq.size) throw new IllegalStateException("The migration table is corrupted: some migrations are deleted")
    var currentBatch = migrationRecords.head._3
    while (migrationRecords.nonEmpty && stepValue > 0) {
      val record = migrationRecords.head
      if (currentBatch != record._3) {
        stepValue -= 1
        currentBatch = record._3
      }
      if (record._2 != migrationSeq.head.tag) throw new IllegalStateException("The migration table is corrupted: some migrations are changed")

      log.info(s"Executing rollback for [${record._2}]..")
      session.execute(
        s"""
           |BEGIN BATCH
           |  ${migrationSeq.head.down}
           |  DELETE FROM migrations WHERE id="${record._1.toString}"
           |APPLY BATCH;
       """.stripMargin
      )
      log.info(s"Rollback for [${record._2}] executed successfully.")
      migrationRecords = migrationRecords.tail
      migrationSeq = migrationSeq.tail
    }

    Done
  }

  def reset(force: Boolean): Try[Done] = rollback(force, Some(Int.MaxValue))

  private def checkForceInProduction(force: Boolean): Unit =
    if (Helpers.isProductionEnvironment(smackConfig) && !force) throw new IllegalStateException("Run with force flag in production environment")

  private def retrieveMigrationRecords(session: Session): Seq[(UUID, String, Int)] = session.execute("SELECT toTimestamp(id), tag, batch FROM migrations")
    .all().asScala
    .sortBy(_.getTimestamp("id"))
    .map(r => (r.getUUID("id"), r.getString("tag"), r.getInt("batch")))
    .toList

  private def createCluster(): Cluster = Cluster.builder
    .addContactPoint(smackConfig.getString("cassandra.contact-point.host"))
    .withPort(smackConfig.getInt("cassandra.contact-point.port"))
    .build

  private val createKeyspaceQuery: String =
    s"""
       |CREATE KEYSPACE $keyspaceName WITH replication = {
       |  'class': '${migrationConfig.getString("keyspaceClass")}',
       |  'replication_factor': '${migrationConfig.getInt("keyspaceReplicationFactor")}'
       |};
    """.stripMargin

  private val createMigrationTableQuery: String =
    s"""
       |CREATE TABLE IF NOT EXISTS migrations (
       |  id TIMEUUID PRIMARY KEY,
       |  tag TEXT,
       |  batch INT);
     """.stripMargin

}
