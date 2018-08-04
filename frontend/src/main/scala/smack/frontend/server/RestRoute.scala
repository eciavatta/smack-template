package smack.frontend.server

import akka.actor.ActorRef
import akka.http.scaladsl.marshalling.ToResponseMarshallable
import akka.http.scaladsl.model.{HttpRequest, StatusCodes}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.pattern.ask
import akka.util.Timeout

import scala.reflect.ClassTag
import scala.util.{Failure, Success}

abstract class RestRoute {

  implicit def backendRouter: ActorRef

  implicit def requestTimeout: Timeout

  def route: Route = extractRequest { req =>
    implicit val request: HttpRequest = req
    internalRoute
  }

  protected def internalRoute(implicit request: HttpRequest): Route

  protected def makeResponse[M, N: ClassTag](request: M, response: N => ToResponseMarshallable): Route =
    onComplete(backendRouter.ask(request)(requestTimeout, backendRouter).mapTo[N]) {
      case Success(m) => complete(response(m))
      case Failure(e) => complete((StatusCodes.ServiceUnavailable, e.getMessage))
    }

  protected def notImplemented: Route = complete(StatusCodes.NotImplemented)

}
