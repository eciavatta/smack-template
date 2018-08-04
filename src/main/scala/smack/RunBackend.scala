package smack

import akka.actor.{ActorSystem, Props}
import akka.cluster.seed.ZookeeperClusterSeed
import com.typesafe.config.ConfigFactory
import smack.backend.Backend

object RunBackend extends App {

  val config = ConfigFactory.load("application")
  implicit val system: ActorSystem = ActorSystem(config.getString("name"), config)
  ZookeeperClusterSeed(system).join()

  system.actorOf(Props[Backend], name = "backendWorker")

}
