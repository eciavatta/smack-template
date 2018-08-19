package smack.common.traits

import java.util.UUID

import akka.actor.{Actor, ActorLogging}
import akka.pattern.{AskTimeoutException, ask}
import com.datastax.driver.core.ResultSet
import com.typesafe.config.Config
import smack.cassandra.CassandraDatabase._
import smack.common.traits.Controller.{BadRequestException, InternalServerErrorException, ServiceUnavailableException}
import smack.common.utils.Helpers

import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

trait CassandraController extends Controller with AskTimeout with ContextDispatcher {
  this: Actor with ActorLogging =>

  private val databaseRef = Helpers.createCassandraDatabaseActor()
  private implicit val config: Config = Helpers.actorConfig

  protected def convertToUUID(uuid: String): Try[UUID] = Try(UUID.fromString(uuid))
    .transform(Success(_), _ => Failure(BadRequestException(Helpers.getError("badUUID"))))

  protected def executeQuery(cassandraMessage: CassandraMessage): Future[ResultSet] =
    databaseRef.ask(cassandraMessage).mapTo[CassandraResult].transform {
      case Success(CassandraResult(tryResult)) => tryResult match {
        case Success(result) => Success(result)
        case Failure(ex) =>
          log.error(ex, ex.getMessage)
          Failure(InternalServerErrorException(ex))
      }
      case Failure(_: AskTimeoutException) => Failure(ServiceUnavailableException)
      case Failure(ex) =>
        log.error(ex, ex.getMessage)
        Failure(InternalServerErrorException(ex))
    }

}
