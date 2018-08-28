package smack.analysis.jobs

import java.util.{Calendar, Date, UUID}

import com.datastax.spark.connector._
import com.datastax.spark.connector.cql.CassandraConnector
import org.apache.spark.internal.Logging
import org.apache.spark.rdd.RDD
import smack.commons.traits.SparkJob

class StatsCollectorJob extends SparkJob with Logging with Serializable {

  var keyspace: String = _
  var statType: String = _
  var statRange: Long = _

  override def run(): Unit = {
    val statTimestamp = currentContext.getScheduledFireTime.getTime
    val rangeString = s"[${new Date(statTimestamp - statRange)} - ${new Date(statTimestamp)}]"

    if (isStatAlreadyPresent(statTimestamp)) {
      log.info(s"Stats of type [$statType] for range $rangeString are already present")
    } else {
      log.info(s"Start collecting stats of type [$statType] for range $rangeString")
      collectStatistics(statTimestamp)
      log.info(s"Terminate collecting stats of type [$statType] for range $rangeString")
    }
  }

  def setKeyspace(keyspace: String): Unit = this.keyspace = keyspace

  def setStatType(statType: String): Unit = this.statType = statType

  def setStatRange(statRange: Long): Unit = this.statRange = statRange

  private def collectStatistics(statTimestamp: Long): Unit = {
    val year = Calendar.getInstance().get(Calendar.YEAR)

    val rdd = sparkContext.cassandraTable(keyspace, "logs")
      .where(s"log_id > maxTimeuuid(${statTimestamp - statRange}) AND log_id < minTimeuuid($statTimestamp)")
      .keyBy(row => row.getUUID("site_id"))
      .spanByKey
      .persist()

    StatsCollectorJob.partialStatsMapping(rdd, year, statTimestamp, statType).saveToCassandra(keyspace, "partial_stats")
    StatsCollectorJob.partialStatsUrlMapping(rdd, year, statTimestamp, statType).saveToCassandra(keyspace, "partial_stats_url")
    StatsCollectorJob.globalStatsMapping(rdd).saveToCassandra(keyspace, "global_stats")
    StatsCollectorJob.globalStatsUrlMapping(rdd).saveToCassandra(keyspace, "global_stats_url")
    addStatReference(statTimestamp, StatsCollectorJob.affectedRecordsMapping(rdd).fold(0)(_ + _))

    rdd.unpersist()
  }

  private def isStatAlreadyPresent(statTimestamp: Long): Boolean = {
    CassandraConnector(sparkContext).withSessionDo { session =>
      val result = session.execute(s"SELECT COUNT(*) FROM $keyspace.stats_reference WHERE stat_type = ? AND stat_time = $statTimestamp", statType)
      result.one().getLong(0) == 1
    }
  }

  private def addStatReference(statTimestamp: Long, affectedRecords: Long): Unit = {
    CassandraConnector(sparkContext).withSessionDo { session =>
      session.execute(s"INSERT INTO $keyspace.stats_reference(stat_type, stat_time, affected_records) VALUES (?, $statTimestamp, $affectedRecords);", statType)
    }
  }

}

object StatsCollectorJob {

  def partialStatsMapping(rdd: RDD[(UUID, scala.Seq[CassandraRow])], year: Int, statTimestamp: Long, statType: String): RDD[PartialStat] =
    rdd.map { pair => PartialStat(
      pair._1,
      year,
      statTimestamp,
      statType,
      pair._2.groupBy(_.getString("ip_address")).mapValues(_.size),
      pair._2.groupBy(_.getString("user_agent")).mapValues(_.size),
      pair._2.size
    )}

  def partialStatsUrlMapping(rdd: RDD[(UUID, scala.Seq[CassandraRow])], year: Int, statTimestamp: Long, statType: String): RDD[PartialStatUrl] =
    rdd.flatMap { pair =>
      pair._2.groupBy(_.getString("url")).mapValues(_.size).map(pair2 => PartialStatUrl(
        pair._1,
        year,
        statTimestamp,
        statType,
        pair2._1,
        pair2._2
      ))
    }

  def globalStatsMapping(rdd: RDD[(UUID, scala.Seq[CassandraRow])]): RDD[GlobalStat] = rdd.map (pair => GlobalStat(pair._1, pair._2.size))

  def globalStatsUrlMapping(rdd: RDD[(UUID, scala.Seq[CassandraRow])]): RDD[GlobalStatUrl] =
    rdd.flatMap(pair => pair._2.groupBy(_.getString("url")).mapValues(_.size).map(pair2 => GlobalStatUrl(pair._1, pair2._1, pair2._2)))

  def affectedRecordsMapping(rdd: RDD[(UUID, scala.Seq[CassandraRow])]): RDD[Long] = rdd.map(pair => pair._2.size)

  case class PartialStat(siteId: UUID, year: Int, statTime: Long, statType: String, ip: Map[String, Int], browser: Map[String, Int], requests: Int)
  case class PartialStatUrl(siteId: UUID, year: Int, statTime: Long, statType: String, url: String, requests: Int)
  case class GlobalStat(siteId: UUID, requests: Long)
  case class GlobalStatUrl(siteId: UUID, url: String, requests: Long)

}
