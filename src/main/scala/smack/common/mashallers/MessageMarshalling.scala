package smack.common.mashallers

import smack.models.HealthMessage
import smack.models.messages._
import spray.json.{DefaultJsonProtocol, RootJsonFormat}

trait MessageMarshalling extends DefaultJsonProtocol {
  this: Marshalling =>

  implicit val getUsersRequestFormat: RootJsonFormat[GetUsersRequest] = jsonFormat0(GetUsersRequest.apply)
  implicit val getUsersResponseFormat: RootJsonFormat[GetUsersResponse] = jsonFormat2(GetUsersResponse.apply)
  implicit val createUserFormat: RootJsonFormat[CreateUserRequest] = jsonFormat3(CreateUserRequest.apply)
  implicit val healthFormat: RootJsonFormat[HealthMessage] = jsonFormat6(HealthMessage.apply)

}
