package smack.controllers

import java.nio.ByteBuffer
import java.util.UUID

import akka.actor.{ActorRef, ActorSystem}
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.serialization.{Serialization, SerializationExtension}
import akka.testkit.TestKitBase
import com.datastax.driver.core.{Session, SimpleStatement}
import com.fasterxml.uuid.Generators
import com.typesafe.config.{Config, ConfigFactory}
import net.manub.embeddedkafka.{EmbeddedKafka, EmbeddedKafkaConfig}
import org.apache.kafka.common.serialization.{ByteBufferDeserializer, Deserializer, StringDeserializer}
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, Matchers, WordSpec}
import smack.backend.controllers.LogController
import smack.commons.mashallers.Marshalling
import smack.commons.utils.{DatabaseUtils, Helpers}
import smack.database.MigrationController
import smack.database.migrations.CreateSitesByTrackingIdTable
import smack.frontend.routes.LogRoute
import smack.kafka.ProtobufSerialization
import smack.models.Events.LogEvent
import smack.models.messages.TraceLogRequest

import scala.concurrent.duration._
import scala.util.{Success, Try}

class LogControllerSpec extends WordSpec with ScalatestRouteTest with TestKitBase
  with BeforeAndAfterEach with BeforeAndAfterAll with Matchers with Marshalling with EmbeddedKafka {

  implicit lazy val config: Config = ConfigFactory.load("test")
  val migrationController: MigrationController = MigrationController.createMigrationController(system, Seq(CreateSitesByTrackingIdTable))

  val topic = "logs"
  val zookeeperPort = 2181
  val kafkaPort = 9092

  lazy val logController: ActorRef = system.actorOf(LogController.props, LogController.name)
  lazy val logRoute: Route = LogRoute(logController).route
  lazy val cassandraSession: Session = DatabaseUtils.createTestSession()

  val trackingId: UUID = Generators.randomBasedGenerator().generate()
  val siteId: UUID = Generators.timeBasedGenerator().generate()

  implicit val kafkaConfig: EmbeddedKafkaConfig = EmbeddedKafkaConfig(kafkaPort, zookeeperPort)
  implicit val keyDeserializer: Deserializer[String] = new StringDeserializer
  implicit val valueDeserializer: Deserializer[ByteBuffer] = new ByteBufferDeserializer
  implicit val serialization: Serialization = SerializationExtension(system)

  override def createActorSystem(): ActorSystem = ActorSystem("logControllerSpec", config)

  override def beforeAll(): Unit = {
    migrationController.migrate(createKeyspace = true)
    cassandraSession.execute(new SimpleStatement("INSERT INTO sites_by_tracking_id (tracking_id, site_id) VALUES (?, ?)", trackingId, siteId))
  }

  override def afterEach(): Unit = {
    Try {
      // clean topic
      consumeNumberMessagesFromTopics[ByteBuffer](Set(topic), number = Int.MaxValue, autoCommit = true, 500.millis, resetTimeoutOnEachMessage = false)
    }
  }

  protected override def afterAll(): Unit = {
    DatabaseUtils.dropTestKeyspace()
    shutdown()
  }

  "log request controller" should {

    "fail when tracking invalid or not existing site" in {
      val url = "example.com"
      val ipAddress = "127.0.0.1"
      val userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/68.0.3440.106 Safari/537.36"

      Post("/logs/malformedUUID", LogEvent(url, ipAddress, userAgent)) ~> logRoute ~> check {
        status shouldEqual StatusCodes.BadRequest
        responseAs[String] shouldEqual Helpers.getError("badUUID").get
      }

      Post(s"/logs/${Generators.randomBasedGenerator().generate().toString}", LogEvent(url, ipAddress, userAgent)) ~> logRoute ~> check {
        status shouldEqual StatusCodes.NotFound
      }
    }

    "trace correct event log" in {
      val url = "example.com"
      val ipAddress = "127.0.0.1"
      val userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/68.0.3440.106 Safari/537.36"

      for (_ <- 1 to 2) {
        Post(s"/logs/${trackingId.toString}", LogEvent(url, ipAddress, userAgent))~> logRoute ~> check {
          status shouldEqual StatusCodes.Accepted
        }

        val (key, value) = consumeFirstKeyedMessageFrom[String, ByteBuffer](topic)
        ProtobufSerialization.deserializeMessage(key, value) shouldEqual Success(TraceLogRequest(siteId.toString, url, ipAddress, userAgent))
      }
    }

  }

}
