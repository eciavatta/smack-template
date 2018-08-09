package smack.backend.controllers

import akka.actor._
import smack.common.utils.RestApiCodes
import smack.models.messages._
import smack.models.structures._

class UserController extends Actor with ActorLogging {

  private var users: Set[User] = Set()

  override def receive: Receive = {
    case GetUsersRequest() =>
      sender ! GetUsersResponse(RestApiCodes.OK, users.toList)
    case GetUserRequest(id) =>
      val user = users.find(u => u.id == id)
      sender ! GetUserResponse(if (user.isDefined) RestApiCodes.OK else RestApiCodes.NotFound, user)
    case CreateUserRequest(email, username, _) =>
      val user = User(users.size, username, email, Some(Date(System.currentTimeMillis())))
      users += user
      sender ! CreateUserResponse(RestApiCodes.Created, Some(user))
    case DeleteUserRequest(id) =>
      val user = users.find(u => u.id == id)
      sender ! DeleteUserResponse(user.fold(RestApiCodes.NotFound)(u => { users -= u; RestApiCodes.OK }))
      DeleteUserRequest(id).toByteArray
  }

}

object UserController {
  def props: Props = Props(new UserController)
  def name: String = "userController"
}
