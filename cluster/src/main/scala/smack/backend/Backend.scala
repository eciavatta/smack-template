package smack.backend

import akka.actor.Actor
import smack.model.GetUsersRequest

class Backend extends Actor {

  override def receive: Receive = {
    case GetUsersRequest => List()
  }

}
