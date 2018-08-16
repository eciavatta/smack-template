package smack.cassandra

import akka.Done
import akka.actor.{Actor, ActorRef, Props}
import akka.event.Logging
import akka.stream.scaladsl.{Flow, GraphDSL, RunnableGraph, Sink, Source, SourceQueueWithComplete}
import akka.stream.{AbruptStageTerminationException, ClosedShape, OverflowStrategy}
import com.datastax.driver.core.{Cluster, ResultSet, Session, Statement}
import smack.cassandra.CassandraDatabase._
import smack.cassandra.ScalaConverters._
import smack.common.traits.{AskTimeout, ContextDispatcher, ImplicitMaterializer, ImplicitSerialization}
import smack.common.utils.Helpers
import smack.models.TestException
import smack.models.messages.GenerateException

import scala.collection.JavaConverters._
import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

class CassandraDatabase(keySpace: String) extends Actor
  with ImplicitMaterializer with ImplicitSerialization with AskTimeout with ContextDispatcher {

  private val log = Logging(context.system, context.self)
  private val config = Helpers.actorConfig.getConfig("smack.cassandra")

  private val cassandraCluster: Cluster = Cluster.builder
    .addContactPoint(config.getString("contact-point.host"))
    .withPort(config.getInt("contact-point.port"))
    .build()

  private implicit var cassandraSession: Session = _
  private var queue: SourceQueueWithComplete[(CassandraMessage, ActorRef)] = _

  override def preStart(): Unit = {
    cassandraSession = cassandraCluster.connect(keySpace)
    queue = createCassandraGraph().run()
  }

  override def postStop(): Unit = {
    queue.complete()
    cassandraSession.close()
  }

  override def receive: Receive = {
    case ex: GenerateException => throw TestException(ex.message)
    case message: CassandraMessage => queue.offer((message, sender))
  }

  private def sourceQueue: Source[(CassandraMessage, ActorRef), SourceQueueWithComplete[(CassandraMessage, ActorRef)]] =
    Source.queue[(CassandraMessage, ActorRef)](config.getInt("buffer-size"), OverflowStrategy.backpressure)

  private def watchTermination = Flow[(CassandraMessage, ActorRef)].watchTermination() { (sourceQueue, futureDone) =>
    futureDone.onComplete {
      case Success(_) => log.debug(s"Cassandra database stream for keySpace $keySpace is closed.")
      case Failure(_: AbruptStageTerminationException) => log.debug(s"Cassandra database stream for keySpace $keySpace is abruptly terminated.")
      case Failure(ex) => log.error(ex, ex.getMessage)
    }
    sourceQueue
  }

  private def executeQueryStatement = Flow[(CassandraMessage, ActorRef)].mapAsync(config.getInt("execute-parallelism")) { c =>
    val result = c._1 match {
      case CassandraQuery(query) => cassandraSession.executeAsync(query).asScala
      case CassandraQuery(query, values @ _*) => cassandraSession.prepareAsync(query).asScala
        .map(_.bind(values.map(_.asInstanceOf[AnyRef]): _*)).map(cassandraSession.execute(_))
      case CassandraQueryMap(query, values) => cassandraSession.executeAsync(query, values.mapValues(_.asInstanceOf[AnyRef]).asJava).asScala
    }

    result.map(resultSet => (CassandraResult(Success(resultSet)), c._2)).recover {
      case ex => (CassandraResult(Failure(ex)), c._2)
    }
  }

  private def sendResponse: Sink[(CassandraResult, ActorRef), Future[Done]] =
    Sink.foreach[(CassandraResult, ActorRef)](c => c._2 ! c._1)

  private def createCassandraGraph(): RunnableGraph[SourceQueueWithComplete[(CassandraMessage, ActorRef)]] = RunnableGraph.fromGraph({
    GraphDSL.create(sourceQueue) {
      implicit builder => queueShape =>
        import GraphDSL.Implicits._

        queueShape ~> watchTermination ~> executeQueryStatement ~> sendResponse
        ClosedShape
    }
  })

}

object CassandraDatabase {

  def props(keySpace: String): Props = Props(new CassandraDatabase(keySpace))
  def name(keySpace: String): String = s"cassandraDatabase-$keySpace"

  sealed trait CassandraMessage

  case class CassandraQuery(query: String, values: Any*) extends CassandraMessage
  case class CassandraQueryMap(query: String, values: Map[String, Any]) extends CassandraMessage
  case class CassandraStatement(statement: Statement) extends CassandraMessage
  case class CassandraResult(resultSet: Try[ResultSet])

}
