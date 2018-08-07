package smack.frontend.marshallers

import java.text.SimpleDateFormat

import com.typesafe.config.ConfigFactory
import smack.frontend.routes.HealthMessage
import smack.models.structures._
import smack.models.messages._
import spray.json.{DefaultJsonProtocol, DeserializationException, JsString, JsValue, RootJsonFormat}

trait ModelMarshalling extends DefaultJsonProtocol {

  implicit object DateJsonFormat extends RootJsonFormat[Date] {

    private val dateFormat = new SimpleDateFormat(ConfigFactory.load().getString("dateFormat"))

    override def write(obj: Date): JsValue = JsString(dateFormat.format(obj.timestamp))

    override def read(json: JsValue): Date = json match {
      case JsString(s) => Date(dateFormat.parse(s).getTime)
      case _ => throw DeserializationException("Date deserialization error")
    }
  }

  implicit val userFormat: RootJsonFormat[User] = jsonFormat4(User.apply)
  implicit val createUserFormat: RootJsonFormat[CreateUserRequest] = jsonFormat3(CreateUserRequest.apply)
  implicit val healthFormat: RootJsonFormat[HealthMessage] = jsonFormat6(HealthMessage.apply)

}
