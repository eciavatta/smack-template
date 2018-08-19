package smack.common.traits

import akka.actor.{Actor, ActorLogging}
import smack.models.structures.ResponseStatus

trait Controller {
  this: Actor with ActorLogging =>

  import Controller._

  private val OK: Int = 200
  private val Created: Int = 201
  private val Accepted: Int = 202
  private val BadRequest: Int = 400
  private val NotFound: Int = 404
  private val InternalServerError: Int = 500
  private val ServiceUnavailable: Int = 503

  protected def success(): Option[ResponseStatus] = successResponse(OK)
  protected def created(): Option[ResponseStatus] = successResponse(Created)
  protected def accepted(): Option[ResponseStatus] = successResponse(Accepted)
  protected def notFound(): Option[ResponseStatus] = failureResponse(NotFound, None, None)
  protected def badRequest(message: Option[String] = None): Option[ResponseStatus] = failureResponse(BadRequest, message, None)
  protected def serviceUnavailable(): Option[ResponseStatus] = failureResponse(ServiceUnavailable, None, None)
  protected def internalServerError(throwable: Throwable, message: Option[String] = None): Option[ResponseStatus] =
    failureResponse(InternalServerError, message, Some(throwable))

  protected final def responseRecovery(throwable: Throwable): Option[ResponseStatus] = throwable match {
    case NotFoundException => notFound()
    case BadRequestException(message) => badRequest(message)
    case ServiceUnavailableException => serviceUnavailable()
    case InternalServerErrorException(_throwable, message) => internalServerError(_throwable, message)
    case other => customRecovery(other)
  }

  protected def customRecovery: Throwable => Option[ResponseStatus] = { ex =>
    log.error(ex, ex.getMessage)
    internalServerError(ex)
  }

  private def successResponse(statusCode: Int) = Some(ResponseStatus(statusCode))

  private def failureResponse(statusCode: Int, message: Option[String], throwable: Option[Throwable]) =
    Some(ResponseStatus(statusCode, message.fold("")(identity), throwable.fold("")(_.getMessage),
      throwable.fold(Seq.empty[String])(_.getStackTrace.map(_.toString).toSeq)))

}

object Controller {

  case object NotFoundException extends Throwable
  case class BadRequestException(message: Option[String]) extends Throwable
  case class InternalServerErrorException(throwable: Throwable, message: Option[String] = None) extends Throwable
  case object ServiceUnavailableException extends Throwable

}
