package smack.backend.routes

import java.net.InetAddress

import akka.actor.ActorSystem
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._ // important
import akka.http.scaladsl.model.HttpRequest
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import smack.backend.marshallers.ModelMarshalling
import smack.backend.server.RestRoute

class HealthRoute(implicit val actorSystem: ActorSystem) extends RestRoute with ModelMarshalling {

  override protected def internalRoute(implicit request: HttpRequest): Route =
    pathPrefix("health") {
      pathEndOrSingleSlash {
        get {
          extractClientIP { ip =>
            complete(HealthMessage(request.protocol.value, request.method.value,
              request.uri.path.toString, ip.getAddress.toString, InetAddress.getLocalHost.getHostName,
              InetAddress.getLocalHost.getHostAddress))
          }
        }
      }
    }
}

case class HealthMessage(protocol: String, method: String, uri: String, clientIp: String, hostname: String, hostIp: String)
