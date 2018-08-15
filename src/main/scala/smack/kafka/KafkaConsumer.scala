package smack.kafka

import java.nio.ByteBuffer

import akka.Done
import akka.actor.{Actor, ActorRef, Props}
import akka.event.Logging
import akka.kafka.scaladsl.Consumer
import akka.kafka.{ConsumerMessage, ConsumerSettings, Subscriptions}
import akka.pattern.ask
import akka.stream.scaladsl.{Flow, GraphDSL, RunnableGraph, Sink, Source}
import akka.stream.{AbruptStageTerminationException, ClosedShape}
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.serialization.{ByteBufferDeserializer, StringDeserializer}
import smack.common.traits.{AskTimeout, ContextDispatcher, ImplicitMaterializer, ImplicitSerialization}
import smack.models.TestException
import smack.models.messages.GenerateException

import scala.util.{Failure, Success, Try}

class KafkaConsumer(topic: String, group: String, consumingActor: ActorRef, kafkaPort: Option[Int] = None)
  extends Actor with ImplicitMaterializer with ImplicitSerialization with AskTimeout with ContextDispatcher {

  private val log = Logging(context.system, context.self)

  private val config = context.system.settings.config.getConfig("akka.kafka.consumer")
  private val consumerSettings = ConsumerSettings(config, new StringDeserializer, new ByteBufferDeserializer)
                                 .withBootstrapServers(kafkaPort.fold(config.getString("bootstrap-server"))(port => s"127.0.0.1:$port"))
                                 .withGroupId(group)
                                 .withProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")
  private var consumerControl: Consumer.Control = _

  override def preStart(): Unit = {
    consumerControl = createConsumerGraph().run()
  }

  override def postStop(): Unit = {
    consumerControl.stop() map (_ => consumerControl.shutdown())
  }

  override def receive: Receive = {
    case ex: GenerateException => throw TestException(ex.message)
  }

  private def consumerSource: Source[ConsumerMessage.CommittableMessage[String, ByteBuffer], Consumer.Control] =
    Consumer.committableSource(consumerSettings, Subscriptions.topics(topic))

  private def watchTermination = Flow[ConsumerMessage.CommittableMessage[String, ByteBuffer]].watchTermination() { (message, futureDone) =>
    futureDone.onComplete {
      case Success(_) => log.debug(s"Kafka consumer stream for topic $topic is closed.")
      case Failure(_: AbruptStageTerminationException) => log.debug(s"Kafka consumer stream of actor for topic $topic is abruptly terminated.")
      case Failure(ex) => log.error(ex, ex.getMessage)
    }
    message
  }

  private def deserialize = Flow[ConsumerMessage.CommittableMessage[String, ByteBuffer]] map { message =>
    ProtobufSerialization.deserializeMessage(message.record.key, message.record.value) map ((_, message.committableOffset))
  }

  private def filterSerializable = Flow[Try[(AnyRef, ConsumerMessage.CommittableOffset)]] filter (_.isSuccess) map (_.get)

  private def consumeMessage = Flow[(AnyRef, ConsumerMessage.CommittableOffset)].mapAsync(config.getInt("consume-message-parallelism")) { pair =>
    (consumingActor ? pair._1).mapTo[Try[Done]] filter (_.isSuccess) map (_ => pair._2)
  }

  private def commitOffset = Flow[ConsumerMessage.CommittableOffset].mapAsync(config.getInt("commit-message-parallelism"))(_.commitScaladsl())

  private def createConsumerGraph(): RunnableGraph[Consumer.Control] = RunnableGraph.fromGraph({
    GraphDSL.create(consumerSource) {
      implicit builder => sourceShape =>
        import GraphDSL.Implicits._

        sourceShape ~> watchTermination ~> deserialize ~> filterSerializable ~> consumeMessage ~> commitOffset ~> Sink.ignore
        ClosedShape
    }
  })

}

object KafkaConsumer {

  def props(topic: String, group: String, consumingActor: ActorRef): Props = Props(new KafkaConsumer(topic, group, consumingActor))
  def name: String = "kafkaConsumer"

}
