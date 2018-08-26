package smack.database

import akka.actor.ActorSystem
import akka.testkit.TestKitBase
import com.typesafe.config.{Config, ConfigFactory}
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}
import smack.commons.utils.DatabaseUtils

import scala.util.Success

class ProjectMigrationsSpec extends TestKitBase with WordSpecLike with BeforeAndAfterAll with Matchers {

  implicit lazy val config: Config = ConfigFactory.load("commons-testing")
  lazy val system: ActorSystem = ActorSystem("projectMigrationsSpec", config)

  override def afterAll(): Unit = {
    DatabaseUtils.dropTestKeyspace()
    shutdown()
  }

  "migration controller" should {

    "perform project migration correctly" in {
      val migrationController = MigrationController.createMigrationController(system)
      migrationController.migrate(createKeyspace = true) shouldEqual Success(MigrateSequence.seq.size)
    }

    "reset project migrations" in {
      val migrationController = MigrationController.createMigrationController(system)
      migrationController.reset() shouldEqual Success(MigrateSequence.seq.size)
    }

  }

}
