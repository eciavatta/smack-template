package smack.frontend.routes

import akka.actor.ActorRef
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import com.typesafe.config.Config
import smack.frontend.server.RestRoute

case class SiteRoute(backendRouter: ActorRef)(implicit val config: Config) extends RestRoute {

  override def route: Route = path("sites") { notImplemented }
}
