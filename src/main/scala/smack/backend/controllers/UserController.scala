package smack.backend.controllers

import akka.actor.{Actor, ActorLogging, Props}
import com.datastax.driver.core.{BatchStatement, SimpleStatement}
import com.fasterxml.uuid.Generators
import com.typesafe.config.Config
import smack.backend.controllers.UserController.EmailAlreadyExistsException
import smack.cassandra.CassandraDatabase.CassandraStatement
import smack.commons.traits.CassandraController
import smack.commons.traits.Controller.NotFoundException
import smack.commons.utils.{Converters, Helpers}
import smack.models.messages._
import smack.models.structures.{Date, ResponseStatus, User}

import scala.collection.JavaConverters._
import scala.concurrent.Future
import scala.util.{Failure, Success}

class UserController extends Actor with CassandraController with ActorLogging {

  private implicit val config: Config = Helpers.actorConfig

  override def receive: Receive = {
    case FindUserRequest(id) =>
      val _sender = sender()
      findUser(id).onComplete {
        case Success(user) => _sender ! FindUserResponse(success(), Some(user))
        case Failure(ex) => _sender ! FindUserResponse(responseRecovery(ex))
      }
    case CreateUserRequest(email, password, fullName) =>
      val _sender = sender()
      createUser(email, password, fullName).onComplete {
        case Success(user) => _sender ! CreateUserResponse(success(), Some(user))
        case Failure(ex) => _sender ! CreateUserResponse(responseRecovery(ex))
      }
    case UpdateUserRequest(id, fullName) =>
      val _sender = sender()
      updateUser(id, fullName).onComplete {
        case Success(user) => _sender ! UpdateUserResponse(success(), Some(user))
        case Failure(ex) => _sender ! UpdateUserResponse(responseRecovery(ex))
      }
    }

  protected override def customRecovery: Throwable => Option[ResponseStatus] = {
    case EmailAlreadyExistsException => badRequest(Helpers.getError("emailAlreadyExists"))
    case ex =>
      log.error(ex, ex.getMessage)
      internalServerError(ex)
  }

  private def findUser(id: String): Future[User] = Future
    .fromTry(convertToUUID(id))
    .map { uuid =>
      CassandraStatement(new SimpleStatement("SELECT id, email, full_name, toTimestamp(id) as registered_date FROM users_by_id WHERE id = ?;", uuid))
    }
    .flatMap(executeQuery)
    .map { resultSet =>
      resultSet.all().asScala.headOption.fold(throw NotFoundException) { row =>
        User(id = row.getUUID("id").toString,
          email = row.getString("email"),
          fullName = row.getString("full_name"),
          registeredDate = Some(Date(row.getTimestamp("registered_date").getTime))
        )
      }
    }

  private def createUser(email: String, password: String, fullName: String): Future[User] = executeQuery(
    CassandraStatement(new SimpleStatement("SELECT COUNT(*) FROM users_by_credentials WHERE email = ?;", email))
  ) .map(resultSet => if (resultSet.one().getLong(0) > 0) throw EmailAlreadyExistsException)
    .flatMap { _ =>
      val uuid = Generators.timeBasedGenerator().generate()
      val queries = Seq(
        new SimpleStatement("INSERT INTO users_by_id (id, email, full_name) VALUES (?, ?, ?);", uuid, email, fullName),
        new SimpleStatement("INSERT INTO users_by_credentials (email, password, user_id) VALUES (?, ?, ?);", email, password, uuid)
      )
      executeQuery(CassandraStatement(new BatchStatement().addAll(queries.asJava)))
        .map(_ => User(uuid.toString, email, fullName, Some(Date(Converters.getTimeFromUUID(uuid)))))
    }

  private def updateUser(id: String, fullName: String): Future[User] = findUser(id)
    .flatMap { user =>
      executeQuery(
        CassandraStatement(new SimpleStatement("UPDATE users_by_id SET full_name = ? WHERE id = ?;", fullName, convertToUUID(id).get))
      ).map (_ => user.copy(fullName = fullName))
    }

}

object UserController {
  def props: Props = Props(new UserController)
  def name: String = "userController"

  case object EmailAlreadyExistsException extends Throwable
}
