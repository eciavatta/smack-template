package smack.backend

import akka.actor.{Actor, ActorLogging}
import smack.models.messages._

class Backend extends Actor with ActorLogging {

  override def receive: Receive = {
    case _: GetUsersRequest => sender ! GetUsersResponse("ciao")
  }

}
