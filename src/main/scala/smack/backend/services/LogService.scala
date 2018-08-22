package smack.backend.services

import akka.Done
import akka.actor.{Actor, ActorLogging, Props}
import akka.pattern.pipe
import com.datastax.driver.core.SimpleStatement
import smack.cassandra.CassandraDatabase.CassandraStatement
import smack.common.traits.CassandraController
import smack.kafka.KafkaConsumer
import smack.models.messages._

import scala.concurrent.Future

class LogService extends Actor with CassandraController with ActorLogging {

  private val topic = "logs"
  private val consumerGroup = "log-group"
  context.actorOf(KafkaConsumer.props(topic, consumerGroup, self), KafkaConsumer.name(topic, consumerGroup))

  override def receive: Receive = {
    case traceLogRequest: TraceLogRequest => traceLog(traceLogRequest) pipeTo sender()
  }

  private def traceLog(traceLogRequest: TraceLogRequest): Future[Done] = Future
    .fromTry(convertToUUID(traceLogRequest.id))
    .map{ siteId =>
      CassandraStatement(new SimpleStatement("INSERT INTO logs (site_id, log_id, url, ip_address, user_agent) VALUES (?, NOW(), ?, ?, ?);",
        siteId, traceLogRequest.url, traceLogRequest.ipAddress, traceLogRequest.userAgent))
    }
    .map(executeQuery)
    .flatten
    .map(_ => Done)

}

object LogService {
  def props: Props = Props(new LogService)
  def name: String = "logService"
}
