package smack.common.traits

import akka.actor.Actor
import akka.util.Timeout

import scala.concurrent.duration.{Duration, FiniteDuration}

trait AskTimeout {
  this: Actor =>

  implicit val askTimeout: Timeout = {
    val t = context.system.settings.config.getString("akka.actor.ask.timeout")
    val d = Duration(t)
    FiniteDuration(d.length, d.unit)
  }

}
