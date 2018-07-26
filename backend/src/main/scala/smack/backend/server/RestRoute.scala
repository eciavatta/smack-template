package smack.backend.server

import akka.actor.ActorSystem
import akka.http.scaladsl.model.HttpRequest
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route

abstract class RestRoute {

  implicit def actorSystem: ActorSystem

  def route: Route = extractRequest { req =>
    implicit val request: HttpRequest = req
    internalRoute
  }

  protected def internalRoute(implicit request: HttpRequest): Route

}
