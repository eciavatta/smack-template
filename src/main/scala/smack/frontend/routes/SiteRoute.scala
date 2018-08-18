package smack.frontend.routes

import akka.actor.ActorRef
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import com.typesafe.config.Config
import smack.frontend.server.RestRoute
import smack.frontend.server.ValidationDirective._
import smack.models.messages._

case class SiteRoute(backendRouter: ActorRef)(implicit val config: Config) extends RestRoute {

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
