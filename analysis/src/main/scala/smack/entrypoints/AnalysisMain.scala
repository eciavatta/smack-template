package smack.entrypoints

import java.io.File

import io.sentry.Sentry
import org.apache.spark.{SparkConf, SparkContext}
import scopt.OptionParser
import smack.analysis.AnalysisSupervisor
import smack.commons.utils.JarProvider

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
      .setMaster(params.master)
      .set("spark.jars.packages", "datastax:spark-cassandra-connector:2.3.0-s_2.11")
      .set("spark.cassandra.connection.host", cassandraRegex.group(1))
      .set("spark.cassandra.connection.port", cassandraRegex.group(2))
      .set("spark.driver.extraJavaOptions", params.sentryDns.fold(log4jParam)(_ => log4jSentryParam))
      .set("spark.executor.extraJavaOptions", params.sentryDns.fold(log4jParam)(_ => log4jSentryParam))
      .setSparkHome(params.sparkHome)

    if (params.jarProviderBinding.isDefined) {
      val jarProviderRegex = addressPattern.findFirstMatchIn(params.jarProviderBinding.get).get
      new JarProvider(params.jarFile, jarProviderRegex.group(2).toInt)
      conf.set("spark.jars", s"http://${params.jarProviderBinding.get}/")
    }

    if (params.jarExternalPath.isDefined) {
      conf.set("spark.jars", params.jarExternalPath.get)
    }

    if (params.sentryDns.isDefined) {
      Sentry.init(params.sentryDns.get)
    }

    val sparkContext = new SparkContext(conf)
    sparkContext.setLogLevel(params.logLevel)

    val analysisSupervisor = new AnalysisSupervisor(sparkContext, params.startFrom)
    analysisSupervisor.startScheduler()
  }

  private def argumentParser: OptionParser[AnalysisParams] = new scopt.OptionParser[AnalysisParams]("smack-analysis") {
    // head(BuildInfo.name, BuildInfo.version)

    opt[String]('m', "master").required()
      .action((master, config) => config.copy(master = master))
      .text("...")

    opt[String]('c', "cassandra-bootstrap").optional()
      .action((cassandra, config) => config.copy(cassandra = cassandra))
      .validate(cassandra => addressPattern.findFirstIn(cassandra).fold(failure("invalid cassandra address"))(_ => success))
      .text("...")

    opt[String]("spark-home").optional()
      .action((sparkHome, config) => config.copy(sparkHome = sparkHome))
      .text("...")

    opt[String]('k', "keyspace").optional()
      .action((keyspace, config) => config.copy(keyspace = keyspace))
      .text("...")

    opt[String]('l',"loglevel").optional()
      .action((level, config) => config.copy(logLevel = level))
      .validate(level => if (Seq("error", "warning", "info", "debug", "off").contains(level.toLowerCase)) success else failure("undefined loglevel"))
      .text("...")

    opt[String]("sentry-dns").optional()
      .action((dns, config) => config.copy(sentryDns = Some(dns)))
      .text("...")

    opt[String]("jar-provider-binding").optional()
      .action((jarProvider, config) => config.copy(jarProviderBinding = Some(jarProvider)))
      .validate(jarProvider => addressPattern.findFirstIn(jarProvider).fold(failure("invalid jar provider address"))(_ => success))
      .text("...")

    opt[File]("jar").optional().valueName("<jar>")
      .action((jarFile, config) => config.copy(jarFile = jarFile))
      .text("...")

    opt[String]("jar-external-path").optional()
      .action((jarExternalPath, config) => config.copy(jarExternalPath = Some(jarExternalPath)))
      .text("...")

    opt[Long]("start-from").optional().valueName("<timestamp>")
      .action((startFrom, config) => config.copy(startFrom = startFrom))
      .text("...")

    note("...")
  }

  private def checkAndGetConfig(args: Array[String]): AnalysisParams = argumentParser.parse(args, AnalysisParams()) match {
    case Some(config) => config
    case None => sys.exit(1)
  }

  case class AnalysisParams(cassandra: String = "127.0.0.1:9042", keyspace: String = "smackdev", logLevel: String = "INFO", master: String = "",
                            sentryDns: Option[String] = None, sparkHome: String = "/opt/spark", jarProviderBinding: Option[String] = None,
                            jarFile: File = new File("/app/"), jarExternalPath: Option[String] = None, startFrom: Long = System.currentTimeMillis())

}

