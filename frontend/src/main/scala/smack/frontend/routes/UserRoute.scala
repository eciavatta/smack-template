package smack.frontend.routes

import akka.actor.ActorRef
import akka.http.scaladsl.marshalling.ToResponseMarshallable
import akka.http.scaladsl.model.HttpRequest
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.util.Timeout
import smack.frontend.marshallers.ModelMarshalling
import smack.frontend.server.RestRoute
import smack.frontend.server.ValidationDirective._
import smack.frontend.validation._
import smack.model._

class UserRoute(implicit val backendRouter: ActorRef,
                implicit val requestTimeout: Timeout) extends RestRoute with ModelMarshalling {

  private val minUsernameLength = 6

  override protected def internalRoute(implicit request: HttpRequest): Route =
    pathPrefix("users") {
      pathEndOrSingleSlash {
        get {
          makeResponse(GetUsersRequest, (g: GetUsersResponse) => g.users)
        } ~
          post {
            entity(as[UserCreated]) { user =>
              validateModel(user, EmailRule("email"), StringLengthRule("username", minUsernameLength)) { validatedUser =>
                makeResponse(AddUserRequest(validatedUser), (g: AddUserResponse) => g.message.getOrElse("OK").asInstanceOf[ToResponseMarshallable])
              }
            }
          }
      }
    }
}

