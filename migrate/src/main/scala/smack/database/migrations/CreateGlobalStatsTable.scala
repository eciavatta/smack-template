package smack.database.migrations

import smack.database.Migration

object CreateGlobalStatsTable extends Migration {

  override def tag: String = "createGlobalStatsTable"

  override def up: String =
    s"""
       |CREATE TABLE global_stats (
       |  site_id     TIMEUUID,
       |  requests    COUNTER,
       |  PRIMARY KEY (site_id)
       |);
     """.stripMargin

  override def down: String = s"DROP TABLE IF EXISTS global_stats"

}
