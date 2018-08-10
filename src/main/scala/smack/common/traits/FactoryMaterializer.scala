package smack.common.traits

import akka.actor.Actor
import akka.stream.ActorMaterializer

trait FactoryMaterializer {
  this: Actor =>

  protected implicit val actorMaterializer: ActorMaterializer = ActorMaterializer()

}
