package smack.frontend.routes

import java.net.InetAddress

import akka.actor.ActorRef
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import com.typesafe.config.Config
import smack.frontend.server.RestRoute
import smack.models.HealthMessage

case class HealthRoute(backendRouter: ActorRef)(implicit val config: Config) extends RestRoute {

  override def route: Route =
    pathPrefix("health") {
      pathEndOrSingleSlash {
        get {
          extractClientIP { ip =>
            extractRequest { request =>
              complete(StatusCodes.OK -> HealthMessage(request.protocol.value, request.method.value,
                request.uri.path.toString, ip.getAddress.toString, InetAddress.getLocalHost.getHostName,
                InetAddress.getLocalHost.getHostAddress))
            }
          }
        }
      }
    }
}
