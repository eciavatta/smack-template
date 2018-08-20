package smack.controllers

import akka.actor.{ActorRef, ActorSystem}
import akka.event.Logging
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.testkit.TestKitBase
import com.fasterxml.uuid.Generators
import com.typesafe.config.{Config, ConfigFactory}
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpec}
import smack.backend.BackendSupervisor
import smack.common.mashallers.Marshalling
import smack.common.utils.Helpers
import smack.commons.utils.DatabaseUtils
import smack.database.MigrationController
import smack.database.migrations.{CreateUsersByCredentialsTable, CreateUsersByIdTable}
import smack.frontend.routes.UserRoute
import smack.frontend.routes.UserRoute._
import smack.frontend.server.ValidationDirective.ModelValidationRejection
import smack.models.structures.User

class UserControllerSpec extends WordSpec with ScalatestRouteTest with TestKitBase with BeforeAndAfterAll with Matchers with Marshalling {

  lazy implicit val config: Config = ConfigFactory.load("test")
  lazy val log = Logging(system, this.getClass.getName)
  val migrationController: MigrationController = MigrationController.createMigrationController(system, Seq(CreateUsersByIdTable, CreateUsersByCredentialsTable))

  lazy val backendSupervisor: ActorRef = system.actorOf(BackendSupervisor.props, BackendSupervisor.name)
  lazy val userRoute: Route = UserRoute(backendSupervisor).route

  protected override def createActorSystem(): ActorSystem = ActorSystem("userControllerSpec", config)

  protected override def beforeAll(): Unit = {
    migrationController.migrate(createKeyspace = true)
  }

  protected override def afterAll(): Unit = {
    DatabaseUtils.dropTestKeyspace()
    shutdown()
  }

  "user request controller" should {

    "fail when finding invalid or non existing users" in {
      Get("/users/malformedUUID") ~> userRoute ~> check {
        status shouldEqual StatusCodes.BadRequest
        responseAs[String] shouldEqual Helpers.getError("badUUID").get
      }

      Get(s"/users/${Generators.timeBasedGenerator().generate().toString}") ~> userRoute ~> check {
        status shouldEqual StatusCodes.NotFound
      }
    }

    "create valid user and find it" in {
      var user: Option[User] = None

      Post("/users", UserCreating("valid@example.com", "foobar", "testUser"))~> userRoute ~> check {
        status shouldEqual StatusCodes.OK
        user = responseAs[Option[User]]
      }

      Get(s"/users/${user.get.id}") ~> userRoute ~> check {
        status shouldEqual StatusCodes.OK
        responseAs[Option[User]] shouldEqual user
      }
    }

    "fail when trying to create an invalid user" in {
      Post("/users", UserCreating("invalidEmail", "foobar", "testUser"))~> userRoute ~> check {
        rejection shouldBe a[ModelValidationRejection]
        rejection.asInstanceOf[ModelValidationRejection].invalidFields.head.name shouldEqual "email"
      }

      Post("/users", UserCreating("valid@example.com", "foo", "testUser"))~> userRoute ~> check {
        rejection shouldBe a[ModelValidationRejection]
        rejection.asInstanceOf[ModelValidationRejection].invalidFields.head.name shouldEqual "password"
      }
    }

    "fail when trying to create an user with an already existing email" in {
      Post("/users", UserCreating("valid@example.com", "barfoo", "testUser"))~> userRoute ~> check {
        status shouldEqual StatusCodes.BadRequest
        responseAs[String] shouldEqual Helpers.getError("emailAlreadyExists").get
      }
    }

    "update a valid user" in {
      var user: Option[User] = None

      Post("/users", UserCreating("valid2@example.com", "foobar", "wrongName"))~> userRoute ~> check {
        status shouldEqual StatusCodes.OK
        user = responseAs[Option[User]]
      }

      Put(s"/users/${user.get.id}", UserUpdating(fullName = "correctName")) ~> userRoute ~> check {
        status shouldEqual StatusCodes.OK
        responseAs[Option[User]] shouldEqual Some(user.get.copy(fullName = "correctName"))
      }

      Get(s"/users/${user.get.id}") ~> userRoute ~> check {
        status shouldEqual StatusCodes.OK
        responseAs[Option[User]] shouldEqual Some(user.get.copy(fullName = "correctName"))
      }
    }

    "fail when trying to update an invalid user" in {
      Put("/users/malformedUUID", UserUpdating(fullName = "testUser")) ~> userRoute ~> check {
        status shouldEqual StatusCodes.BadRequest
        responseAs[String] shouldEqual Helpers.getError("badUUID").get
      }

      Put(s"/users/${Generators.timeBasedGenerator().generate().toString}", UserUpdating(fullName = "testUser")) ~> userRoute ~> check {
        status shouldEqual StatusCodes.NotFound
      }
    }

  }

}
