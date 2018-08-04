package smack.backend

import akka.actor.{Actor, ActorLogging, DeadLetter}
import smack.model.{GetUsersRequest, GetUsersResponse}

class Backend extends Actor with ActorLogging {

  override def receive: Receive = {
    case GetUsersRequest => sender ! GetUsersResponse(List())
    case d: DeadLetter => log.info(d.toString)
  }

}
