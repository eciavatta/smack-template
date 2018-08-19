package smack.common.mashallers

import java.text.SimpleDateFormat

import com.typesafe.config.ConfigFactory
import smack.models.structures.{Date, ResponseStatus, Site, User}
import spray.json.{DefaultJsonProtocol, DeserializationException, JsString, JsValue, RootJsonFormat}

trait Marshalling extends DefaultJsonProtocol {

  implicit object DateJsonFormat extends RootJsonFormat[Date] {

    private val dateFormat = new SimpleDateFormat(ConfigFactory.load("smack.conf").getString("smack.dateFormat"))

    override def write(obj: Date): JsValue = JsString(dateFormat.format(obj.timestamp))

    override def read(json: JsValue): Date = json match {
      case JsString(s) => Date(dateFormat.parse(s).getTime)
      case _ => throw DeserializationException("Date deserialization error")
    }
  }

  implicit val userFormat: RootJsonFormat[User] = jsonFormat4(User.apply)
  implicit val siteFormat: RootJsonFormat[Site] = jsonFormat4(Site.apply)
  implicit val responseStatusFormat: RootJsonFormat[ResponseStatus] = jsonFormat4(ResponseStatus.apply)

}

