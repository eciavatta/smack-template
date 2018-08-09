package smack.backend.controllers

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import smack.common.traits.KafkaController
import smack.common.utils.RestApiCodes
import smack.models.messages.{TestRequest, TestResponse}

class TestController(protected val kafkaController: ActorRef) extends Actor with ActorLogging with KafkaController {

  override def receive: Receive = {
    case t: TestRequest => sendToKafka(t, sender, TestResponse(RestApiCodes.Accepted, t.value))
  }

}

object TestController {
  def props(kafkaController: ActorRef): Props = Props(new TestController(kafkaController))
  def name: String = "testController"
}
