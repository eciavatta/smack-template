package smack.common.traits

import akka.actor.Actor
import akka.serialization.{Serialization, SerializationExtension}

trait ImplicitSerialization {
  this: Actor =>

  protected implicit val serialization: Serialization = SerializationExtension(context.system)

}
