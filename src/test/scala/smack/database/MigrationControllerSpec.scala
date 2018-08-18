package smack.database

import akka.actor.ActorSystem
import akka.testkit.TestKitBase
import com.typesafe.config.{Config, ConfigFactory}
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}
import smack.commons.utils.DatabaseUtils

import scala.collection.JavaConverters._
import scala.util.{Failure, Success}

class MigrationControllerSpec extends TestKitBase with WordSpecLike with BeforeAndAfterAll with Matchers {

  lazy implicit val system: ActorSystem = ActorSystem("migrationControllerSpec", config)
  lazy implicit val config: Config = ConfigFactory.load("test")

  override def afterAll(): Unit = {
    DatabaseUtils.dropTestKeyspace()
    shutdown()
  }

  "migration controller" should {

    "perform a full migration when no keyspace exists" in {
      val migrationController = MigrationController.createMigrationController(system, MigrationControllerSpec.getOneElemMigrationSeq)
      migrationController.migrate(createKeyspace = true) shouldBe Success(1)
      getAllTables should contain theSameElementsAs List("migrations", "migration1")
    }

    "perform a partial migration" in {
      val migrationController = MigrationController.createMigrationController(system, MigrationControllerSpec.getThreeElemsMigrationSeq)
      migrationController.migrate() shouldBe Success(2)
      getAllTables should contain theSameElementsAs List("migrations", "migration1", "migration2", "migration3")
    }

    "fail if no migrations are present or some migrations are deleted" in {
      val migrationController1 = MigrationController.createMigrationController(system, MigrationControllerSpec.getThreeElemsMigrationSeq)
      migrationController1.migrate() shouldBe Failure(_: IllegalStateException)

      val migrationController2 = MigrationController.createMigrationController(system, MigrationControllerSpec.getTwoElemsMigrationSeq)
      migrationController2.migrate() shouldBe Failure(_: IllegalStateException)

      getAllTables should contain theSameElementsAs List("migrations", "migration1", "migration2", "migration3")
    }

    "rollback a single step" in {
      val migrationController1 = MigrationController.createMigrationController(system, MigrationControllerSpec.getThreeElemsMigrationSeq)
      migrationController1.rollback() shouldBe Success(2)
      getAllTables should contain theSameElementsAs List("migrations", "migration1")

      val migrationController2 = MigrationController.createMigrationController(system, Seq())
      migrationController2.rollback() shouldBe Failure(_: IllegalStateException)
    }

    "rollback multiple steps" in {
      reMigrate()
      val migrationController = MigrationController.createMigrationController(system, MigrationControllerSpec.getThreeElemsMigrationSeq)
      migrationController.rollback(force = false, 2) shouldBe Success(2)
      getAllTables should contain theSameElementsAs List("migrations", "migration1")
    }

    "reset migrations" in {
      reMigrate()
      val migrationController = MigrationController.createMigrationController(system, MigrationControllerSpec.getThreeElemsMigrationSeq)
      migrationController.reset() shouldBe Success(3)
      getAllTables should contain theSameElementsAs List("migrations")
    }

  }

  private def reMigrate(): Unit = {
    val migrationController1 = MigrationController.createMigrationController(system, MigrationControllerSpec.getTwoElemsMigrationSeq)
    migrationController1.migrate() shouldBe Success(1)
    val migrationController2 = MigrationController.createMigrationController(system, MigrationControllerSpec.getThreeElemsMigrationSeq)
    migrationController2.migrate() shouldBe Success(1)
  }

  private def getAllTables: List[String] =
    DatabaseUtils.createCluster().getMetadata
      .getKeyspace(DatabaseUtils.getTestKeyspaceName)
      .getTables.asScala
      .map(_.getName).toList
}

object MigrationControllerSpec {

  def getOneElemMigrationSeq: Seq[Migration] = Seq(Migration1)
  def getTwoElemsMigrationSeq: Seq[Migration] = Seq(Migration1, Migration2)
  def getThreeElemsMigrationSeq: Seq[Migration] = Seq(Migration1, Migration2, Migration3)

  object Migration1 extends Migration {
    override def up: String = "CREATE TABLE migration1 (id INT PRIMARY KEY);"
    override def down: String = "DROP TABLE migration1;"
    override def tag: String = "migration1"
  }

  object Migration2 extends Migration {
    override def up: String = "CREATE TABLE migration2 (id INT PRIMARY KEY);"
    override def down: String = "DROP TABLE migration2;"
    override def tag: String = "migration2"
  }

  object Migration3 extends Migration {
    override def up: String = "CREATE TABLE migration3 (id INT PRIMARY KEY);"
    override def down: String = "DROP TABLE migration3;"
    override def tag: String = "migration3"
  }

}
