package smack.commons.traits

import org.apache.spark.SparkContext
import org.apache.spark.internal.Logging
import org.quartz.{DisallowConcurrentExecution, Job, JobExecutionContext, PersistJobDataAfterExecution}

@DisallowConcurrentExecution
@PersistJobDataAfterExecution
trait SparkJob extends Job with Logging {

  var sparkContext: SparkContext = _

  var currentContext: JobExecutionContext = _

  override def execute(context: JobExecutionContext): Unit = {
    currentContext = context

    log.info(s"Start job with name: ${context.getJobDetail.getKey.getName}")
    run()
    log.info(s"Terminate job with name: ${context.getJobDetail.getKey.getName}")
  }

  def run(): Unit

  def setSparkContext(sparkContext: SparkContext): Unit = this.sparkContext = sparkContext

}
