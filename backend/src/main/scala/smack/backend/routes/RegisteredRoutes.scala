package smack.backend.routes

import akka.actor.ActorSystem
import smack.backend.server.RestRoute

object RegisteredRoutes {

  def getRegisteredRoutes(implicit actorSystem: ActorSystem): Seq[RestRoute] = Seq(
    new MessageRoute,
    new UserRoute,
    new HealthRoute
  )

}
