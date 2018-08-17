package smack.frontend.routes

import akka.actor.ActorRef
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.util.Timeout
import smack.common.mashallers.Marshalling
import smack.frontend.server.RestRoute
import smack.frontend.server.ValidationDirective._
import smack.frontend.validation.ValidationRules._
import smack.models.messages._

class LogRoute(val backendRouter: ActorRef)(implicit val requestTimeout: Timeout) extends RestRoute with Marshalling {

  private val minPasswordLength = 6

  override def route: Route =
    pathPrefix("users") {
      pathEndOrSingleSlash {
        post {
          entity(as[CreateUserRequest]) { user =>
            validateModel(user, EmailRule("email"), StringLengthRule("password", minPasswordLength)) { req =>
              handle(req, (res: CreateUserResponse) => res.user)
            }
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
