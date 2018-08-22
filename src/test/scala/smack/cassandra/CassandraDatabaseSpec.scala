package smack.cassandra

import akka.actor.ActorSystem
import akka.pattern.ask
import akka.testkit.{DefaultTimeout, TestKitBase}
import com.datastax.driver.core.{ResultSet, Session}
import com.typesafe.config.{Config, ConfigFactory}
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}
import smack.cassandra.CassandraDatabase.{CassandraQuery, CassandraQueryMap, CassandraResult}
import smack.commons.utils.DatabaseUtils
import smack.models.messages.GenerateException

import scala.collection.JavaConverters._
import scala.collection.immutable
import scala.concurrent.Await
import scala.concurrent.duration._
import scala.util.Try

class CassandraDatabaseSpec extends TestKitBase with WordSpecLike with BeforeAndAfterAll with Matchers with DefaultTimeout {

  implicit lazy val config: Config = ConfigFactory.load("test")
  lazy val system: ActorSystem = ActorSystem("cassandraDatabaseSpec", config)

  lazy val session: Session = DatabaseUtils.createTestSession()
  val keyspaceName: String = DatabaseUtils.getTestKeyspaceName

  override def beforeAll(): Unit = {
    DatabaseUtils.createTestKeyspace()
  }

  override def afterAll(): Unit = {
    DatabaseUtils.dropTestKeyspace()
    shutdown()
  }

  def createTable(tableName: String): ResultSet = session.execute(
    s"""
       |CREATE TABLE IF NOT EXISTS $tableName (
       |	id int,
       |	PRIMARY KEY (id)
       |);
      """.stripMargin
  )

  def populate(tableName: String): immutable.IndexedSeq[ResultSet] = (0 until 100) map { i =>
    session.execute(s"INSERT INTO $tableName(id) VALUES ($i)")
  }

  "Cassandra database actor" should {

    "return empty list when selecting empty table" in {
      val testName = "cassandra1"
      val cassandraDatabaseRef = system.actorOf(CassandraDatabase.props(keyspaceName), testName)
      createTable(testName)

      val future = cassandraDatabaseRef ? CassandraQuery(s"SELECT * FROM $testName")
      val CassandraResult(resultSet: Try[ResultSet]) = Await.result(future, 3.seconds)
      resultSet.get.all() shouldBe empty
    }

    "execute a query statement and return the correct results" in {
      val testName = "cassandra2"
      val cassandraDatabaseRef = system.actorOf(CassandraDatabase.props(keyspaceName), testName)
      createTable(testName)
      populate(testName)

      val future = cassandraDatabaseRef ? CassandraQuery(s"SELECT * FROM $testName")
      val CassandraResult(resultSet: Try[ResultSet]) = Await.result(future, 3.seconds)
      resultSet.get.all().asScala.map(_.getInt("id")) should contain theSameElementsAs (0 until 100).toList
    }

    "insert elements with map binding and check result with select with query binding" in {
      val testName = "cassandra3"
      val cassandraDatabaseRef = system.actorOf(CassandraDatabase.props(keyspaceName), testName)
      createTable(testName)

      val future = cassandraDatabaseRef ? CassandraQueryMap(s"INSERT INTO $testName(id) VALUES (?)", Map("id" -> 3))
      val CassandraResult(resultSet: Try[ResultSet]) = Await.result(future, 3.seconds)
      resultSet.isSuccess shouldBe true

      val future2 = cassandraDatabaseRef ? CassandraQuery(s"SELECT * FROM $testName WHERE id = ?", 3)
      val CassandraResult(resultSet2: Try[ResultSet]) = Await.result(future2, 3.seconds)
      resultSet2.get.one().getInt("id") shouldBe 3
    }

    "execute an invalid query and return failing result" in {
      val testName = "cassandra4"
      val cassandraDatabaseRef = system.actorOf(CassandraDatabase.props(keyspaceName), testName)
      createTable(testName)

      val future = cassandraDatabaseRef ? CassandraQuery("INVALID QUERY")
      val CassandraResult(resultSet: Try[ResultSet]) = Await.result(future, 3.seconds)
      resultSet.isSuccess shouldBe false
    }

    "restart if an exception occurs" in {
      val testName = "cassandra5"
      val cassandraDatabaseRef = system.actorOf(CassandraDatabase.props(keyspaceName), testName)
      createTable(testName)
      populate(testName)

      cassandraDatabaseRef ! GenerateException("cassandra exception test")
      val future = cassandraDatabaseRef ? CassandraQuery(s"SELECT * FROM $testName")

      val CassandraResult(resultSet: Try[ResultSet]) = Await.result(future, 3.seconds)
      resultSet.get.all().asScala.map(_.getInt("id")) should contain theSameElementsAs (0 until 100).toList
    }
  }

}
