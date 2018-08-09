package smack.common.traits

import akka.Done
import akka.actor.{Actor, ActorLogging, ActorRef}
import smack.common.serialization.MessageSerializer.ResponseMessage
import akka.pattern.{AskTimeoutException, ask}

import scala.util.{Failure, Success}

trait KafkaController extends AskTimeout with ContextDispatcher {
  this: Actor with ActorLogging =>

  protected def kafkaController: ActorRef

  protected def sendToKafka(message: AnyRef, sender: ActorRef, response: ResponseMessage): Unit =
    kafkaController.ask(message).mapTo[Done].onComplete {
      case Success(_) => sender ! response
      case Failure(_: AskTimeoutException) => log.error(s"Kafka controller [${kafkaController.path}] is unreachable.")
      case Failure(ex) => log.error(ex, ex.getMessage)
    }

}
