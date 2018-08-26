package smack.database

import smack.database.migrations._

object MigrateSequence {

  val seq: Seq[Migration] = Seq(
    CreateUsersByIdTable,
    CreateUsersByCredentialsTable,
    CreateSitesByIdTable,
    CreateSitesByTrackingIdTable,
    CreateSitesByUsersTable,
    CreateLogsTable,
    CreatePartialStatsTable,
    CreatePartialStatsUrlTable,
    CreateGlobalStatsTable,
    CreateGlobalStatsUrlTable,
    CreateStatsReferenceTable
  )

}
