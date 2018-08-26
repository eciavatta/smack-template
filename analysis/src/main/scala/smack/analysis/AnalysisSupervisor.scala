package smack.analysis

import org.apache.spark.SparkContext
import org.apache.spark.internal.Logging
import org.quartz._
import org.quartz.impl.StdSchedulerFactory
import smack.analysis.jobs.SparkPi
import smack.commons.traits.SparkJob

class AnalysisSupervisor(sparkContext: SparkContext, startFrom: Long) extends Logging {

  private val scheduler = new StdSchedulerFactory().getScheduler()
  scheduleSparkJob(classOf[SparkPi], "0/10 * * * * ?")

  def startScheduler(): Unit = {
    scheduler.start()
  }

  private def scheduleSparkJob(sparkJobClass: Class[_ <: SparkJob], chroneExpression: String): (JobDetail, CronTrigger) = {
    val jobName = sparkJobClass.getSimpleName
    val jobDataMap = new JobDataMap()
    jobDataMap.put("SparkContext", sparkContext)

    val jobDetail = JobBuilder.newJob(sparkJobClass).withIdentity(jobName, "Spark").usingJobData(jobDataMap).build()

    val trigger = TriggerBuilder.newTrigger().withIdentity(s"${jobName}Trigger", "SparkTrigger")
      .withSchedule(CronScheduleBuilder.cronSchedule(chroneExpression)).build()

    scheduler.scheduleJob(jobDetail, trigger)
    (jobDetail, trigger)
  }

}
