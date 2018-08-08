package smack.mashallers

import smack.models.HealthMessage
import smack.models.messages.CreateUserRequest
import spray.json.{DefaultJsonProtocol, RootJsonFormat}

object MessageMarshalling extends DefaultJsonProtocol {

  implicit val createUserFormat: RootJsonFormat[CreateUserRequest] = jsonFormat3(CreateUserRequest.apply)
  implicit val healthFormat: RootJsonFormat[HealthMessage] = jsonFormat6(HealthMessage.apply)

}
