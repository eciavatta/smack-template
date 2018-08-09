package smack.frontend.server

import akka.actor.ActorRef
import akka.http.scaladsl.marshalling.ToResponseMarshallable
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.pattern.ask
import akka.util.Timeout
import smack.common.serialization.MessageSerializer.{RequestMessage, ResponseMessage}

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

  protected def notImplemented: Route = complete(StatusCodes.NotImplemented)

  protected def handle[M >: RequestMessage, N <: ResponseMessage]
  (request: M, responseMapping: N => ToResponseMarshallable)
  (implicit c: ClassTag[N]): Route =
    onComplete(backendRouter.ask(request)(requestTimeout, backendRouter).mapTo[N]) {
      case Success(m) => jsonResponse(m, responseMapping)
      case Failure(e) => throw e
    }

  private def jsonResponse[N <: ResponseMessage]
  (responseMessage: ResponseMessage, responseMapping: N => ToResponseMarshallable): Route = {
    val statusCode = StatusCodes.getForKey(responseMessage.statusCode)
    mapResponse(res => res.copy(status = statusCode.getOrElse(StatusCodes.InternalServerError),
      entity = res.entity.withContentType(ContentTypes.`application/json`))) {
      complete(responseMapping(responseMessage.asInstanceOf[N]))
    }
  }
}
