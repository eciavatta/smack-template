package smack.database.migrations

import smack.database.Migration

object CreateSitesByUsersTable extends Migration {

  override def tag: String = "createSitesByUsersId"

  override def up: String =
    s"""
       |CREATE TABLE sites_by_users (
       |  user_id TIMEUUID,
       |  site_id TIMEUUID,
       |  PRIMARY KEY (user_id, site_id);
     """.stripMargin

  override def down: String = s"DROP TABLE IF EXISTS sites_by_users"

}
