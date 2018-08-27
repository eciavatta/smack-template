package smack.database.migrations

import smack.database.Migration

object CreatePartialStatsUrlTable extends Migration {

  override def tag: String = "createPartialStatsUrlTable"

  override def up: String =
    s"""
       |CREATE TABLE partial_stats_url (
       |  site_id     TIMEUUID,
       |  year        INT,
       |  stat_time   TIMESTAMP,
       |  stat_type   TEXT,
       |  url         TEXT,
       |  requests    INT,
       |  PRIMARY KEY ((site_id, year), stat_time, stat_type, url)
       |)
       |WITH CLUSTERING ORDER BY (stat_time DESC);
     """.stripMargin

  override def down: String = s"DROP TABLE IF EXISTS partial_stats_url"

}
