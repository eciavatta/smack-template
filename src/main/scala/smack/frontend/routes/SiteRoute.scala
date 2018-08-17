package smack.frontend.routes

import akka.actor.ActorRef
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.util.Timeout
import smack.common.mashallers.Marshalling
import smack.frontend.server.RestRoute
import smack.frontend.server.ValidationDirective._
import smack.models.messages._

class SiteRoute(val backendRouter: ActorRef)(implicit val requestTimeout: Timeout) extends RestRoute with Marshalling {

  override def route: Route =
    pathPrefix("sites") {
      pathEndOrSingleSlash {
        post {
          entity(as[CreateSiteRequest]) { req =>
            handle(req, (res: CreateSiteResponse) => res.site)
          }
        }
      }
    } ~
    pathPrefix("users" / Segment) { id =>
      pathEndOrSingleSlash {
        get {
          handle(FindUserRequest(id), (res: FindUserResponse) => res.user)
        } ~
        put {
          entity(as[UpdateUserRequest]) { req =>
            handle(req, (res: UpdateUserResponse) => res.user)
          }
        }
      }
    }
}
