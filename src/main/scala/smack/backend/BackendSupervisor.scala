package smack.backend

import akka.actor.SupervisorStrategy.Resume
import akka.actor.{Actor, ActorLogging, OneForOneStrategy, Props}
import smack.backend.controllers.UserController
import smack.common.serialization.MessageSerializer.UserRequest

class BackendSupervisor extends Actor with ActorLogging {

  private val userController = context.actorOf(UserController.props, UserController.name)

  override def supervisorStrategy: OneForOneStrategy = OneForOneStrategy() {
    case _ => Resume
  }

  override def receive: Receive = {
    case message: UserRequest => userController forward message
  }

}

object BackendSupervisor {
  def props: Props = Props(new BackendSupervisor)
  def name: String = "backendSupervisor"
}
