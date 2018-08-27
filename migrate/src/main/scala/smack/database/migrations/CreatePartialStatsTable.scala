package smack.database.migrations

import smack.database.Migration

object CreatePartialStatsTable extends Migration {

  override def tag: String = "createPartialStatsTable"

  override def up: String =
    s"""
       |CREATE TABLE partial_stats (
       |  site_id     TIMEUUID,
       |  year        INT,
       |  stat_time   TIMESTAMP,
       |  stat_type   TEXT,
       |  ip          MAP<TEXT,INT>,
       |  browser     MAP<TEXT,INT>,
       |  requests    BIGINT,
       |  PRIMARY KEY ((site_id, year), stat_time, stat_type)
       |)
       |WITH CLUSTERING ORDER BY (stat_time DESC);
     """.stripMargin

  override def down: String = s"DROP TABLE IF EXISTS partial_stats"

}
