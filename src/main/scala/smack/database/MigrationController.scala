package smack.database

import akka.actor.ActorSystem

import scala.util.Try

trait MigrationController {

  def migrate(force: Boolean, createKeyspace: Boolean = false): Try[Int]
  def rollback(force: Boolean, steps: Int = 1): Try[Int]
  def reset(force: Boolean): Try[Int]

}

object MigrationController {

  def createMigrationController(system: ActorSystem): MigrationController = new MigrationControllerImpl(system, MigrateSequence.seq)

  private[database] def createMigrationController(system: ActorSystem, migrationSeq: Seq[Migration]): MigrationController =
    new MigrationControllerImpl(system, migrationSeq)

}
