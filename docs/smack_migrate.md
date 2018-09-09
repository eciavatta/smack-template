# smack-migrate
Performs the database schema migration.

## Usage
```
smack-migrate 0.3.0-SNAPSHOT
Usage: smack-migrate [rollback|reset] [options]

  -c, --cassandra-contact-points <addr>
                           The cassandra contact point/s (default: 127.0.0.1:9042)
  --create-keyspace        Create keyspace before start with migration
  -d, --debug <bool>       True if debug should be enabled
  -e, --environment <env>  The environment to be used (default: development)
  --force                  Must be set in production environments
  -l, --loglevel <level>   The log level used by standard output and (optionally) by sentry logger (default: info)
  --sentry-dns <key>       If defined, every logs are sent to sentry servers and can be viewed on Sentry.io. The standard output remain unchanged
  --help                   Display help
Command: rollback [options]
Execute a rollback of the database schema.
  --steps <num>            The number of steps to be rollbacked
Command: reset
Reset the database schema and clean all tables.
Perform the migration of the database schema.
```

An example of a script to launch the command is available here: [run-migrate](/scripts/run-migrate).

## Behavior
The tool allows to migrate the database schema managed by Cassandra.
Migration means the process of evolution of the database schema.
Each migration must be a simple operation, for example creating a table or inserting an index on a table.
Within a migration there must be a unique tag to all migrations, a query to create the elementary data structure and a query to destroy it.
The migrations are performed keeping the established order.
The tool keeps track of all migrations made by saving the status of the tool in the table `migrations` of the database.
You can add new migrations at any time; when new ones are inserted it is necessary to re-run the migration in order to create only the missing structures.
It is not possible to alter old migrations already made or modify tags because they would compromise the results of the tool.  

## Structure
Each migration must be contained in the package [migrations](/migrate/src/main/scala/smack/database/migrations) and must implement trait`Migration`;
the following methods must therefore be defined.
```scala
def up: String
def down: String
def tag: String
```
The method `up` is called when the migration is to be performed and the data structure created.
The method `down`, on the other side, is called in rollback or reset when the data structure must be destroyed.
The method `tag` defines the identification code of the migration that must be unique.
An example of migration can be the following.
```scala
package smack.database.migrations
import smack.database.Migration

object CreateUsersByIdTable extends Migration {
  override def tag: String = "createUsersById"
  override def up: String =
    s"""
       |CREATE TABLE users_by_id (
       |  id        TIMEUUID PRIMARY KEY,
       |  email     TEXT,
       |  full_name TEXT
       |);
     """.stripMargin
  override def down: String = s"DROP TABLE IF EXISTS users_by_id"
}
```

Finally, you must insert the object created in the ordered list of migrations to be executed,
contained in [MigrateSequence](/migrate/src/main/scala/smack/database/MigrateSequence.scala).
