package smack.backend.controllers

import akka.actor.ActorSystem
import akka.http.scaladsl.model.HttpRequest
import smack.backend.server.Controller

import scala.concurrent.{ExecutionContext, Future}

class ExceptionController(implicit val system: ActorSystem) extends Controller[Throwable, Unit] {

  private implicit val ec: ExecutionContext = system.dispatcher

  def handle(throwable: Throwable)(implicit request: HttpRequest): Future[Unit] = Future {
    throwable.printStackTrace()
  }

}
