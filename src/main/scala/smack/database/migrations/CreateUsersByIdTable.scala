package smack.database.migrations

import smack.database.Migration

object CreateUsersByIdTable extends Migration {

  override def tag: String = "createUsersById"

  override def up: String =
    s"""
       |CREATE TABLE users_by_id (
       |  id TIMEUUID PRIMARY KEY,
       |  email TEXT,
       |  full_name TEXT);
     """.stripMargin

  override def down: String = s"DROP TABLE IF EXISTS users_by_id"

}
