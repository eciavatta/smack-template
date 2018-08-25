package smack.database

import akka.actor.ActorSystem

import scala.util.Try

trait MigrationController {

  def migrate(force: Boolean = false, createKeyspace: Boolean = false): Try[Int]
  def rollback(force: Boolean = false, steps: Int = 1): Try[Int]
  def reset(force: Boolean = false): Try[Int]

}

object MigrationController {

  def createMigrationController(system: ActorSystem): MigrationController = new MigrationControllerImpl(system, MigrateSequence.seq)

  def createMigrationController(system: ActorSystem, migrationSeq: Seq[Migration]): MigrationController = new MigrationControllerImpl(system, migrationSeq)

}
