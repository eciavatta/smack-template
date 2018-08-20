package smack.common.traits

import akka.Done
import akka.actor.{Actor, ActorLogging}
import akka.pattern.{AskTimeoutException, ask}
import smack.common.traits.Controller.{InternalServerErrorException, ServiceUnavailableException}
import smack.common.utils.Helpers

import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

trait KafkaController extends Controller with AskTimeout with ContextDispatcher {
  this: Actor with ActorLogging =>

  private val kafkaProducerRef = Helpers.createKafkaProducerPool(kafkaTopic)

  protected def kafkaTopic: String

  protected def sendToKafka(message: AnyRef): Future[Done] =
    kafkaProducerRef.ask(message).mapTo[Try[Done]].map {
      case Success(done) => done
      case Failure(_: AskTimeoutException) => throw ServiceUnavailableException
      case Failure(ex) =>
        log.error(ex, ex.getMessage)
        throw InternalServerErrorException(ex)
    }

}
