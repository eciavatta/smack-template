package smack.database.migrations

import smack.database.Migration

object CreatePartialStatsUrlTable extends Migration {

  override def tag: String = "createPartialStatsUrlTable"

  override def up: String =
    s"""
       |CREATE TABLE partial_stats_url (
       |  site_id     TIMEUUID,
       |  year        INT,
       |  stat_id     TIMEUUID,
       |  stat_type   TEXT,
       |  url         TEXT,
       |  requests    BIGINT,
       |  PRIMARY KEY ((site_id, year), stat_id, stat_type, url)
       |)
       |WITH CLUSTERING ORDER BY (stat_id DESC);
     """.stripMargin

  override def down: String = s"DROP TABLE IF EXISTS partial_stats_url"

}
