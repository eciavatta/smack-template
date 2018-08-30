package smack.client

import java.util.Date

import akka.actor.{Actor, ActorLogging, Props}
import akka.routing.BroadcastPool
import smack.commons.traits.ContextDispatcher
import smack.commons.utils.Helpers

import scala.concurrent.duration._

class WebClient extends Actor with ActorLogging with ContextDispatcher {

  import smack.client.WebClient._
  private val clientConfig = Helpers.actorConfig.getConfig("smack.client")
  private val host = clientConfig.getString("host")
  private val port = clientConfig.getInt("port")
  private val requestsPerSecond = clientConfig.getInt("requests-per-second")
  private val parallelism = clientConfig.getInt("parallelism")
  private val maxCount = clientConfig.getLong("count")
  private val isHttps = clientConfig.getBoolean("https")

  private val logProps = LogWorker.props(self, host, port, requestsPerSecond, isHttps)
  private val logRouter = context.actorOf(BroadcastPool(parallelism).props(logProps), "logRouter")

  private var startTime: Long = _
  private var actorReplies = 0
  private var livingActors = parallelism

  private var responseStatisticsTotal = Map[Int, Long]()
  private var enqueuedTotal: Long = 0
  private var droppedTotal: Long = 0
  private var failedTotal: Long = 0
  private var minTimeTotal: Long = 0
  private var maxTimeTotal: Long = 0
  private var sumTimeTotal: Long = 0

  private var responseStatisticsPartial = Map[Int, Long]()
  private var enqueuedPartial: Long = 0
  private var droppedPartial: Long = 0
  private var failedPartial: Long = 0
  private var minTimePartial: Long = Long.MaxValue
  private var maxTimePartial: Long = 0
  private var sumTimePartial: Long = 0

  override def preStart(): Unit = {
    context.system.scheduler.schedule(5.second, 5.second, self, PrintPartialStatistics)
    startTime = System.currentTimeMillis
    log.info(s"Start collecting statistics at ${new Date(startTime)}")
  }

  override def postStop(): Unit = {
    logStatistics(true)
    log.info(s"Execution time: ${(System.currentTimeMillis - startTime).toDouble / 1000d} seconds. Total requests: ${responseStatisticsTotal.values.sum}")
  }

  override def receive: Receive = {
    case PrintPartialStatistics => logRouter ! GetStatistics
    case PrintTotalStatistics => logStatistics(true)

    case Statistics(responses, enqueued, dropped, failed, bt) =>
      actorReplies += 1

      responseStatisticsTotal = sumLongMaps(responseStatisticsTotal, responses)
      enqueuedTotal += enqueued
      droppedTotal += dropped
      failedTotal += failed
      minTimeTotal = if (bt.min < minTimeTotal) bt.min else minTimeTotal
      maxTimeTotal = if (bt.max > maxTimeTotal) bt.max else maxTimeTotal
      sumTimeTotal += bt.sum

      responseStatisticsPartial = sumLongMaps(responseStatisticsPartial, responses)
      enqueuedPartial += enqueued
      droppedPartial += dropped
      failedPartial += failed
      minTimePartial = if (bt.min < minTimePartial) bt.min else minTimePartial
      maxTimePartial = if (bt.max > maxTimePartial) bt.max else maxTimePartial
      sumTimePartial += bt.sum

      if (actorReplies >= livingActors) {
        logStatistics(false)
        actorReplies = 0

        responseStatisticsPartial = Map()
        enqueuedPartial = 0
        droppedPartial = 0
        failedPartial = 0
        minTimePartial = Long.MaxValue
        maxTimePartial = 0
        sumTimePartial = 0
      }

      if (maxCount > 0 && responseStatisticsTotal.values.sum > maxCount) {
        context.stop(logRouter)
        context.system.terminate()
      }

    case WorkerFailure =>
      livingActors -= 1
      if (livingActors <= 0) {
        context.system.terminate()
      }
  }

  private def logStatistics(isTotal: Boolean): Unit = {
    val responses = if (isTotal) responseStatisticsTotal else responseStatisticsPartial
    val enqueued  = if (isTotal) enqueuedTotal           else enqueuedPartial
    val dropped   = if (isTotal) droppedTotal            else droppedPartial
    val failed    = if (isTotal) failedTotal             else failedPartial
    val minTime   = if (isTotal) minTimeTotal            else minTimePartial
    val maxTime   = if (isTotal) maxTimeTotal            else maxTimePartial
    val sumTime   = if (isTotal) sumTimeTotal            else sumTimePartial

    val statisticsText =
      s"""
         |
         |[STATISTICS] ${if (isTotal) "Global" else "Partial"} (living actors: $livingActors)
         |  Responses by status code:
         |    ${responses.map { case (statusCode, count) => s"Response code $statusCode:  $count" }.mkString("\n  ")}
         |
         |  Enqueued requests: $enqueued
         |  Dropped  requests: $dropped
         |  Failed   requests: $failed
         |  TOTAL    REQUESTS: ${enqueued + dropped + failed}
         |
         |  Min time (in millis): $minTime
         |  Max time (in millis): $maxTime
         |  Avg time (in millis): ${val count = responses.values.sum; if (count > 0) sumTime / responses.values.sum else 0}
      """.stripMargin

    log.info(statisticsText)
  }

  private def sumLongMaps[K](maps: Map[K, Long]*): Map[K, Long] = maps.flatMap(_.toSeq).groupBy(_._1).mapValues(_.map(_._2).sum)

}

object WebClient {

  def props: Props = Props(new WebClient)
  def name: String = "webClient"

  case object GetStatistics
  case object PrintPartialStatistics
  case object PrintTotalStatistics
  case class Statistics(responses: Map[Int, Long], enqueued: Long, dropped: Long, failed: Long, bt: BenchTimes)
  case class BenchTimes(var min: Long = Long.MaxValue, var max: Long = 0, var sum: Long = 0)
  case object WorkerFailure

}
