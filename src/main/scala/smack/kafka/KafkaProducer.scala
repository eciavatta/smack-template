package smack.kafka

import java.nio.ByteBuffer

import akka.Done
import akka.actor._
import akka.kafka.scaladsl.Producer
import akka.kafka.ProducerSettings
import akka.serialization.{Serialization, SerializationExtension}
import akka.stream.scaladsl.{Keep, RunnableGraph, Sink, Source, SourceQueueWithComplete}
import akka.stream.{ActorMaterializer, OverflowStrategy, QueueOfferResult}
import org.apache.kafka.clients.producer
import org.apache.kafka.common.serialization.{ByteBufferSerializer, StringSerializer}
import smack.kafka.KafkaProducer._

import scala.concurrent.ExecutionContext

class KafkaProducer(topic: String) extends Actor with ActorLogging {

  private implicit val actorMaterializer: ActorMaterializer = ActorMaterializer()
  private implicit val executionContext: ExecutionContext = context.dispatcher
  private val config = context.system.settings.config.getConfig("akka.kafka.producer")
  private implicit val serialization: Serialization = SerializationExtension(context.system)

  private val producerSettings: ProducerSettings[String, ByteBuffer] =
    ProducerSettings(config, new StringSerializer, new ByteBufferSerializer)
      .withBootstrapServers(config.getString("bootstrap-server"))
  private var kafkaProducer: producer.KafkaProducer[String, ByteBuffer] = _
  private var queue: SourceQueueWithComplete[KafkaMessage] = _

  override def preStart(): Unit = {
    kafkaProducer = producerSettings.createKafkaProducer()
    queue = createKafkaGraph().run()
  }

  override def postStop(): Unit = {
    kafkaProducer.close()
    queue.complete()
  }

  override def receive: Receive = {
    case message: AnyRef =>
      queue.offer(KafkaMessage(topic = topic, key = message.getClass.getName, value = message, sender = sender())).map {
        case QueueOfferResult.Enqueued => log.debug(s"Kafka message of class ${message.getClass.getName} is enqueued")
        case QueueOfferResult.Dropped => log.warning(s"Kafka message of class ${message.getClass.getName} is dropped")
        case QueueOfferResult.Failure(ex) => log.error(ex, s"Error after enqueuing message of class ${message.getClass.getName}")
        case QueueOfferResult.QueueClosed => log.debug(s"Kafka message of class ${message.getClass.getName} is dropped because queue is closed")
      }
  }

  private def createKafkaGraph(): RunnableGraph[SourceQueueWithComplete[KafkaMessage]] = Source
    .queue[KafkaMessage](config.getInt("buffer-size"), OverflowStrategy.backpressure)
    .via(ProtobufSerialization.serialize)
    .via(Producer.flexiFlow(producerSettings, kafkaProducer))
    .toMat(Sink.foreach(result => result.passThrough ! Done))(Keep.left)
}

object KafkaProducer {

  def props(topic: String): Props = Props(new KafkaProducer(topic))
  def name: String = "kafkaProducer"

  case class KafkaMessage(topic: String, partition: Int = 0, key: String, value: AnyRef, sender: ActorRef)

  trait KafkaResult
  case class SingleKafkaResult(topic: String, partition: Int, offset: Option[Long], timestamp: Option[Long],
                               key: String, value: AnyRef, sender: ActorRef) extends KafkaResult
  case class MultiKafkaResult(parts: List[SingleKafkaResult], sender: ActorRef) extends KafkaResult
  case class EmptyResult(sender: ActorRef) extends KafkaResult

}


