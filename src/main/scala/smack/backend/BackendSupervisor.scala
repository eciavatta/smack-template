package smack.backend

import akka.actor.SupervisorStrategy.Restart
import akka.actor.{Actor, ActorLogging, OneForOneStrategy, Props}
import smack.backend.controllers.{LogController, SiteController, UserController}
import smack.commons.serialization.MessageSerializer.{SiteRequest, UserRequest}
import smack.models.messages.TraceLogRequest

class BackendSupervisor extends Actor with ActorLogging {

  private lazy val userController = context.actorOf(UserController.props, UserController.name)
  private lazy val siteController = context.actorOf(SiteController.props, SiteController.name)
  private lazy val logController = context.actorOf(LogController.props, LogController.name)

  override def supervisorStrategy: OneForOneStrategy = OneForOneStrategy() {
    case _ => Restart
  }

  override def receive: Receive = {
    case message: UserRequest => userController forward message
    case message: SiteRequest => siteController forward message
    case message: TraceLogRequest => logController forward message
  }

}

object BackendSupervisor {
  def props: Props = Props(new BackendSupervisor)
  def name: String = "backendSupervisor"
}
