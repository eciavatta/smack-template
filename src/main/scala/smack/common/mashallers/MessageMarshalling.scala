package smack.common.mashallers

import smack.models.HealthMessage
import smack.models.messages._
import spray.json.{DefaultJsonProtocol, RootJsonFormat}

trait MessageMarshalling extends DefaultJsonProtocol {
  this: Marshalling =>

  implicit val healthFormat: RootJsonFormat[HealthMessage] = jsonFormat6(HealthMessage.apply)

  implicit val createUserRequestFormat: RootJsonFormat[CreateUserRequest] = jsonFormat3(CreateUserRequest.apply)
  implicit val updateUserRequestFormat: RootJsonFormat[UpdateUserRequest] = jsonFormat2(UpdateUserRequest.apply)
  implicit val createSiteRequestFormat: RootJsonFormat[CreateSiteRequest] = jsonFormat2(CreateSiteRequest.apply)

}
