package smack.entrypoints

import akka.actor.ActorSystem
import com.typesafe.config.ConfigFactory
import scopt.OptionParser
import smack.commons.abstracts.EntryPoint
import smack.database.MigrationController
import smack.migrate.BuildInfo

import scala.util.{Failure, Success}

object MigrateMain extends EntryPoint[MigrateMain] {

  def main(args: Array[String]): Unit = {
    var params = checkAndGetConfig(args, MigrateMain())

    if (params.cassandraContactPoints.isEmpty) {
      params = params.copy(cassandraContactPoints = Seq("127.0.0.1:9042"))
    }

    val env = params.environment match {
      case "development" => "dev"
      case "production" => "prod"
      case "testing" => "test"
    }

    val config = ConfigFactory.parseString(
      s"""
         |${params.debug.fold("")(debug => s"smack.debug = ${if (debug) "on" else "off"}")}
         |akka.loggers = ["$defaultLogger"${if (params.sentryDns.isDefined) ", \"" + sentryLogger + "\"" else ""}]
         |akka.loglevel = "${params.logLevel.toUpperCase}"
         |smack.cassandra.contact-points = [${params.cassandraContactPoints.map(cassandra => s""""$cassandra"""").mkString(",")}]
         |smack.sentry.dns = "${params.sentryDns.fold("")(identity)}"
         |smack.name = "${BuildInfo.name}-$env"
         |smack.version = "${BuildInfo.version}"
         |smack.scala-version = "${BuildInfo.scalaVersion}"
         |smack.sbt-version = "${BuildInfo.sbtVersion}"
       """.stripMargin)
      .withFallback(ConfigFactory.parseResources(s"commons-${params.environment}.conf")).resolve()

    val system: ActorSystem = ActorSystem(config.getString("smack.name"), config)
    val migrationController = MigrationController.createMigrationController(system)

    if (params.isRollback) {
      val result = migrationController.reset(params.force)
      result match {
        case Success(op) => system.log.info(s"[RESULTS] Reset operation affected $op tables")
        case Failure(ex) => system.log.error(ex, ex.getMessage)
      }
    }
    else if (params.isReset) {
      val result = migrationController.rollback(params.force, params.rollbackSteps)
      result match {
        case Success(op) => system.log.info(s"[RESULTS] Rollback operation affected $op tables")
        case Failure(ex) => system.log.error(ex, ex.getMessage)
      }
    }
    else {
      val result = migrationController.migrate(params.force, params.createKeyspace)
      result match {
        case Success(op) => system.log.info(s"[RESULTS] Migrate operation affected $op tables")
        case Failure(ex) => system.log.error(ex, ex.getMessage)
      }
    }

    import system.dispatcher
    system.terminate().map(_ => sys.exit(0))
  }

  override protected def argumentParser: OptionParser[MigrateMain] = new scopt.OptionParser[MigrateMain](BuildInfo.name) {
    head(BuildInfo.name, BuildInfo.version)

    opt[String]('c', "cassandra-contact-points").optional().unbounded()
      .action((cassandra, config) => config.copy(cassandraContactPoints = config.cassandraContactPoints :+ cassandra))
      .validate(cassandra => addressPattern.findFirstIn(cassandra).fold(failure("invalid cassandra-contact-points address"))(_ => success))
      .valueName("<addr>")
      .text("The cassandra contact point/s (default: 127.0.0.1:9042)")

    opt[Unit]("create-keyspace").optional()
      .action((_, config) => config.copy(createKeyspace = true))
      .text("Create keyspace before start with migration")

    opt[Boolean]('d', "debug").optional()
      .action((debug, config) => config.copy(debug = Some(debug)))
      .valueName("<bool>")
      .text("True if debug should be enabled")

    opt[String]('e', "environment").optional()
      .action((environment, config) => config.copy(environment = environment))
      .validate(environment => if (Seq("development", "production", "testing").contains(environment)) success else failure("undefined environment"))
      .valueName("<env>")
      .text("The environment to be used (default: development)")

    opt[Unit]("force")
      .action((_, config) => config.copy(force = true))
      .text("Must be set in production environments")

    opt[String]('l',"loglevel").optional()
      .action((level, config) => config.copy(logLevel = level))
      .validate(level => if (Seq("error", "warning", "info", "debug", "off").contains(level.toLowerCase)) success else failure("undefined loglevel"))
      .valueName("<level>")
      .text("The log level used by standard output and (optionally) by sentry logger (default: info)")

    opt[String]("sentry-dns").optional()
      .action((dns, config) => config.copy(sentryDns = Some(dns)))
      .valueName("<key>")
      .text("If defined, every logs are sent to sentry servers and can be viewed on Sentry.io. The standard output remain unchanged")

    help("help").text("Display help")

    cmd("rollback")
      .action((_, c) => c.copy(isRollback = true))
      .children(
        opt[Int]("steps").optional()
          .action((steps, config) => config.copy(rollbackSteps = steps))
          .validate(steps => if (steps > 0) success else failure("steps must be greater than 0"))
          .valueName("<num>")
          .text("The number of steps to be rollbacked")
      )
      .text("Execute a rollback of the database schema.")

    cmd("reset")
      .action((_, c) => c.copy(isReset = true))
      .text("Reset the database schema and clean all tables.")

    note("Perform the migration of the database schema.")
  }

}

case class MigrateMain(cassandraContactPoints: Seq[String] = Seq(), createKeyspace: Boolean = false, debug: Option[Boolean] = None,
                       environment: String = "development", force: Boolean = false, isReset: Boolean = false, isRollback: Boolean = false,
                       logLevel: String = "INFO", rollbackSteps: Int = 1, sentryDns: Option[String] = None)
