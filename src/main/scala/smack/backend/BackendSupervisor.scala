package smack.backend

import akka.actor.SupervisorStrategy.Resume
import akka.actor.{Actor, ActorLogging, OneForOneStrategy, Props}
import smack.backend.controllers.{TestController, UserController}
import smack.common.serialization.MessageSerializer.UserRequest
import smack.kafka.KafkaProducer
import smack.models.messages.TestRequest

class BackendSupervisor extends Actor with ActorLogging {

  private val kafkaProducer = context.actorOf(KafkaProducer.props("test"), KafkaProducer.name)
  private val userController = context.actorOf(UserController.props, UserController.name)
  private val testController = context.actorOf(TestController.props(kafkaProducer), TestController.name)

  override def supervisorStrategy: OneForOneStrategy = OneForOneStrategy() {
    case _ => Resume
  }

  override def receive: Receive = {
    case message: UserRequest => userController forward message
    case test: TestRequest => testController forward test
  }

}

object BackendSupervisor {
  def props: Props = Props(new BackendSupervisor)
  def name: String = "backendSupervisor"
}
