package smack.database.migrations

import smack.database.Migration

object CreateUsersByCredentialsTable extends Migration {

  override def tag: String = "createUsersByCredentials"

  override def up: String =
    s"""
       |CREATE TABLE users_by_credentials (
       |  email       TEXT,
       |  password    TEXT,
       |  user_id     TIMEUUID,
       |  PRIMARY KEY (email, password)
       |);
     """.stripMargin

  override def down: String = s"DROP TABLE IF EXISTS users_by_credentials"

}
