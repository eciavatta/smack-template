package smack.common.utils

import com.typesafe.config.Config

import scala.concurrent.duration.{Duration, FiniteDuration}

object Converters {

  def toScalaDuration(config: Config, path: String): FiniteDuration = {
    val t = config.getString(path)
    val d = Duration(t)
    Duration(d.length, d.unit)
  }

}
