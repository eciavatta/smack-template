package smack.entrypoints

import akka.actor.ActorSystem
import com.typesafe.config.ConfigFactory
import scopt.OptionParser
import smack.BuildInfo
import smack.commons.abstracts.EntryPoint
import smack.database.MigrationController

import scala.util.{Failure, Success}

object MigrateMain extends EntryPoint[MigrateMain] {

  def main(args: Array[String]): Unit = {
    val params = checkAndGetConfig(args, MigrateMain())

    val cassandraRegex = addressPattern.findFirstMatchIn(params.cassandra).get

    val config = ConfigFactory.parseString(
      s"""
         |${params.debug.fold("")(debug => s"smack.debug = ${if (debug) "on" else "off"}")}
         |akka.loggers = ["$defaultLogger"${if (params.sentryDns.isDefined) ", \"" + sentryLogger + "\"" else ""}]
         |akka.loglevel = "${params.logLevel.toUpperCase}"
         |smack.cassandra.contact-point.host = "${cassandraRegex.group(1)}"
         |smack.cassandra.contact-point.port = ${cassandraRegex.group(2)}
         |smack.sentry.dns = "${params.sentryDns.fold("")(identity)}"
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

    system.terminate()
  }

  override protected def argumentParser: OptionParser[MigrateMain] = new scopt.OptionParser[MigrateMain](BuildInfo.name) {
    head(BuildInfo.name, BuildInfo.version)

    opt[String]('c', "cassandra-bootstrap").optional()
      .action((cassandra, config) => config.copy(cassandra = cassandra))
      .validate(cassandra => addressPattern.findFirstIn(cassandra).fold(failure("invalid cassandra address"))(_ => success))
      .text("...")

    opt[Unit]("create-keyspace").optional()
      .action((_, config) => config.copy(createKeyspace = true))
      .text("...")

    opt[Boolean]('d', "debug").optional()
      .action((debug, config) => config.copy(debug = Some(debug)))
      .text("...")

    opt[String]('e', "environment").optional()
      .action((environment, config) => config.copy(environment = environment))
      .validate(environment => if (Seq("development", "production").contains(environment)) success else failure("undefined environment"))
      .text("...")

    opt[Boolean]("force")
      .action((force, config) => config.copy(force = force))
      .text("...")

    opt[String]('l',"loglevel").optional()
      .action((level, config) => config.copy(logLevel = level))
      .validate(level => if (Seq("error", "warning", "info", "debug", "off").contains(level.toLowerCase)) success else failure("undefined loglevel"))
      .text("...")

    opt[String]("sentry-dns").optional()
      .action((dns, config) => config.copy(sentryDns = Some(dns)))
      .text("...")

    cmd("rollback")
      .action((_, c) => c.copy(isRollback = true))
      .children(
        opt[Int]("steps").optional()
          .action((steps, config) => config.copy(rollbackSteps = steps))
          .validate(steps => if (steps > 0) success else failure("steps must be greater than 0"))
          .text("...")
      )
      .text("...")

    cmd("reset")
      .action((_, c) => c.copy(isReset = true))
      .text("...")

    note("...")
  }

}

case class MigrateMain(cassandra: String = "127.0.0.1:9042", createKeyspace: Boolean = false, debug: Option[Boolean] = None,
                               environment: String = "development", force: Boolean = false, isReset: Boolean = false, isRollback: Boolean = false,
                               logLevel: String = "INFO", rollbackSteps: Int = 1, sentryDns: Option[String] = None)
