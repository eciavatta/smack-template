package smack.project

import akka.actor.{ActorRef, ActorSystem}
import akka.event.Logging
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.testkit.{DefaultTimeout, TestKitBase}
import com.datastax.driver.core.utils.UUIDs
import com.typesafe.config.{Config, ConfigFactory}
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpec}
import smack.backend.controllers.UserController
import smack.common.mashallers.Marshalling
import smack.common.utils.Helpers
import smack.commons.utils.DatabaseUtils
import smack.database.MigrationController
import smack.database.migrations.{CreateUsersByCredentialsTable, CreateUsersByIdTable}
import smack.frontend.routes.UserRoute

class UserFlowSpec extends WordSpec with ScalatestRouteTest with TestKitBase with BeforeAndAfterAll with Matchers with Marshalling with DefaultTimeout {

  lazy implicit val config: Config = ConfigFactory.load("test")
  lazy val log = Logging(system, this.getClass.getName)
  val migrationController: MigrationController = MigrationController.createMigrationController(system, Seq(CreateUsersByIdTable, CreateUsersByCredentialsTable))

  lazy val userController: ActorRef = system.actorOf(UserController.props, UserController.name)
  lazy val userRoute: Route = UserRoute(userController).route

  protected override def createActorSystem(): ActorSystem = ActorSystem("userFlowSpec", config)

  protected override def beforeAll(): Unit = {
    migrationController.migrate(createKeyspace = true)
  }

  protected override def afterAll(): Unit = {
    DatabaseUtils.dropTestKeyspace()
    shutdown()
  }

  "user request flow" should {

    "find user if exists" in {

      Get("/users/malformedUUID") ~> userRoute ~> check {
        status shouldEqual StatusCodes.BadRequest
        responseAs[String] shouldEqual Helpers.getError("badUUID").get
      }

      Get(s"/users/${UUIDs.timeBased().toString}") ~> userRoute ~> check {
        status shouldEqual StatusCodes.NotFound
      }

    }

  }

}
