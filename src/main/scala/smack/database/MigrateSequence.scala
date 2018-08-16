package smack.database

import smack.database.migrations._

object MigrateSequence {

  val list: Seq[Migration] = Seq(
    CreateUsersByIdTable,
    CreateUsersByCredentialsTable,
    CreateSitesByIdTable,
    CreateSitesByTrackingIdTable,
    CreateSitesByUsersTable,
    CreateLogsTable
  )

}
