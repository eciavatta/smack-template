package smack.client

import java.util.concurrent.atomic.{AtomicLong, AtomicReference}

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Props}
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.marshalling.Marshal
import akka.http.scaladsl.model.{ContentTypes, HttpMethods, HttpRequest, RequestEntity}
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.scaladsl.{Keep, Sink, Source}
import akka.stream.{AbruptStageTerminationException, OverflowStrategy, QueueOfferResult}
import smack.client.WebClient.{BenchTimes, GetStatistics, Statistics, WorkerFailure}
import smack.commons.mashallers.Marshalling
import smack.commons.traits.{ContextDispatcher, ImplicitMaterializer}
import smack.models.Events.{LogEvent, SiteCreating, UserCreating}
import smack.models.structures.{Site, User}

import scala.collection.mutable
import scala.concurrent.duration._
import scala.util.{Failure, Random, Success}

class LogWorker(parent: ActorRef, host: String, port: Int, requestsPerSecond: Int) extends Actor with ActorLogging
  with ContextDispatcher with ImplicitMaterializer with Marshalling {

  import LogWorker._

  private implicit val system: ActorSystem = context.system

  private val queueSize = requestsPerSecond * 10
  private val randomLength = 8

  private val responseStatistics = mutable.Map[Int, Long]().withDefaultValue(0)
  private val enqueuedCounter = new AtomicLong()
  private val droppedCounter = new AtomicLong()
  private val failedCounter = new AtomicLong()
  private val benchTimes = new AtomicReference(BenchTimes())

  override def preStart(): Unit = {
    val userName = randomString(randomLength)
    Marshal(UserCreating(s"$userName@example.com", randomString(randomLength), userName)).to[RequestEntity] flatMap {
      entity => Http().singleRequest(
        HttpRequest(uri = s"http://$host:$port/users", method = HttpMethods.POST, entity = entity.withContentType(ContentTypes.`application/json`)))
    } flatMap { createResponse => Unmarshal(createResponse.entity).to[User]
    } flatMap { user => Marshal(SiteCreating(user.id, s"$userName.com")).to[RequestEntity]
    } flatMap { entity => Http().singleRequest(
      HttpRequest(uri = s"http://$host:$port/sites", method = HttpMethods.POST, entity = entity.withContentType(ContentTypes.`application/json`)))
    } flatMap { createResponse => Unmarshal(createResponse.entity).to[Site]
    } map { site => (Marshal(LogEvent("/", "127.0.0.1", "LogWorker")).to[RequestEntity], site)
    } onComplete {
      case Success((eventMarshal, site)) => eventMarshal.onComplete {
        case Success(logEvent) => system.scheduler.schedule(100.millis, 1.second, self,
          ExecuteRequest(
            HttpRequest(method = HttpMethods.POST, uri = s"/logs/${site.trackingId}", entity = logEvent.withContentType(ContentTypes.`application/json`))))
        case Failure(ex) => terminate(ex) }
      case Failure(ex) => terminate(ex)
    }
  }

  override def receive: Receive = {
    case GetStatistics =>
      sender ! Statistics(responseStatistics.toMap, enqueuedCounter.get, droppedCounter.get, failedCounter.get, benchTimes.get)
      responseStatistics.clear()
      enqueuedCounter.set(0)
      droppedCounter.set(0)
      failedCounter.set(0)
      benchTimes.set(BenchTimes())

    case ExecuteRequest(request) => for (_ <- 1 to requestsPerSecond) queueRequest(request)
  }

  private val queue = Source.queue[HttpRequest](queueSize, OverflowStrategy.dropNew)
    .watchTermination() { (message, futureDone) =>
      futureDone.onComplete {
        case Success(_) => log.debug(s"Queue terminated successfully")
        case Failure(_: AbruptStageTerminationException) =>
        case Failure(ex) => terminate(ex)
      }
      message
    }
    .map {req => req -> System.currentTimeMillis()}
    .via(Http().cachedHostConnectionPool[Long](host, port))
    .toMat(Sink.foreach {
      case (Success(response), start) =>
        updateTimes(start)
        responseStatistics(response.status.intValue) += 1
      case (Failure(_), start) =>
        updateTimes(start)
        failedCounter.incrementAndGet()
    })(Keep.left)
    .run()

  private def queueRequest(request: HttpRequest): Unit = {
    queue.offer(request).map {
      case QueueOfferResult.Enqueued    => enqueuedCounter.incrementAndGet()
      case QueueOfferResult.Dropped     => droppedCounter.incrementAndGet()
      case QueueOfferResult.Failure(ex)  => terminate(ex)
      case QueueOfferResult.QueueClosed => droppedCounter.incrementAndGet()
    }
  }

  private def randomString(length: Int): String = Random.alphanumeric.take(length).mkString

  private def updateTimes(start: Long): Unit = {
    val time = System.currentTimeMillis() - start
    benchTimes.updateAndGet { bt =>
      var result = bt
      if (time < result.min) result = result.copy(min = time)
      if (time > result.max) result = result.copy(max = time)
      result.copy(sum = result.sum + time)
    }
  }

  private def terminate(throwable: Throwable): Unit = {
    log.error(throwable, throwable.getMessage)
    parent ! WorkerFailure
    context.stop(self)
  }

}

object LogWorker {

  val defaultPort: Int = 80
  val defaultRequestsPerSecond: Int = 20

  def props(parent: ActorRef, host: String, port: Int = defaultPort, requestsPerSecond: Int = defaultRequestsPerSecond): Props =
    Props(new LogWorker(parent, host, port, requestsPerSecond))
  def name: String = "logWorker"

  case class ExecuteRequest(request: HttpRequest)

}
