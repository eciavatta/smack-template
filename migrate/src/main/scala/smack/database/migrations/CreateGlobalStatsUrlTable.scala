package smack.database.migrations

import smack.database.Migration

object CreateGlobalStatsUrlTable extends Migration {

  override def tag: String = "createGlobalStatsUrlTable"

  override def up: String =
    s"""
       |CREATE TABLE global_stats_url (
       |  site_id     TIMEUUID,
       |  url         TEXT,
       |  requests    COUNTER,
       |  PRIMARY KEY (site_id, url)
       |);
     """.stripMargin

  override def down: String = s"DROP TABLE IF EXISTS global_stats_url"

}
