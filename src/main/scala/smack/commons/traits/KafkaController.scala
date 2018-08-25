package smack.commons.traits

import akka.Done
import akka.actor.{Actor, ActorContext, ActorLogging, ActorRef}
import akka.pattern.{AskTimeoutException, ask}
import akka.routing.RoundRobinGroup
import Controller.{InternalServerErrorException, ServiceUnavailableException}
import smack.commons.utils.Helpers
import smack.kafka.KafkaProducer

import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

trait KafkaController extends Controller with AskTimeout with ContextDispatcher {
  this: Actor with ActorLogging =>

  private val kafkaProducerRef = createKafkaProducerPool(kafkaTopic)

  protected def kafkaTopic: String

  protected def sendToKafka(message: AnyRef): Future[Done] =
    kafkaProducerRef.ask(message).mapTo[Try[Done]].map {
      case Success(done) => done
      case Failure(_: AskTimeoutException) => throw ServiceUnavailableException
      case Failure(ex) =>
        log.error(ex, ex.getMessage)
        throw InternalServerErrorException(ex)
    }

  private def createKafkaProducerPool(topic: String)(implicit context: ActorContext): ActorRef = {
    val paths = (0 until Helpers.actorConfig.getInt(s"smack.topics.$topic.kafka-partitions")) map { i =>
      context.actorOf(KafkaProducer.props(topic, i), KafkaProducer.name(topic, i)).path.toStringWithoutAddress
    }

    context.actorOf(RoundRobinGroup(paths).props(), s"${topic}KafkaProducerRouter")
  }

}
