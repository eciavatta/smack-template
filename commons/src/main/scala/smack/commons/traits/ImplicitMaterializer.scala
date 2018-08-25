package smack.commons.traits

import akka.actor.Actor
import akka.stream.ActorMaterializer

trait ImplicitMaterializer {
  this: Actor =>

  protected implicit val actorMaterializer: ActorMaterializer = ActorMaterializer()

}
