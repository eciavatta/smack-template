package smack.frontend.routes

import akka.actor.ActorRef
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import com.typesafe.config.Config
import smack.frontend.server.RestRoute
import smack.frontend.server.ValidationDirective._
import smack.frontend.validation.ValidationRules._
import smack.models.Events.{UserCreating, UserUpdating}
import smack.models.messages._

case class UserRoute(backendRouter: ActorRef)(implicit val config: Config) extends RestRoute {

  private val minPasswordLength = 6

  override def route: Route =
    pathPrefix("users") {
      pathEndOrSingleSlash {
        post {
          entity(as[UserCreating]) { userCreating =>
            validateModel(userCreating, EmailRule("email"), StringLengthRule("password", minPasswordLength)) { valid =>
              handle(CreateUserRequest(valid.email, valid.password, valid.fullName), (res: CreateUserResponse) => res.user)
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
          entity(as[UserUpdating]) { userUpdating =>
            handle(UpdateUserRequest(id, userUpdating.fullName), (res: UpdateUserResponse) => res.user)
          }
        }
      }
    }
}
