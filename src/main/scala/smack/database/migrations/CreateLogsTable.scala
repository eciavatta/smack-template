package smack.database.migrations

import smack.database.Migration

object CreateLogsTable extends Migration {

  override def tag: String = "createLogs"

  override def up: String =
    s"""
       |CREATE TABLE logs (
       |  site_id TIMEUUID,
       |  log_id TIMEUUID,
       |  url TEXT,
       |  ip_address TEXT,
       |  user_agent TEXT,
       |  PRIMARY KEY (site_id, log_id)
       |)
       |WITH CLUSTERING ORDER BY (log_id DESC);
     """.stripMargin

  override def down: String = s"DROP TABLE IF EXISTS logs"

}
