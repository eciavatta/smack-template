package smack.backend.server

import akka.actor.ActorSystem
import akka.http.scaladsl.model.HttpRequest

import scala.concurrent.Future

trait EmptyController {
  protected def system: ActorSystem
}

trait Controller[I, O] extends EmptyController {
  def handle(instance: I)(implicit request: HttpRequest): Future[O]
}
