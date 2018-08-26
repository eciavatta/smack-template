package smack.analysis.jobs

import org.apache.spark.internal.Logging
import smack.commons.traits.SparkJob

import scala.math.random

class SparkPi extends SparkJob with Logging {

  override def run(): Unit = {
    val slices = 10000
    val n = math.min(100000L * 100, Int.MaxValue).toInt // avoid overflow
    val p = sparkContext.parallelize(1 until n, slices)

    val count = p.map { _ =>
      val x = random * 2 - 1
      val y = random * 2 - 1
      if (x*x + y*y <= 1) 1 else 0
    }.reduce(_ + _)

    log.info(s"Pi is roughly ${4.0 * count / (n - 1)}")
  }

}
