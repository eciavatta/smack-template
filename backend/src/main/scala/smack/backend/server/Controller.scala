package smack.backend.server

import akka.actor.ActorSystem
import akka.http.scaladsl.model.HttpRequest

import scala.concurrent.Future

trait Controller[I, O] {

  def handle(instance: I)(implicit request: HttpRequest): Future[O]

  protected def system: ActorSystem

}
