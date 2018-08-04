package smack

import akka.actor.{ActorSystem, DeadLetter, Props}
import akka.cluster.seed.ZookeeperClusterSeed
import com.typesafe.config.ConfigFactory
import smack.backend.Backend

object RunBackend extends App {

  val config = ConfigFactory.load("application")
  implicit val system: ActorSystem = ActorSystem(config.getString("name"), config)
  ZookeeperClusterSeed(system).join()

  val backend = system.actorOf(Props[Backend], name = "backendWorker")
  system.eventStream.subscribe(backend, classOf[DeadLetter])

}
