package smack.backend

import akka.actor.SupervisorStrategy.Resume
import akka.actor.{Actor, ActorLogging, OneForOneStrategy, Props}
import smack.backend.controllers.UserController
import smack.kafka.KafkaProducer

class BackendSupervisor extends Actor with ActorLogging {

  private val kafkaProducer = context.actorOf(KafkaProducer.props("test"), KafkaProducer.name)
  private val backend = context.actorOf(UserController.props(kafkaProducer), UserController.name)

  override def supervisorStrategy: OneForOneStrategy = OneForOneStrategy() {
    case _ => Resume
  }

  override def receive: Receive = {
    case message: Any => backend forward message
  }

}

object BackendSupervisor {
  def props: Props = Props(new BackendSupervisor)
  def name: String = "clusterSupervisor"
}
