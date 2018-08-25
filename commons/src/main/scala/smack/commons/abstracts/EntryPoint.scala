package smack.commons.abstracts

import scopt.OptionParser

import scala.util.matching.Regex

abstract class EntryPoint[C] {

  protected val defaultLogger: String = "akka.event.Logging$DefaultLogger"
  protected val sentryLogger: String = "smack.commons.utils.SentryLogger"

  protected val addressPattern: Regex = "^([\\w-\\.]{3,}):(\\d{3,5})$".r
  protected val zooPattern: Regex = "^([\\w-\\.]{3,}):(\\d{3,5})(\\/\\w+)$$".r

  protected def checkAndGetConfig(args: Array[String], emptyConfig: C): C = argumentParser.parse(args, emptyConfig) match {
    case Some(config) => config
    case None => sys.exit(1)
  }

  protected def argumentParser: OptionParser[C]

}
