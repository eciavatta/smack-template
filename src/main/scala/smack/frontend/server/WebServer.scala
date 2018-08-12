package smack.frontend.server

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.event.Logging
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server._
import akka.pattern.AskTimeoutException
import akka.routing.FromConfig
import akka.stream.ActorMaterializer
import akka.util.Timeout
import com.typesafe.config.Config
import smack.common.utils.Converters
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
  private val host: String = config.getString("akka.http.server.host")
  private val port: Int = config.getInt("akka.http.server.port")
  private var binding: Future[Http.ServerBinding] = Future.never

  private implicit val requestTimeout: Timeout = Converters.toScalaDuration(config, "akka.http.server.request-timeout")
  private val backendRouter: ActorRef = system.actorOf(FromConfig.props(Props.empty), name = "backendRouter")

  private implicit def myRejectionHandler: RejectionHandler = RejectionHandler.newBuilder()
    .handle {
      case mvr@ModelValidationRejection(_) => buildBadRequestResponse(mvr.invalidFields.toJson.toString)
      case vr: ValidationRejection => buildBadRequestResponse(vr.message)
      case mrcr: MalformedRequestContentRejection => buildBadRequestResponse(mrcr.message)
    }.handleNotFound {
    routeWithDirectives(complete(StatusCodes.NotFound))
  }.result()

  def start(): Unit = {
    if (binding != Future.never) throw new IllegalStateException("Webserver already started")
    val route: Route = concat(RegisteredRoutes.getRegisteredRoutes(backendRouter).map(_.route): _*)
    binding = Http().bindAndHandle(routeWithDirectives(route), host, port)
    binding.onComplete {
      case Success(bind) => logger.info(s"Webserver bound to ${bind.localAddress}")
      case Failure(e) => logger.error(e, e.getMessage)
    }
  }

  private implicit def myExceptionHandler: ExceptionHandler = ExceptionHandler {
    case ex: AskTimeoutException =>
      logger.error(ex.getMessage)
      routeWithDirectives(routeWithDirectives(complete(StatusCodes.ServiceUnavailable)))
    case ex =>
      logger.error(ex, ex.getMessage)
      routeWithDirectives(routeWithDirectives(complete(StatusCodes.InternalServerError)))
  }

  private def routeWithDirectives(route: Route): Route = {
    withRequestLogging {
      withServerAddressHeader(s"$host:$port") {
        route
      }
    }
  }

  private def withRequestLogging: Directive0 =
    if (config.getBoolean("akka.http.server.log-server-requests.enabled")) {
      extractRequestContext.flatMap { ctx =>
        val time = System.currentTimeMillis()
        extractClientIP.flatMap { ip =>
          mapResponse { res =>
            logger.info(s"${ctx.request.protocol.value} ${ctx.request.method.value} ${ctx.request.uri.path}" +
              s" | ${res.status.value} | IP: $ip | Time: ${calcMillis(time)}")
            res
          }
        }
      }
    } else {
      Directive.Empty
    }

  private def calcMillis(from: Long) = s"${System.currentTimeMillis() - from} ms"

  private def withServerAddressHeader(serverAddress: String): Directive0 =
    if (config.getBoolean("akka.http.server.address-server-header.enabled")) {
      respondWithHeaders(RawHeader("X-Server-Address", serverAddress))
    } else {
      Directive.Empty
    }

  def stop(): Unit = {
    if (binding == Future.never) throw new IllegalStateException("Webserver not active")
    val onceAllConnectionsTerminated = Await.result(binding, 10.seconds).terminate(hardDeadline = 3.seconds)

    onceAllConnectionsTerminated.flatMap { _ =>
      system.terminate()
    }
  }

  private def buildBadRequestResponse(message: String): Route = routeWithDirectives(complete(HttpResponse(
    StatusCodes.BadRequest, entity = HttpEntity(message).withContentType(ContentTypes.`application/json`))))

}
