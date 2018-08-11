package smack.kafka

import java.nio.ByteBuffer

import akka.Done
import akka.actor.ActorSystem
import akka.serialization.{Serialization, SerializationExtension}
import akka.testkit.{DefaultTimeout, ImplicitSender, TestKitBase}
import com.typesafe.config.{Config, ConfigFactory}
import net.manub.embeddedkafka.{EmbeddedKafka, EmbeddedKafkaConfig}
import org.apache.kafka.common.serialization.{ByteBufferDeserializer, Deserializer, StringDeserializer}
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}
import smack.models.SerializationException
import smack.models.messages.{GenerateException, TestRequest}

import scala.util.{Failure, Success}

class KafkaProducerSpec extends TestKitBase with EmbeddedKafka with WordSpecLike with Matchers with BeforeAndAfterAll
  with ImplicitSender with DefaultTimeout {

  implicit lazy val system: ActorSystem = ActorSystem("kafkaProducerSpec", actorConfig)
  lazy val actorConfig: Config = ConfigFactory.parseString(
    """
    akka.loglevel = "INFO"
    akka.kafka.producer.buffer-size = 10
  """).withFallback(ConfigFactory.load("serialization"))

  val kafkaTopic = "test"
  val messageValue = "testingMessage"
  val testingPort = 6001

  implicit lazy val keyDeserializer: Deserializer[String] = new StringDeserializer
  implicit lazy val valueDeserializer: Deserializer[ByteBuffer] = new ByteBufferDeserializer
  implicit lazy val serialization: Serialization = SerializationExtension(system)
  implicit val kafkaConfig: EmbeddedKafkaConfig = EmbeddedKafkaConfig(zooKeeperPort = 0, kafkaPort = testingPort)

  "kafka producer actor" should {

    "serialize messages correctly" in {
      val serializableMessage = TestRequest(messageValue)
      val unSerializableMessage = NonSerializableMessage(messageValue)

      val serializationResult = ProtobufSerialization.serializeMessage(serializableMessage)
      serializationResult.isSuccess shouldBe true
      // ProtobufSerialization.deserializeMessage(TestRequest.getClass.getName, serializationResult.get) shouldBe Success(serializableMessage)

      val unSerializationResult = ProtobufSerialization.serializeMessage(unSerializableMessage)
      unSerializationResult.isSuccess shouldBe false
    }

    "report a serializable message" in {
      val kafkaProducerRef = system.actorOf(KafkaProducer.props("test1", testingPort), "test1")
      val successMessage = TestRequest(messageValue)

      kafkaProducerRef ! successMessage
      expectMsg(Success(Done))

      val (key, value) = consumeFirstKeyedMessageFrom[String, ByteBuffer]("test1")
      ProtobufSerialization.deserializeMessage(key, value) shouldBe Success(successMessage)
    }

    "fail if message is not serializable" in {
      val kafkaProducerRef = system.actorOf(KafkaProducer.props("test2", testingPort), "test2")
      val unSerializableMessage = NonSerializableMessage(messageValue)

      kafkaProducerRef ! unSerializableMessage
      expectMsg(Failure(SerializationException))
    }

    "restart when exception occurs" in {
      val kafkaProducerRef = system.actorOf(KafkaProducer.props("test3", testingPort), "test3")
      val testMessage = TestRequest(messageValue)

      kafkaProducerRef ! testMessage
      expectMsg(Success(Done))
      kafkaProducerRef ! GenerateException("kafka producer exception test")
      kafkaProducerRef ! testMessage
      expectMsg(Success(Done))

      for (_ <- 1 to 2) {
        val (key, value) = consumeFirstKeyedMessageFrom[String, ByteBuffer]("test3")
        ProtobufSerialization.deserializeMessage(key, value) shouldBe Success(testMessage)
      }
    }

  }

  protected override def beforeAll(): Unit = {
    EmbeddedKafka.start()
  }

  protected override def afterAll(): Unit = {
    EmbeddedKafka.stop()
    shutdown()
  }

  case class NonSerializableMessage(value: String)

}
