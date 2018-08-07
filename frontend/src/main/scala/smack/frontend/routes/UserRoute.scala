package smack.frontend.routes

import akka.actor.ActorRef
import akka.http.scaladsl.model.HttpRequest
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.util.Timeout
import smack.frontend.marshallers.ModelMarshalling
import smack.frontend.server.RestRoute
import smack.frontend.server.ValidationDirective._
import smack.frontend.validation.ValidationRules._
import smack.models.messages._
import smack.models.structures.User

class UserRoute(val backendRouter: ActorRef)
               (implicit val requestTimeout: Timeout) extends RestRoute with ModelMarshalling {

  private val minUsernameLength = 6

  override protected def internalRoute(implicit request: HttpRequest): Route =
    pathPrefix("users") {
      pathEndOrSingleSlash {
        get {
          handle(GetUsersRequest(), (g: GetUsersResponse) => Some(g.users))
        } ~
          post {
            entity(as[CreateUserRequest]) { user =>
              validateModel(user, EmailRule("email"), StringLengthRule("username", minUsernameLength)) {
                req => handle(req, (r: CreateUserResponse) => r.user)
              }
            }
          }
      }
    } ~ pathPrefix("users" / LongNumber) { id =>
      pathEndOrSingleSlash {
        get {
          handle(GetUserRequest(id), (g: GetUserResponse) => g.user)
        } ~
          delete {
            handle(DeleteUserRequest(id), (_: DeleteUserResponse) => Option.empty[User])
          }
      }
    }
}
