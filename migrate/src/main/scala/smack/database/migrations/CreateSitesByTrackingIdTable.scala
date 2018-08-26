package smack.database.migrations

import smack.database.Migration

object CreateSitesByTrackingIdTable extends Migration {

  override def tag: String = "createSitesByTrackingId"

  override def up: String =
    s"""
       |CREATE TABLE sites_by_tracking_id (
       |  tracking_id UUID PRIMARY KEY,
       |  site_id     TIMEUUID
       |);
     """.stripMargin

  override def down: String = s"DROP TABLE IF EXISTS sites_by_tracking_id"

}
