package smack.frontend.server

import akka.actor.ActorRef
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.marshalling.ToResponseMarshallable
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.pattern.ask
import akka.util.Timeout
import com.typesafe.config.Config
import smack.common.mashallers.Marshalling
import smack.common.serialization.MessageSerializer.{RequestMessage, ResponseMessage}
import smack.common.utils.{Converters, Helpers}
import smack.models.structures.ResponseStatus

import scala.reflect.ClassTag
import scala.util.{Failure, Success}

abstract class RestRoute extends Marshalling {

  def route: Route

  protected def backendRouter: ActorRef

  protected implicit def config: Config

  protected def notImplemented: Route = complete(StatusCodes.NotImplemented)

  protected def handle[M >: RequestMessage, N <: ResponseMessage](request: M, responseMapping: N => ToResponseMarshallable)(implicit c: ClassTag[N]): Route =
    onComplete(backendRouter.ask(request)(requestTimeout, backendRouter).mapTo[N]) {
      case Success(m) => jsonResponse(m, responseMapping)
      case Failure(e) => throw e
    }

  private implicit val requestTimeout: Timeout = Converters.toScalaDuration(config, "akka.http.server.request-timeout")

  private def jsonResponse[N <: ResponseMessage](responseMessage: ResponseMessage, responseMapping: N => ToResponseMarshallable): Route =
    responseMessage.responseStatus.fold(mapResponse(setStatusCode(StatusCodes.InternalServerError.intValue))(complete("Invalid response message"))) {
      case ResponseStatus(statusCode, _, _, _) if StatusCodes.getForKey(statusCode).isEmpty =>
        mapResponse(setStatusCode(statusCode))(complete("Invalid response status"))
      case ResponseStatus(statusCode, message, _, _) if statusFor(statusCode).isSuccess() && message.isEmpty =>
        mapResponse(setStatusCode(statusCode, isJson = true))(complete(responseMapping(responseMessage.asInstanceOf[N])))
      case ResponseStatus(statusCode, message, _, _) if statusFor(statusCode).isSuccess() =>
        mapResponse(setStatusCode(statusCode))(complete(message))
      case ResponseStatus(statusCode, message, cause, _) if !Helpers.isDebugEnabled(config) || cause.isEmpty =>
        mapResponse(setStatusCode(statusCode))(complete(if (message.isEmpty) statusFor(statusCode) else message))
      case responseStatus: ResponseStatus => mapResponse(setStatusCode(responseStatus.statusCode, isJson = true))(complete(responseStatus))
    }

  private def statusFor(statusCode: Int): StatusCode = StatusCodes.getForKey(statusCode).get

  private def setStatusCode(statusCode: Int, isJson: Boolean = false)(response: HttpResponse): HttpResponse = response.copy(
    status = statusFor(statusCode),
    entity = response.entity.withContentType(if (isJson) ContentTypes.`application/json` else ContentTypes.`text/plain(UTF-8)`)
  )

}
