package smack.analysis

import org.apache.spark.SparkContext
import org.apache.spark.internal.Logging
import org.quartz._
import org.quartz.impl.StdSchedulerFactory
import smack.analysis.jobs.StatsCollectorJob
import smack.commons.traits.SparkJob

class AnalysisSupervisor(sparkContext: SparkContext, keyspace: String) extends Logging {

  val fiveMinutesInMillis = 300000L

  private val scheduler = new StdSchedulerFactory().getScheduler()
  scheduleSparkJob(classOf[StatsCollectorJob], "0 0/5 * * * ?", jobDataMap => { // every 5 minutes
    jobDataMap.put("StatType", "5_minutes")
    jobDataMap.put("StatRange", fiveMinutesInMillis)
    jobDataMap.put("Keyspace", keyspace)
    jobDataMap
  })

  def startScheduler(): Unit = {
    scheduler.start()
  }

  private def scheduleSparkJob(sparkJobClass: Class[_ <: SparkJob], chroneExpression: String, jobDataMapping: JobDataMap => JobDataMap):
  (JobDetail, CronTrigger, JobDataMap) = {
    val jobName = sparkJobClass.getSimpleName
    val jobDataMap = new JobDataMap()
    jobDataMap.put("SparkContext", sparkContext)
    jobDataMapping(jobDataMap)

    val jobDetail = JobBuilder.newJob(sparkJobClass).withIdentity(jobName, "Spark").usingJobData(jobDataMap).build()

    val trigger = TriggerBuilder.newTrigger().withIdentity(s"${jobName}Trigger", "SparkTrigger")
      .withSchedule(CronScheduleBuilder.cronSchedule(chroneExpression)).build()

    scheduler.scheduleJob(jobDetail, trigger)
    (jobDetail, trigger, jobDataMap)
  }

}
