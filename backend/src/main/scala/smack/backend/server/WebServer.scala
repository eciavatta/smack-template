package smack.backend.server

import akka.actor.ActorSystem
import akka.event.Logging
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{ExceptionHandler, RejectionHandler, ValidationRejection}
import akka.stream.ActorMaterializer
import com.typesafe.config.ConfigFactory
import smack.backend.controllers.ExceptionController
import smack.backend.routes.RegisteredRoutes
import smack.backend.server.ValidationDirective._

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.util.{Failure, Success}

class WebServer(implicit val system: ActorSystem, implicit val materializer: ActorMaterializer) {

  private implicit val ec: ExecutionContext = system.dispatcher
  private val config = ConfigFactory.load()
  private val logger = Logging(system, config.getString("name"))
  var host: String = config.getString("http.host")
  var port: Int = config.getInt("http.port")
  private var binding: Future[Http.ServerBinding] = Future.never
  private val exceptionController = new ExceptionController()

  private implicit def myRejectionHandler: RejectionHandler = RejectionHandler.newBuilder()
    .handle { case mvr @ ModelValidationRejection(_) =>
      complete(HttpResponse(StatusCodes.BadRequest, entity = HttpEntity(mvr.invalidFields.toJson.toString)
        .withContentType(ContentTypes.`application/json`)))
    }.handle { case vr: ValidationRejection =>
      complete(HttpResponse(StatusCodes.BadRequest, entity = HttpEntity(vr.message)
        .withContentType(ContentTypes.`application/json`)))
    }.handleNotFound {
      complete((StatusCodes.NotFound, "Not found"))
    }.result()

  private implicit def myExceptionHandler: ExceptionHandler = ExceptionHandler {
    case ex => extractRequest { req =>
      implicit val request: HttpRequest = req
      onSuccess(exceptionController.handle(ex)) {
        complete(HttpResponse(StatusCodes.InternalServerError, entity = "There was an internal server error."))
      }
    }
  }

  def start(): Unit = {
    if (binding != Future.never) throw new IllegalStateException("Webserver already started")
    val route = concat(RegisteredRoutes.getRegisteredRoutes.map(_.route): _*)
    binding = Http().bindAndHandle(route, host, port)
    binding.onComplete {
      case Success(bind) => logger.info(s"Webserver bound to ${bind.localAddress}")
      case Failure(e) => logger.error("Webserver bounding error: " + e.getMessage)
    }
  }

  def stop(): Unit = {
    if (binding == Future.never) throw new IllegalStateException("Webserver not active")
    val onceAllConnectionsTerminated = Await.result(binding, 10.seconds).terminate(hardDeadline = 3.seconds)

    onceAllConnectionsTerminated.flatMap { _ =>
      system.terminate()
    }
  }

}
