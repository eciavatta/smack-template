package smack.frontend.routes

import akka.actor.ActorRef
import akka.util.Timeout
import smack.frontend.server.RestRoute

object RegisteredRoutes {

  def getRegisteredRoutes(implicit backendRouter: ActorRef, requestTimeout: Timeout): Seq[RestRoute] = Seq(
    new UserRoute(backendRouter),
    new HealthRoute
  )

}
