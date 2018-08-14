package smack.kafka

import java.nio.ByteBuffer

import akka.Done
import akka.actor.ActorSystem
import akka.serialization.{Serialization, SerializationExtension}
import akka.testkit.{DefaultTimeout, ImplicitSender, TestKitBase, TestProbe}
import com.typesafe.config.{Config, ConfigFactory}
import net.manub.embeddedkafka.{EmbeddedKafka, EmbeddedKafkaConfig}
import org.apache.kafka.common.serialization._
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}
import smack.common.utils.TestKitUtils
import smack.models.messages.{GenerateException, TestRequest}
import smack.models.{SerializationException, TestException}

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.util.{Failure, Success, Try}

class KafkaProducerConsumerSpec extends TestKitBase with EmbeddedKafka
  with WordSpecLike with Matchers with BeforeAndAfterAll with ImplicitSender with DefaultTimeout {

  implicit lazy val system: ActorSystem = ActorSystem("kafkaProducerConsumerSpec", actorConfig)
  lazy val actorConfig: Config = TestKitUtils.config.withFallback(ConfigFactory.load("kafka-producer")
    .withFallback(ConfigFactory.load("kafka-consumer")))

  val messageValue = "testingMessage"
  val testingPort = 6001
  val consumerGroup = "group1"

  implicit lazy val keySerializer: Serializer[String] = new StringSerializer
  implicit lazy val valueSerializer: Serializer[ByteBuffer] = new ByteBufferSerializer
  implicit lazy val keyDeserializer: Deserializer[String] = new StringDeserializer
  implicit lazy val valueDeserializer: Deserializer[ByteBuffer] = new ByteBufferDeserializer
  implicit lazy val serialization: Serialization = SerializationExtension(system)
  implicit val kafkaConfig: EmbeddedKafkaConfig = EmbeddedKafkaConfig(zooKeeperPort = 0, kafkaPort = testingPort)

  protected override def beforeAll(): Unit = {
    EmbeddedKafka.start()
  }

  protected override def afterAll(): Unit = {
    EmbeddedKafka.stop()
    Await.result(system.terminate(), 15.seconds)
  }

  "kafka producer actor" should {

    "serialize messages correctly" in {
      val serializableMessage = TestRequest(messageValue)
      val unSerializableMessage = NonSerializableMessage(messageValue)

      val serializationResult = ProtobufSerialization.serializeMessage(serializableMessage)
      serializationResult.isSuccess shouldBe true

      ProtobufSerialization.deserializeMessage(serializableMessage.getClass.getName,
        ByteBuffer.wrap(serializationResult.get.array())) shouldBe Success(serializableMessage)

      val unSerializationResult = ProtobufSerialization.serializeMessage(unSerializableMessage)
      unSerializationResult.isSuccess shouldBe false
    }

    "report a serializable message" in {
      val kafkaProducerRef = system.actorOf(KafkaProducer.props("producer1", testingPort), "producer1")
      val successMessage = TestRequest(messageValue)

      kafkaProducerRef ! successMessage
      expectMsg(Success(Done))

      val (key, value) = consumeFirstKeyedMessageFrom[String, ByteBuffer]("producer1")
      ProtobufSerialization.deserializeMessage(key, value) shouldBe Success(successMessage)
    }

    "fail if message is not serializable" in {
      val kafkaProducerRef = system.actorOf(KafkaProducer.props("producer2", testingPort), "producer2")
      val unSerializableMessage = NonSerializableMessage(messageValue)

      kafkaProducerRef ! unSerializableMessage
      expectMsg(Failure(SerializationException))
    }

    "restart when exception occurs" in {
      val kafkaProducerRef = system.actorOf(KafkaProducer.props("producer3", testingPort), "producer3")
      val testMessage = TestRequest(messageValue)

      kafkaProducerRef ! GenerateException("kafka producer exception test")
      kafkaProducerRef ! testMessage

      expectMsg(15.seconds, Success(Done))
      val (key, value) = consumeFirstKeyedMessageFrom[String, ByteBuffer]("producer3")
      ProtobufSerialization.deserializeMessage(key, value) shouldBe Success(testMessage)
    }

  }

  "kafka consumer actor" should {

    "consume serializable messages" in {
      val messagesConsumerProbe = TestProbe()
      system.actorOf(KafkaConsumer.props("consumer1", consumerGroup, messagesConsumerProbe.ref, testingPort), "consumer1")
      val testMessage = TestRequest(messageValue)

      val testMessageSerialized = ProtobufSerialization.serializeMessage(testMessage)
      testMessageSerialized.isSuccess shouldBe true

      publishToKafka("consumer1", testMessage.getClass.getName, testMessageSerialized.get)
      messagesConsumerProbe.expectMsg(testMessage)
      messagesConsumerProbe.reply(Try(Done))
    }

    "fail when consuming un-serializable message" in {
      val messagesConsumerProbe = TestProbe()
      system.actorOf(KafkaConsumer.props("consumer2", consumerGroup, messagesConsumerProbe.ref, testingPort), "consumer2")

      publishToKafka("consumer1", NonSerializableMessage.getClass.getName, ByteBuffer.wrap(Array.emptyByteArray))
      messagesConsumerProbe.expectNoMessage()
    }

    "skip committing if fail to process message" in {
      val messagesConsumerProbe = TestProbe()
      system.actorOf(KafkaConsumer.props("consumer3", consumerGroup, messagesConsumerProbe.ref, testingPort), "consumer3")
      val testMessage = TestRequest(messageValue)

      val testMessageSerialized = ProtobufSerialization.serializeMessage(testMessage)
      testMessageSerialized.isSuccess shouldBe true

      publishToKafka("consumer3", testMessage.getClass.getName, testMessageSerialized.get)
      messagesConsumerProbe.expectMsg(testMessage)
      messagesConsumerProbe.reply(Failure(TestException("Fail to process message")))
    }

    "restart if an exception occurs" in {
      val messagesConsumerProbe = TestProbe()
      val kafkaConsumerRef = system.actorOf(KafkaConsumer.props("consumer4", consumerGroup, messagesConsumerProbe.ref, testingPort), "consumer4")
      val testMessage = TestRequest(messageValue)

      val testMessageSerialized = ProtobufSerialization.serializeMessage(testMessage)
      testMessageSerialized.isSuccess shouldBe true

      kafkaConsumerRef ! GenerateException("kafka consumer exception test")
      publishToKafka("consumer4", testMessage.getClass.getName, testMessageSerialized.get)

      messagesConsumerProbe.expectMsg(15.seconds, testMessage)
      messagesConsumerProbe.reply(Try(Done))
    }

  }

  case class NonSerializableMessage(value: String)

}
