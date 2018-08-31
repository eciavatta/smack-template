package smack.entrypoints

import io.sentry.Sentry
import org.apache.spark.{SparkConf, SparkContext}
import scopt.OptionParser
import smack.analysis.{AnalysisSupervisor, BuildInfo}

import scala.util.matching.Regex

object AnalysisMain {

  private val addressPattern: Regex = "^([\\w-\\.]{3,}):(\\d{3,5})$".r

  def main(args: Array[String]): Unit = {
    val params = checkAndGetConfig(args)

    val cassandraRegex = addressPattern.findFirstMatchIn(params.cassandra).get
    val log4jParam = "-Dlog4j.configuration=log4j.properties"
    val log4jSentryParam = "-Dlog4j.configuration=log4j.properties"

    val conf = new SparkConf(true)
      .setAppName("smack-analysis")
      .set("spark.jars.packages", "datastax:spark-cassandra-connector:2.3.0-s_2.11")
      .set("spark.cassandra.connection.host", cassandraRegex.group(1))
      .set("spark.cassandra.connection.port", cassandraRegex.group(2))
      .set("spark.driver.extraJavaOptions", params.sentryDns.fold(log4jParam)(_ => log4jSentryParam))
      .set("spark.executor.extraJavaOptions", params.sentryDns.fold(log4jParam)(_ => log4jSentryParam))

    if (params.sentryDns.isDefined) {
      Sentry.init(params.sentryDns.get)
    }

    val sparkContext = new SparkContext(conf)
    sparkContext.setLogLevel(params.logLevel)

    val analysisSupervisor = new AnalysisSupervisor(sparkContext, params.keyspace)
    analysisSupervisor.startScheduler()
  }

  private def argumentParser: OptionParser[AnalysisParams] = new scopt.OptionParser[AnalysisParams](BuildInfo.name) {
    head(BuildInfo.name, BuildInfo.version)

    opt[String]('c', "cassandra-bootstrap").required()
      .action((cassandra, config) => config.copy(cassandra = cassandra))
      .validate(cassandra => addressPattern.findFirstIn(cassandra).fold(failure("invalid cassandra address"))(_ => success))
      .text("...")

    opt[String]('k', "keyspace").optional()
      .action((keyspace, config) => config.copy(keyspace = keyspace))
      .text("...")

    opt[String]('l',"loglevel").optional()
      .action((level, config) => config.copy(logLevel = level))
      .validate(level => if (Seq("error", "warn", "info", "debug", "off").contains(level.toLowerCase)) success else failure("undefined loglevel"))
      .text("...")

    opt[String]("sentry-dns").optional()
      .action((dns, config) => config.copy(sentryDns = Some(dns)))
      .text("...")

    note("...")
  }

  private def checkAndGetConfig(args: Array[String]): AnalysisParams = argumentParser.parse(args, AnalysisParams()) match {
    case Some(config) => config
    case None => sys.exit(1)
  }

  case class AnalysisParams(cassandra: String = "", keyspace: String = "smackdev", logLevel: String = "INFO", sentryDns: Option[String] = None)

}

