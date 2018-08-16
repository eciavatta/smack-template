package smack.kafka

import java.nio.ByteBuffer

import akka.actor.{Actor, ActorRef, Props}
import akka.event.Logging
import akka.kafka.scaladsl.Producer
import akka.kafka.{ProducerMessage, ProducerSettings}
import akka.serialization.Serialization
import akka.stream.scaladsl.{Broadcast, Flow, GraphDSL, Merge, RunnableGraph, Sink, Source, SourceQueueWithComplete}
import akka.stream.{AbruptStageTerminationException, ClosedShape, OverflowStrategy, QueueOfferResult}
import akka.{Done, NotUsed}
import org.apache.kafka.clients.producer
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.{ByteBufferSerializer, StringSerializer}
import smack.common.traits.{ContextDispatcher, ImplicitMaterializer, ImplicitSerialization}
import smack.common.utils.Helpers
import smack.kafka.KafkaProducer.KafkaMessage
import smack.kafka.ProtobufSerialization.serializeMessage
import smack.models.messages.GenerateException
import smack.models.{SerializationException, TestException}

import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

class KafkaProducer(topic: String, partition: Int)
  extends Actor with ImplicitMaterializer with ImplicitSerialization with ContextDispatcher {

  private val log = Logging(context.system, context.self)
  private val config = Helpers.actorConfig.getConfig("smack.kafka.producer")

  private val producerSettings: ProducerSettings[String, ByteBuffer] =
    ProducerSettings(Helpers.actorConfig.getConfig("akka.kafka.producer"), new StringSerializer, new ByteBufferSerializer)
      .withBootstrapServers(config.getString("bootstrap-server"))
  private var kafkaProducer: producer.KafkaProducer[String, ByteBuffer] = _
  private var queue: SourceQueueWithComplete[KafkaMessage] = _

  override def preStart(): Unit = {
    kafkaProducer = producerSettings.createKafkaProducer()
    queue = createKafkaGraph().run()
  }

  override def postStop(): Unit = {
    queue.complete()
    kafkaProducer.close()
  }

  override def receive: Receive = {
    case ex: GenerateException => throw TestException(ex.message)
    case message: AnyRef =>
      queue.offer(KafkaMessage(topic = topic, partition = partition, key = message.getClass.getName, value = message, sender = sender())).map {
        case QueueOfferResult.Enqueued => log.debug(s"Kafka message of class ${message.getClass.getName} is enqueued")
        case QueueOfferResult.Dropped => log.warning(s"Kafka message of class ${message.getClass.getName} is dropped")
        case QueueOfferResult.Failure(ex) => log.error(ex, s"Error after enqueuing message of class ${message.getClass.getName}")
        case QueueOfferResult.QueueClosed => log.debug(s"Kafka message of class ${message.getClass.getName} is dropped because queue is closed")
      }
  }

  private def sourceQueue: Source[KafkaMessage, SourceQueueWithComplete[KafkaMessage]] =
    Source.queue[KafkaMessage](config.getInt("buffer-size"), OverflowStrategy.backpressure)

  private def watchTermination = Flow[KafkaMessage].watchTermination() { (sourceQueue, futureDone) =>
    futureDone.onComplete {
      case Success(_) => log.debug(s"Kafka producer stream for topic $topic is closed.")
      case Failure(_: AbruptStageTerminationException) => log.debug(s"Kafka producer stream for topic $topic is abruptly terminated.")
      case Failure(ex) => log.error(ex, ex.getMessage)
    }
    sourceQueue
  }

  private def serialize(implicit serialization: Serialization) = Flow[KafkaMessage] map { message =>
    serializeMessage(message.value) match {
      case Success(serialized) => Right(ProducerMessage.Message(
        new ProducerRecord(message.topic, message.partition, message.key, serialized), message.sender)
      )
      case Failure(_) => Left(message.sender)
    }
  }

  private def filterSerializable = Flow[Either[ActorRef, ProducerMessage.Message[String, ByteBuffer, ActorRef]]] filter(_.isRight) map (_.right.get)
  private def filterUnSerializable = Flow[Either[ActorRef, ProducerMessage.Message[String, ByteBuffer, ActorRef]]] filter(_.isLeft) map {
    either => (either.left.get, Failure(SerializationException))
  }

  private def sendToKafka: Flow[ProducerMessage.Envelope[String, ByteBuffer, ActorRef], ProducerMessage.Results[String, ByteBuffer, ActorRef], NotUsed] =
    Producer.flexiFlow[String, ByteBuffer, ActorRef](producerSettings, kafkaProducer)

  private def mapKafkaResult = Flow[ProducerMessage.Results[String, ByteBuffer, ActorRef]] map {
    result => (result.passThrough, Success(Done))
  }

  private def sendResponse: Sink[(ActorRef, Try[Done]), Future[Done]] =
    Sink.foreach[(ActorRef, Try[Done])](pair => pair._1 ! pair._2)

  private def createKafkaGraph(): RunnableGraph[SourceQueueWithComplete[KafkaMessage]] = RunnableGraph.fromGraph({
    GraphDSL.create(sourceQueue) {
      implicit builder => queueShape =>
        import GraphDSL.Implicits._

        val broadcast = builder.add(Broadcast[Either[ActorRef, ProducerMessage.Message[String, ByteBuffer, ActorRef]]](2))
        val merge = builder.add(Merge[(ActorRef, Try[Done])](2))

        queueShape ~> watchTermination ~> serialize ~> broadcast ~> filterSerializable ~> sendToKafka ~> mapKafkaResult ~> merge ~> sendResponse
                                                       broadcast ~> filterUnSerializable                                ~> merge
        ClosedShape
    }
  })

}

object KafkaProducer {

  def props(topic: String, partition: Int): Props = Props(new KafkaProducer(topic, partition))
  def name(topic: String, partition: Int): String = s"kafkaProducer-$topic-$partition"

  private[kafka] case class KafkaMessage(topic: String, partition: Int, key: String, value: AnyRef, sender: ActorRef)

  private[kafka] trait KafkaResult
  private[kafka] case class SingleKafkaResult(topic: String, partition: Int, offset: Option[Long],
                                              timestamp: Option[Long], key: String, value: AnyRef,
                                              sender: ActorRef) extends KafkaResult
  private[kafka] case class MultiKafkaResult(parts: List[SingleKafkaResult], sender: ActorRef) extends KafkaResult
  private[kafka] case class EmptyResult(sender: ActorRef) extends KafkaResult

}
