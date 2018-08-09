package smack.common.traits

import akka.actor.Actor

import scala.concurrent.ExecutionContext

trait ContextDispatcher {
  this: Actor =>

  implicit val executionContext: ExecutionContext = context.dispatcher

}
