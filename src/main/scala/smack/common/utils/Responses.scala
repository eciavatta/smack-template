package smack.common.utils

import smack.models.structures.ResponseStatus

object Responses {

  private val OK: Int = 200
  private val Created: Int = 201
  private val Accepted: Int = 202
  private val BadRequest: Int = 400
  private val NotFound: Int = 404
  private val InternalServerError: Int = 500

  def success(): Option[ResponseStatus] = successResponse(OK)
  def created(): Option[ResponseStatus] = successResponse(Created)
  def accepted(): Option[ResponseStatus] = successResponse(Accepted)
  def notFound(message: Option[String] = None, throwable: Option[Throwable] = None): Option[ResponseStatus] = failureResponse(NotFound, message, throwable)
  def badRequest(message: Option[String] = None, throwable: Option[Throwable] = None): Option[ResponseStatus] = failureResponse(BadRequest, message, throwable)
  def internalServerError(message: Option[String] = None, throwable: Option[Throwable] = None): Option[ResponseStatus] =
    failureResponse(InternalServerError, message, throwable)

  private def successResponse(statusCode: Int) = Some(ResponseStatus(statusCode))
  private def failureResponse(statusCode: Int, message: Option[String], throwable: Option[Throwable]) =
    Some(ResponseStatus(statusCode, message.fold("")(identity), throwable.fold("")(_.getMessage),
      throwable.fold(Seq.empty[String])(_.getStackTrace.map(_.toString).toSeq)))

}
