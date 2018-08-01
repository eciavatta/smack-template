package smack.backend.routes

import akka.actor.ActorSystem
import akka.http.scaladsl.model.{HttpRequest, StatusCodes}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import smack.backend.controllers.UserController
import smack.backend.marshallers.ModelMarshalling
import smack.backend.server.RestRoute
import smack.backend.server.ValidationDirective._
import smack.backend.validation.{EmailRule, StringLengthRule}
import smack.model.UserCreated

class MessageRoute(implicit val actorSystem: ActorSystem) extends RestRoute with ModelMarshalling {

  private val controller = new UserController
  private val minUsernameLength = 6

  override protected def internalRoute(implicit request: HttpRequest): Route =
    pathPrefix("users") {
      pathEndOrSingleSlash {
        get {
          onSuccess(controller.list()) { list =>
            complete((StatusCodes.OK, list))
          }
        } ~
        post {
          entity(as[UserCreated]) { user =>
            validateModel(user, EmailRule("email"), StringLengthRule("username", minUsernameLength)) { validatedUser =>
              onSuccess(controller.create(validatedUser)) {
                case true => complete((StatusCodes.Accepted, "Ok"))
                case false => complete((StatusCodes.BadRequest, "Already present"))
              }
            }
          }
        }
      }
    }
}
