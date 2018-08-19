package smack.common.utils

import java.util.UUID

import com.typesafe.config.Config

import scala.concurrent.duration.{Duration, FiniteDuration}

object Converters {

  // This method comes from Hector's TimeUUIDUtils class:
  // https://github.com/rantav/hector/blob/master/core/src/main/java/me/prettyprint/cassandra/utils/TimeUUIDUtils.java
  private val NUM_100NS_INTERVALS_SINCE_UUID_EPOCH = 0x01b21dd213814000L
  def getTimeFromUUID(uuid: UUID): Long = (uuid.timestamp - NUM_100NS_INTERVALS_SINCE_UUID_EPOCH) / 10000

  def toScalaDuration(config: Config, path: String): FiniteDuration = {
    val t = config.getString(path)
    val d = Duration(t)
    Duration(d.length, d.unit)
  }

}
