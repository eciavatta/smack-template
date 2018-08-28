package smack.backend.services

import akka.Done
import akka.actor.{Actor, ActorLogging, Props}
import com.datastax.driver.core.SimpleStatement
import smack.cassandra.CassandraDatabase.CassandraStatement
import smack.commons.traits.CassandraController
import smack.kafka.KafkaConsumer
import smack.models.messages._

import scala.concurrent.Future
import scala.util.{Failure, Success}

class LogService extends Actor with CassandraController with ActorLogging {

  private val topic = "logs"
  private val consumerGroup = "service-consumer"
  context.actorOf(KafkaConsumer.props(topic, consumerGroup, self), KafkaConsumer.name(topic, consumerGroup))

  override def receive: Receive = {
    case traceLogRequest: TraceLogRequest =>
      val _sender = sender()
      traceLog(traceLogRequest).onComplete {
      case Success(done) => _sender ! Success(done)
      case Failure(ex) => _sender ! Failure(ex)
    }
  }

  private def traceLog(traceLogRequest: TraceLogRequest): Future[Done] = Future
    .fromTry(convertToUUID(traceLogRequest.id))
    .map{ siteId =>
      CassandraStatement(new SimpleStatement("INSERT INTO logs (site_id, log_id, url, ip_address, user_agent) VALUES (?, NOW(), ?, ?, ?);",
        siteId, traceLogRequest.url, traceLogRequest.ipAddress, traceLogRequest.userAgent))
    }
    .flatMap(executeQuery)
    .map(_ => Done)

}

object LogService {
  def props: Props = Props(new LogService)
  def name: String = "logService"
}
