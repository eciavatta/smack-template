package smack.controllers

import akka.actor.{ActorRef, ActorSystem}
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.testkit.TestKitBase
import com.fasterxml.uuid.Generators
import com.typesafe.config.{Config, ConfigFactory}
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpec}
import smack.backend.controllers.UserController
import smack.commons.mashallers.Marshalling
import smack.commons.utils.{DatabaseUtils, Helpers}
import smack.database.MigrationController
import smack.database.migrations.{CreateUsersByCredentialsTable, CreateUsersByIdTable}
import smack.frontend.routes.UserRoute
import smack.frontend.server.ValidationDirective.ModelValidationRejection
import smack.models.Events.{UserCreating, UserUpdating}
import smack.models.structures.User

class UserControllerSpec extends WordSpec with ScalatestRouteTest with TestKitBase with BeforeAndAfterAll with Matchers with Marshalling {

  implicit lazy val config: Config = ConfigFactory.load("test")
  val migrationController: MigrationController = MigrationController.createMigrationController(system, Seq(CreateUsersByIdTable, CreateUsersByCredentialsTable))

  lazy val userController: ActorRef = system.actorOf(UserController.props, UserController.name)
  lazy val userRoute: Route = UserRoute(userController).route

  override def createActorSystem(): ActorSystem = ActorSystem("userControllerSpec", config)

  override def beforeAll(): Unit = {
    migrationController.migrate(createKeyspace = true)
  }

  override def afterAll(): Unit = {
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
