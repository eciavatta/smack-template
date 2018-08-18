package smack.kafka

import java.nio.ByteBuffer

import akka.Done
import akka.actor.ActorSystem
import akka.serialization.{Serialization, SerializationExtension}
import akka.testkit.{DefaultTimeout, ImplicitSender, TestKitBase, TestProbe}
import com.typesafe.config.ConfigFactory
import net.manub.embeddedkafka.{EmbeddedKafka, EmbeddedKafkaConfig}
import org.apache.kafka.common.serialization._
import org.scalatest.{BeforeAndAfterEach, Matchers, WordSpecLike}
import smack.common.traits.AfterAllShutdown
import smack.models.messages.{GenerateException, TestRequest}
import smack.models.{SerializationException, TestException}

import scala.concurrent.duration._
import scala.util.{Failure, Success, Try}

class KafkaProducerConsumerSpec extends TestKitBase with EmbeddedKafka
  with WordSpecLike with Matchers with AfterAllShutdown with BeforeAndAfterEach with ImplicitSender with DefaultTimeout {

  implicit lazy val system: ActorSystem = ActorSystem("kafkaProducerConsumerSpec", ConfigFactory.load("test"))

  val consumerGroup = "embedded-kafka-spec"
  val topic = "test"
  val partition = 0
  val zookeeperPort = 2181
  val kafkaPort = 9092

  implicit lazy val keySerializer: Serializer[String] = new StringSerializer
  implicit lazy val valueSerializer: Serializer[ByteBuffer] = new ByteBufferSerializer
  implicit lazy val keyDeserializer: Deserializer[String] = new StringDeserializer
  implicit lazy val valueDeserializer: Deserializer[ByteBuffer] = new ByteBufferDeserializer
  implicit lazy val serialization: Serialization = SerializationExtension(system)
  implicit val kafkaConfig: EmbeddedKafkaConfig = EmbeddedKafkaConfig(kafkaPort, zookeeperPort)

  protected override def afterEach(): Unit = {
    Try {
      consumeNumberMessagesFromTopics[ByteBuffer](Set(topic), number = 3, autoCommit = true, 500.millis, resetTimeoutOnEachMessage = false) // clean topic
    }
  }

  "protobuf serialization" should {

    "serialize and deserialize messages" in {
      val messageText = "de-serialize test"
      val serializableMessage = TestRequest(messageText)
      val unSerializableMessage = NonSerializableMessage(messageText)

      val serializationResult = ProtobufSerialization.serializeMessage(serializableMessage)
      serializationResult.isSuccess shouldBe true

      ProtobufSerialization.deserializeMessage(serializableMessage.getClass.getName,
        ByteBuffer.wrap(serializationResult.get.array())) shouldBe Success(serializableMessage)

      val unSerializationResult = ProtobufSerialization.serializeMessage(unSerializableMessage)
      unSerializationResult.isSuccess shouldBe false
    }

  }

  "kafka producer actor" should {

    "log a serializable message" in {
      val kafkaProducerRef = system.actorOf(KafkaProducer.props(topic, partition), "producer1")
      val successMessage = TestRequest("success producer message")

      kafkaProducerRef ! successMessage
      expectMsg(Success(Done))

      val (key, value) = consumeFirstKeyedMessageFrom[String, ByteBuffer](topic)
      ProtobufSerialization.deserializeMessage(key, value) shouldBe Success(successMessage)
    }

    "fail if message is not serializable" in {
      val kafkaProducerRef = system.actorOf(KafkaProducer.props(topic, partition), "producer2")
      val unSerializableMessage = NonSerializableMessage("non serializable producer message")

      kafkaProducerRef ! unSerializableMessage
      expectMsg(Failure(SerializationException))
    }

    "restart when exception occurs" in {
      val kafkaProducerRef = system.actorOf(KafkaProducer.props(topic, partition), "producer3")
      val restartMessage = TestRequest("success producer message after restart")

      kafkaProducerRef ! GenerateException("kafka producer exception test")
      kafkaProducerRef ! restartMessage

      expectMsg(Success(Done))
      val (key, value) = consumeFirstKeyedMessageFrom[String, ByteBuffer](topic)
      ProtobufSerialization.deserializeMessage(key, value) shouldBe Success(restartMessage)
    }

  }

  "kafka consumer actor" should {

    "consume serializable messages" in {
      val messagesConsumerProbe = TestProbe()
      system.actorOf(KafkaConsumer.props(topic, consumerGroup, messagesConsumerProbe.ref), "consumer1")
      val successMessage = TestRequest("success consumer message")

      val successMessageSerialized = ProtobufSerialization.serializeMessage(successMessage)
      successMessageSerialized.isSuccess shouldBe true

      publishToKafka(topic, successMessage.getClass.getName, successMessageSerialized.get)
      messagesConsumerProbe.expectMsg(successMessage)
      messagesConsumerProbe.reply(Try(Done))
    }

    "fail when consuming un-serializable message" in {
      val messagesConsumerProbe = TestProbe()
      system.actorOf(KafkaConsumer.props(topic, consumerGroup, messagesConsumerProbe.ref), "consumer2")

      publishToKafka(topic, NonSerializableMessage.getClass.getName, ByteBuffer.wrap(Array.emptyByteArray))
      messagesConsumerProbe.expectNoMessage()
    }

    "skip committing if fail to process message" in {
      val messagesConsumerProbe = TestProbe()
      system.actorOf(KafkaConsumer.props(topic, consumerGroup, messagesConsumerProbe.ref), "consumer3")
      val skipMessage = TestRequest("skip consumer message")

      val skipMessageSerialized = ProtobufSerialization.serializeMessage(skipMessage)
      skipMessageSerialized.isSuccess shouldBe true

      publishToKafka(topic, skipMessage.getClass.getName, skipMessageSerialized.get)
      messagesConsumerProbe.expectMsg(skipMessage)
      messagesConsumerProbe.reply(Failure(TestException("Fail to process message")))

    }

    "restart if an exception occurs" in {
      val messagesConsumerProbe = TestProbe()
      val kafkaConsumerRef = system.actorOf(KafkaConsumer.props(topic, consumerGroup, messagesConsumerProbe.ref), "consumer4")
      val restartMessage = TestRequest("success consumer message after restart")

      val restartMessageSerialized = ProtobufSerialization.serializeMessage(restartMessage)
      restartMessageSerialized.isSuccess shouldBe true

      kafkaConsumerRef ! GenerateException("kafka consumer exception test")
      publishToKafka(topic, restartMessage.getClass.getName, restartMessageSerialized.get)

      messagesConsumerProbe.expectMsg(restartMessage)
      messagesConsumerProbe.reply(Try(Done))
    }

  }

  case class NonSerializableMessage(value: String)

}
