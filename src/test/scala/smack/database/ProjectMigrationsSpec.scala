package smack.database

import akka.actor.ActorSystem
import akka.testkit.TestKitBase
import com.datastax.driver.core.Cluster
import com.typesafe.config.{Config, ConfigFactory}
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

import scala.util.Success

class ProjectMigrationsSpec extends TestKitBase with WordSpecLike with BeforeAndAfterAll with Matchers {

  implicit lazy val system: ActorSystem = ActorSystem("projectMigrationsSpec", config)
  lazy val config: Config = ConfigFactory.load("test")
  val keyspaceName: String = config.getString("smack.database.migrations.testing.keyspaceName")

  override def afterAll(): Unit = {
    Cluster.builder
      .addContactPoint(config.getString("smack.cassandra.contact-point.host"))
      .withPort(config.getInt("smack.cassandra.contact-point.port"))
      .build.newSession().execute(s"DROP KEYSPACE IF EXISTS $keyspaceName;")
    shutdown()
  }

  "migration controller" should {

    "perform project migration correctly" in {
      val migrationController = MigrationController.createMigrationController(system)
      migrationController.migrate(force = false, createKeyspace = true) shouldBe Success(MigrateSequence.seq.size)
    }

    "reset project migrations" in {
      val migrationController = MigrationController.createMigrationController(system)
      migrationController.reset(force = false) shouldBe Success(MigrateSequence.seq.size)
    }

  }

}
