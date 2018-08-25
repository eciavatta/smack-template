package smack.multinode

import java.nio.ByteBuffer
import java.util.UUID

import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.marshalling.Marshal
import akka.http.scaladsl.model._
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.remote.testkit.MultiNodeSpec
import akka.serialization.{Serialization, SerializationExtension}
import akka.stream.ActorMaterializer
import akka.testkit.DefaultTimeout
import com.datastax.driver.core.{Session, SimpleStatement}
import com.fasterxml.uuid.Generators
import com.typesafe.config.Config
import net.manub.embeddedkafka.{EmbeddedKafka, EmbeddedKafkaConfig}
import org.apache.kafka.common.serialization._
import smack.backend.BackendSupervisor
import smack.commons.mashallers.Marshalling
import smack.commons.traits.STMultiNodeSpec
import smack.commons.utils.DatabaseUtils
import smack.commons.utils.SystemIntegrationConfig.SystemIntegrationMultiNodeConfig._
import smack.commons.utils.SystemIntegrationConfig._
import smack.database.MigrationController
import smack.database.migrations.{CreateLogsTable, CreateUsersByCredentialsTable, CreateUsersByIdTable}
import smack.frontend.server.WebServer
import smack.kafka.ProtobufSerialization
import smack.models.Events.UserCreating
import smack.models.messages.TraceLogRequest
import smack.models.structures.User

import scala.collection.JavaConverters._
import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.util.Try

class SystemIntegrationSpecMultiJvmFrontend extends MultiNodeSpec(SystemIntegrationMultiNodeConfig, createActorSystem("frontend"))
  with STMultiNodeSpec with DefaultTimeout with Marshalling with EmbeddedKafka {

  implicit val config: Config = actorConfig("frontend")
  implicit val ec: ExecutionContext = system.dispatcher
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  val migrationController: MigrationController = MigrationController.createMigrationController(system,
    Seq(CreateUsersByIdTable, CreateUsersByCredentialsTable, CreateLogsTable))

  val zookeeperPort = 2181
  val kafkaPort = 9092
  val quantity = 20
  val waitTime = 500

  lazy val cassandraSession: Session = DatabaseUtils.createTestSession()

  implicit val kafkaConfig: EmbeddedKafkaConfig = EmbeddedKafkaConfig(kafkaPort, zookeeperPort)
  implicit val keySerializer: Serializer[String] = new StringSerializer
  implicit val valueSerializer: Serializer[ByteBuffer] = new ByteBufferSerializer
  implicit val keyDeserializer: Deserializer[String] = new StringDeserializer
  implicit val valueDeserializer: Deserializer[ByteBuffer] = new ByteBufferDeserializer
  implicit val serialization: Serialization = SerializationExtension(system)

  def initialParticipants: Int = roles.size

  override def beforeAll(): Unit = {
    super.beforeAll()
    migrationController.migrate(createKeyspace = true)
  }

  override def afterAll(): Unit = {
    super.afterAll()
    Try {
      consumeNumberMessagesFromTopics[ByteBuffer](Set("logs"), number = Int.MaxValue, autoCommit = true, 500.millis, resetTimeoutOnEachMessage = false)
    }
    DatabaseUtils.dropTestKeyspace()
  }

  "System integration test" should {

    "wait for all nodes to enter a barrier" in {
      enterBarrier("startup")
      enterBarrier("ready")

      val backendPath = node(backend) / "user" / BackendSupervisor.name
      val backendRef = Await.result(system.actorSelection(backendPath).resolveOne(), 1.second)
      WebServer.create(system, backendRef).start()
    }

    "execute an integration test that use both systems" in {
      val futureTest: Future[Unit] = Marshal(UserCreating("valid@example.com", "foobar", "testUser")).to[RequestEntity] flatMap { entity =>
        Http().singleRequest(HttpRequest(uri = "http://127.0.0.1:8080/users", method = HttpMethods.POST, entity = entity))
      } flatMap { createResponse =>
        createResponse.status shouldEqual StatusCodes.OK
        Unmarshal(createResponse.entity).to[User]
      } flatMap { user =>
        Http().singleRequest(HttpRequest(uri = s"http://127.0.0.1:8080/users/${user.id}", method = HttpMethods.GET))
      } map { getResponse =>
        getResponse.status shouldEqual StatusCodes.OK
      }

      Await.ready(futureTest, 10.seconds)
    }

    "consume log message from kafka partition and save in database" in {
      val url = "example.com"
      val ipAddress = "127.0.0.1"
      val userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/68.0.3440.106 Safari/537.36"

      val siteId: UUID = Generators.timeBasedGenerator().generate()
      val log = TraceLogRequest(siteId.toString, url, ipAddress, userAgent)
      val logSerialized = ProtobufSerialization.serializeMessage(log)
      logSerialized.isSuccess shouldBe true

      for (_ <- 1 to quantity) {
        publishToKafka("logs", log.getClass.getName, logSerialized.get)
      }

      Thread.sleep(waitTime)

      val results = cassandraSession.execute(new SimpleStatement("SELECT * FROM logs WHERE site_id = ?;", siteId)).all().asScala
      results.size shouldBe quantity

      results.map(row => TraceLogRequest(row.getUUID("site_id").toString, row.getString("url"), row.getString("ip_address"),
        row.getString("user_agent"))).toList shouldEqual List.fill(quantity)(log)
    }

    "wait the completion and terminate the test" in {
      enterBarrier("finished")
    }

  }
}
