package smack.frontend.routes

import java.net.InetAddress

import akka.actor.ActorRef
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.util.Timeout
import smack.frontend.server.RestRoute
import smack.models.HealthMessage
import smack.common.mashallers.Marshalling

class HealthRoute(val backendRouter: ActorRef)(implicit val requestTimeout: Timeout) extends RestRoute with Marshalling {

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
