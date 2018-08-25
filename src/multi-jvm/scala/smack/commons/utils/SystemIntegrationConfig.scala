package smack.commons.utils

import akka.actor.ActorSystem
import akka.remote.testconductor.RoleName
import akka.remote.testkit.MultiNodeConfig
import com.typesafe.config.{Config, ConfigFactory}

object SystemIntegrationConfig {

  def actorConfig(roleName: String): Config =
    ConfigFactory.parseResources("test.conf").withFallback(ConfigFactory.parseResources(s"$roleName.conf")).resolve()

  object SystemIntegrationMultiNodeConfig extends MultiNodeConfig {
    val backend: RoleName = role("backend")
    val frontend: RoleName = role("frontend")
    val service: RoleName = role("service")
  }

  def createActorSystem(roleName: String): Config => ActorSystem =
    superConfig => ActorSystem("BackendFrontendItMultiJvmBackend", superConfig.withFallback(actorConfig(roleName)))

}
