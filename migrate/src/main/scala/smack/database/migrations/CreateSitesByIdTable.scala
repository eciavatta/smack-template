package smack.database.migrations

import smack.database.Migration

object CreateSitesByIdTable extends Migration {

  override def tag: String = "createSitesById"

  override def up: String =
    s"""
       |CREATE TABLE sites_by_id (
       |  id          TIMEUUID PRIMARY KEY,
       |  domain      TEXT,
       |  user_id     TIMEUUID,
       |  tracking_id UUID
       |);
     """.stripMargin

  override def down: String = s"DROP TABLE IF EXISTS sites_by_id"

}
