package smack.backend.controllers

import java.util.Calendar

import akka.actor.ActorSystem
import smack.backend.marshallers.ModelMarshalling
import smack.backend.server.EmptyController
import smack.model.{User, UserCreated}

import scala.concurrent.{ExecutionContext, Future}

class MessageController(implicit val system: ActorSystem) extends EmptyController with ModelMarshalling {

  private implicit val ec: ExecutionContext = system.dispatcher
  private var users = Set[User]()

  def list(): Future[List[User]] = Future(users.toList)

  def create(user: UserCreated): Future[Boolean] = Future {
    if (users.exists(_.username == user.username)) {
      false
    } else {
      users += User(users.size, user.username, Calendar.getInstance.getTime)
      true
    }
  }

}
