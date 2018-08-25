package smack.commons.mashallers

import java.text.SimpleDateFormat

import com.typesafe.config.ConfigFactory
import smack.models.Events._
import smack.models.structures.{Date, ResponseStatus, Site, User}
import spray.json.{DefaultJsonProtocol, DeserializationException, JsString, JsValue, RootJsonFormat}

trait Marshalling extends DefaultJsonProtocol {

  implicit object DateJsonFormat extends RootJsonFormat[Date] {

    private val dateFormat = new SimpleDateFormat(ConfigFactory.load("common").getString("smack.dateFormat"))

    override def write(obj: Date): JsValue = JsString(dateFormat.format(obj.timestamp))

    override def read(json: JsValue): Date = json match {
      case JsString(s) => Date(dateFormat.parse(s).getTime)
      case _ => throw DeserializationException("Date deserialization error")
    }
  }

  implicit val userFormat: RootJsonFormat[User] = jsonFormat4(User.apply)
  implicit val siteFormat: RootJsonFormat[Site] = jsonFormat4(Site.apply)
  implicit val responseStatusFormat: RootJsonFormat[ResponseStatus] = jsonFormat4(ResponseStatus.apply)

  // Events

  implicit val userCreatingFormat: RootJsonFormat[UserCreating] = jsonFormat3(UserCreating.apply)
  implicit val userUpdatingFormat: RootJsonFormat[UserUpdating] = jsonFormat1(UserUpdating.apply)

  implicit val sitesListingFormat: RootJsonFormat[SitesListing] = jsonFormat1(SitesListing.apply)
  implicit val siteCreatingFormat: RootJsonFormat[SiteCreating] = jsonFormat2(SiteCreating.apply)
  implicit val siteUpdatingFormat: RootJsonFormat[SiteUpdating] = jsonFormat1(SiteUpdating.apply)
  implicit val siteDeletingFormat: RootJsonFormat[SiteDeleting] = jsonFormat1(SiteDeleting.apply)

  implicit val logEventFormat: RootJsonFormat[LogEvent] = jsonFormat3(LogEvent.apply)

  implicit val healthFormat: RootJsonFormat[HealthMessage] = jsonFormat6(HealthMessage.apply)

}

