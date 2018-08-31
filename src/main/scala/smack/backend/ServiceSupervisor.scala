package smack.backend

import akka.actor.SupervisorStrategy.Restart
import akka.actor.{Actor, ActorLogging, OneForOneStrategy, Props}
import smack.backend.services.LogService

class ServiceSupervisor extends Actor with ActorLogging {

  context.actorOf(LogService.props, LogService.name)

  override def supervisorStrategy: OneForOneStrategy = OneForOneStrategy() {
    case _ => Restart
  }

  override def receive: Receive = {
    case _ =>
  }

}

object ServiceSupervisor {
  def props: Props = Props(new ServiceSupervisor)
  def name: String = "serviceSupervisor"
}


