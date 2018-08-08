package smack.cluster.backend

import akka.actor._
import smack.models.messages._
import smack.models.structures._

class Backend(kafkaProducer: ActorRef) extends Actor with ActorLogging {

  private val OK: Int = 200
  private val Created: Int = 201
  private val NotFound: Int = 404

  private var users: Set[User] = Set()

  override def receive: Receive = {
    case GetUsersRequest() =>
      sender ! GetUsersResponse(OK, users.toList)
    case GetUserRequest(id) =>
      val user = users.find(u => u.id == id)
      sender ! GetUserResponse(if (user.isDefined) OK else NotFound, user)
    case CreateUserRequest(email, username, _) =>
      val user = User(users.size, username, email, Some(Date(System.currentTimeMillis())))
      users += user
      sender ! CreateUserResponse(Created, Some(user))
    case DeleteUserRequest(id) =>
      val user = users.find(u => u.id == id)
      sender ! DeleteUserResponse(user.fold(NotFound)(u => { users -= u; OK }))
      DeleteUserRequest(id).toByteArray
  }

}

object Backend {
  def props(kafkaProducer: ActorRef): Props = Props(new Backend(kafkaProducer))
  def name: String = "backend"
}
