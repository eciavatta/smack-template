package smack.frontend.routes

import akka.actor.{Actor, ActorLogging, ActorRef}
import akka.http.scaladsl.marshalling.ToResponseMarshallable
import akka.http.scaladsl.model.{HttpRequest, StatusCodes}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.util.Timeout
import smack.frontend.marshallers.ModelMarshalling
import smack.frontend.server.RestRoute
import smack.frontend.server.ValidationDirective._
import smack.frontend.validation._
import smack.model._

import scala.util.{Failure, Success}

class UserRoute(val backendRouter: ActorRef)
               (implicit val requestTimeout: Timeout) extends RestRoute with ModelMarshalling {

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

class ActorTest extends Actor with ActorLogging {
  override def receive: Receive = {
    case a: Any => log.info(a.toString)
  }
}

