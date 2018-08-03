package smack.frontend.server

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.event.Logging
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server._
import akka.stream.ActorMaterializer
import akka.util.Timeout
import com.typesafe.config.Config
import smack.frontend.routes.RegisteredRoutes
import smack.frontend.server.ValidationDirective._

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.util.{Failure, Success}

class WebServer(private val system: ActorSystem, private val config: Config) {

  private implicit val actorSystem: ActorSystem = system
  private implicit val materializer: ActorMaterializer = ActorMaterializer()
  private implicit val ec: ExecutionContext = system.dispatcher

  private val logger = Logging(system, config.getString("name"))
  var host: String = config.getString("http.host")
  var port: Int = config.getInt("http.port")
  private var binding: Future[Http.ServerBinding] = Future.never

  private implicit val requestTimeout: Timeout = requestTimeout(config)

  private implicit val frontendRouter: ActorRef = system.actorOf(Props.empty, name = "workerRouter")

  private implicit def myRejectionHandler: RejectionHandler = RejectionHandler.newBuilder()
    .handle {
      case mvr@ModelValidationRejection(_) => buildBadRequestResponse(mvr.invalidFields.toJson.toString)
      case vr: ValidationRejection => buildBadRequestResponse(vr.message)
      case mrcr: MalformedRequestContentRejection => buildBadRequestResponse(mrcr.message)
    }.handleNotFound {
    logRequest(complete((StatusCodes.NotFound, "Not found")))
  }.result()

  def start(): Unit = {
    if (binding != Future.never) throw new IllegalStateException("Webserver already started")
    val route = concat(RegisteredRoutes.getRegisteredRoutes.map(_.route): _*)
    binding = Http().bindAndHandle(logRequest(route), host, port)
    binding.onComplete {
      case Success(bind) => logger.info(s"Webserver bound to ${bind.localAddress}")
      case Failure(e) => logger.error("Webserver bounding error: " + e.getMessage)
    }
  }

  private implicit def myExceptionHandler: ExceptionHandler = ExceptionHandler {
    case ex => extractRequest { req =>
      // implicit val request: HttpRequest = req
      complete(HttpResponse(StatusCodes.InternalServerError, entity = "There was an internal server error."))
    }
  }

  private def logRequest(innerRoutes: => Route) = extractRequestContext { ctx =>
    val time = System.currentTimeMillis()
    extractClientIP { ip =>
      mapResponse { res =>
        logger.info(s"${ctx.request.protocol.value} ${ctx.request.method.value} ${ctx.request.uri.path}" +
          s" | ${res.status.value} | IP: $ip | Time: ${calcMillis(time)}")
        res
      }(innerRoutes)
    }
  }

  private def calcMillis(from: Long) = s"${System.currentTimeMillis() - from} ms"

  def stop(): Unit = {
    if (binding == Future.never) throw new IllegalStateException("Webserver not active")
    val onceAllConnectionsTerminated = Await.result(binding, 10.seconds).terminate(hardDeadline = 3.seconds)

    onceAllConnectionsTerminated.flatMap { _ =>
      system.terminate()
    }
  }

  private def buildBadRequestResponse(message: String) = logRequest(complete(HttpResponse(
    StatusCodes.BadRequest, entity = HttpEntity(message).withContentType(ContentTypes.`application/json`))))

  private def requestTimeout(config: Config): Timeout = {
    val t = config.getString("akka.http.server.request-timeout")
    val d = Duration(t)
    FiniteDuration(d.length, d.unit)
  }

}
