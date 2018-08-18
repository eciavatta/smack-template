package smack.frontend.routes

import akka.actor.ActorRef
import com.typesafe.config.Config
import smack.frontend.server.RestRoute

object RegisteredRoutes {

  def getRegisteredRoutes(backendRouter: ActorRef)(implicit config: Config): Seq[RestRoute] = Seq(
    UserRoute(backendRouter),
    SiteRoute(backendRouter),
    LogRoute(backendRouter),
    HealthRoute(backendRouter)
  )

}
