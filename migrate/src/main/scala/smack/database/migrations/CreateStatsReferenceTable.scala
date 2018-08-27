package smack.database.migrations

import smack.database.Migration

object CreateStatsReferenceTable extends Migration {

  override def tag: String = "createStatsReferenceTable"

  override def up: String =
    s"""
       |CREATE TABLE stats_reference (
       |  stat_type        TEXT,
       |  stat_time        TIMESTAMP,
       |  affected_records BIGINT,
       |  PRIMARY KEY (stat_type, stat_time)
       |)
       |WITH CLUSTERING ORDER BY (stat_time DESC);
     """.stripMargin

  override def down: String = s"DROP TABLE IF EXISTS stats_reference"

}
