package smack.frontend.routes

import akka.actor.ActorRef
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import com.typesafe.config.Config
import smack.frontend.server.RestRoute
import smack.models.messages.{TraceLogRequest, TraceLogResponse}
import spray.json.{DefaultJsonProtocol, RootJsonFormat}

case class LogRoute(backendRouter: ActorRef)(implicit val config: Config) extends RestRoute {
  import LogRoute._

  override def route: Route =
    pathPrefix("logs" / Segment) { trackingId =>
      pathEndOrSingleSlash {
        post {
          entity(as[LogEvent]) { log =>
            handle(TraceLogRequest(trackingId, log.url, log.ipAddress, log.userAgent), (_: TraceLogResponse) => Option.empty[String])
          }
        }
      }
    }
}

object LogRoute extends DefaultJsonProtocol {

  case class LogEvent(url: String, ipAddress: String, userAgent: String)

  implicit val logEventFormat: RootJsonFormat[LogEvent] = jsonFormat3(LogEvent.apply)

}
