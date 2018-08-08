package smack.cluster

import akka.actor.SupervisorStrategy.Resume
import akka.actor.{Actor, ActorLogging, OneForOneStrategy, Props}
import smack.cluster.backend.Backend
import smack.cluster.kafka.KafkaProducer

class ClusterSupervisor extends Actor with ActorLogging {

  private val kafkaProducer = context.actorOf(KafkaProducer.props("test"), KafkaProducer.name)
  private val backend = context.actorOf(Backend.props(kafkaProducer), Backend.name)

  override def supervisorStrategy: OneForOneStrategy = OneForOneStrategy() {
    case _ => Resume
  }

  override def receive: Receive = {
    case message: Any => backend forward message
  }

}

object ClusterSupervisor {
  def props: Props = Props(new ClusterSupervisor)
  def name: String = "clusterSupervisor"
}
