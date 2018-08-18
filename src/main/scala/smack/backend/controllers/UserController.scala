package smack.backend.controllers

import java.util.UUID

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import com.datastax.driver.core.SimpleStatement
import smack.common.traits.{AskTimeout, CassandraController, ContextDispatcher}
import smack.common.utils.Responses
import smack.models.messages.{FindUserRequest, FindUserResponse}
import smack.models.structures.{Date, User}

import scala.collection.JavaConverters._

class UserController extends Actor with CassandraController with ActorLogging with AskTimeout with ContextDispatcher  {

  override def receive: Receive = {
    case FindUserRequest(id) => parseUUID(id) match {
      case Left(badRequest) => sender ! FindUserResponse(badRequest)
      case Right(uuid) => findUser(uuid, sender)
    }
    // case CreateUserRequest(email, password, fullName) =>

  }

  private def findUser(uuid: UUID, sender: ActorRef): Unit = executeStatement(sender,
    new SimpleStatement("SELECT id, email, full_name, toTimestamp(id) as registered_date FROM users_by_id WHERE id = ?", uuid)) {
      case Right(result) => result.all().asScala.headOption.fold(FindUserResponse(Responses.notFound())) { row =>
        FindUserResponse(user = Some(User(
          id = row.getString("id"),
          email = row.getString("email"),
          fullName = row.getString("full_name"),
          registeredDate = Some(Date(row.getTimestamp("registered_date").getTime))
        )))
      }
      case Left(serverError) => FindUserResponse(serverError)
    }

//  private def checkEmailAlreadyUsed(email: String, sender: ActorRef): Unit = executeStatement(sender,
//    new SimpleStatement("SELECT COUNT(*) FROM users_by_credentials WHERE email = ?", email)) {
//    case Right(result) =>
//    case Left(serverError) => CreateUserResponse(serverError)
//  }

}

object UserController {
  def props: Props = Props(new UserController)
  def name: String = "userController"
}
