package smack.backend.controllers

import java.util.UUID
import java.util.concurrent.TimeUnit

import akka.Done
import akka.actor.{Actor, ActorLogging, Props}
import com.datastax.driver.core.SimpleStatement
import com.google.common.cache.CacheBuilder
import com.typesafe.config.Config
import smack.cassandra.CassandraDatabase.CassandraStatement
import smack.commons.traits.Controller.NotFoundException
import smack.commons.traits.KafkaController
import smack.commons.utils.Helpers
import smack.commons.traits.{CassandraController, KafkaController}
import smack.models.messages._

import scala.collection.JavaConverters._
import scala.concurrent.Future
import scala.util.{Failure, Success}

class LogController extends Actor with KafkaController with CassandraController with ActorLogging {

  private implicit val config: Config = Helpers.actorConfig

  private lazy val trackingIdCache = CacheBuilder.newBuilder()
    .expireAfterWrite(cacheDuration.getSeconds, TimeUnit.SECONDS)
    .maximumSize(config.getLong("smack.cache.logs.maximum-size"))
    .build[UUID, UUID]()

  private val cacheDuration = config.getDuration("smack.cache.logs.expire-after")

  override protected def kafkaTopic: String = "logs"

  override def receive: Receive = {
    case traceLogRequest: TraceLogRequest =>
      val _sender = sender()
      traceLog(traceLogRequest).onComplete {
        case Success(_) => _sender ! TraceLogResponse(accepted())
        case Failure(ex) => _sender ! TraceLogResponse(responseRecovery(ex))
      }
    }

  private def traceLog(traceLogRequest: TraceLogRequest): Future[Done] = Future
    .fromTry(convertToUUID(traceLogRequest.id))
    .map{ trackingId =>
      Option(trackingIdCache.getIfPresent(trackingId)).fold(
        findSiteId(trackingId).map(siteId => {trackingIdCache.put(trackingId, siteId); siteId}))(
        Future(_))
    }.flatten
    .map { siteId =>
      sendToKafka(traceLogRequest.copy(id = siteId.toString))
    }.flatten

  private def findSiteId(trackingId: UUID): Future[UUID] = executeQuery(
    CassandraStatement(new SimpleStatement("SELECT site_id FROM sites_by_tracking_id WHERE tracking_id = ?", trackingId)))
    .map { resultSet =>
      resultSet.all().asScala.headOption.fold(throw NotFoundException) { row =>
        row.getUUID("site_id")
      }
    }
}

object LogController {
  def props: Props = Props(new LogController)
  def name: String = "logController"
}
