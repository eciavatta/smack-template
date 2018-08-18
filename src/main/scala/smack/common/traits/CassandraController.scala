package smack.common.traits

import java.util.UUID

import akka.actor.{Actor, ActorLogging, ActorRef}
import akka.pattern.ask
import com.datastax.driver.core.{ResultSet, Statement}
import com.typesafe.config.Config
import smack.cassandra.CassandraDatabase._
import smack.common.serialization.MessageSerializer.ResponseMessage
import smack.common.utils.{Helpers, Responses}
import smack.models.structures.ResponseStatus

import scala.util.{Failure, Success, Try}

trait CassandraController {
  this: Actor with ActorLogging with AskTimeout with ContextDispatcher =>

  private val databaseRef = Helpers.createCassandraDatabaseActor()
  private implicit val config: Config = Helpers.actorConfig

  protected def executeQuery(sender: ActorRef, query: String, values: Any*)
                            (onComplete: Either[Option[ResponseStatus], ResultSet] => ResponseMessage): Unit =
    sendCassandraMessage(sender, CassandraQuery(query, values))(onComplete)

  protected def executeQueryMap(sender: ActorRef, query: String, values: Map[String, Any])
                               (onComplete: Either[Option[ResponseStatus], ResultSet] => ResponseMessage): Unit =
    sendCassandraMessage(sender, CassandraQueryMap(query, values))(onComplete)

  protected def executeStatement(sender: ActorRef, statement: Statement)
                                (onComplete: Either[Option[ResponseStatus], ResultSet] => ResponseMessage): Unit =
    sendCassandraMessage(sender, CassandraStatement(statement))(onComplete)

  protected def parseUUID(uuid: String): Either[Option[ResponseStatus], UUID] = Try(UUID.fromString(uuid)) match {
    case Success(_uuid) => Right(_uuid)
    case Failure(_) => Left(Responses.badRequest(Helpers.getError("badUUID")))
  }

  private def sendCassandraMessage(sender: ActorRef, cassandraMessage: CassandraMessage)
                                  (onComplete: Either[Option[ResponseStatus], ResultSet] => ResponseMessage): Unit = {
    databaseRef.ask(cassandraMessage).mapTo[CassandraResult].onComplete {
      case Success(CassandraResult(tryResult)) => tryResult match {
        case Success(result) => sender ! onComplete(Right(result))
        case Failure(ex) =>
          sender ! onComplete(Left(Responses.internalServerError(throwable = Some(ex))))
          log.error(ex, ex.getMessage)
      }
      case Failure(ex) =>
        sender ! onComplete(Left(Responses.internalServerError(throwable = Some(ex))))
        log.error(ex, ex.getMessage)
    }
  }

}
