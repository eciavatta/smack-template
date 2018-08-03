package smack.model

case object GetUsersRequest
case class GetUsersResponse(users: List[User])
case class AddUserRequest(user: UserCreated)
case class AddUserResponse(message: Option[String])
