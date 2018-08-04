package smack.frontend.marshallers

import java.text.SimpleDateFormat
import java.util.Date

import com.typesafe.config.ConfigFactory
import smack.frontend.routes.HealthMessage
import smack.models.Tweeters._
import spray.json.{DefaultJsonProtocol, DeserializationException, JsString, JsValue, RootJsonFormat}

trait ModelMarshalling extends DefaultJsonProtocol {

  implicit object DateJsonFormat extends RootJsonFormat[Date] {

    private val dateFormat = new SimpleDateFormat(ConfigFactory.load().getString("dateFormat"))

    override def write(obj: Date): JsValue = JsString(dateFormat.format(obj))

    override def read(json: JsValue): Date = json match {
      case JsString(s) => dateFormat.parse(s)
      case _ => throw DeserializationException("Date deserialization error")
    }
  }

  implicit val authorFormat: RootJsonFormat[User] = jsonFormat3(User)
  implicit val authorCreationFormat: RootJsonFormat[UserCreated] = jsonFormat3(UserCreated)

  implicit val healthFormat: RootJsonFormat[HealthMessage] = jsonFormat6(HealthMessage)

}
