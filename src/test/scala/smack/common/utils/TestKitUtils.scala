package smack.common.utils

import com.typesafe.config.{Config, ConfigFactory}

object TestKitUtils {

  val config: Config = ConfigFactory.parseString("""
    akka {
      actor.ask.timeout = 3 s
      loglevel = "INFO"
    }
    """).withFallback(ConfigFactory.load("serialization"))

}
