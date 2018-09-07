# smack-migrate
Esegue la migrazione dello schema del database.

## Utilizzo
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

Un esempio di script per lanciare il comando è disponibile qui: [run-migrate](/scripts/run-migrate).

## Funzionamento
Lo strumento permette di effettuare migrazioni dello schema del database gestito da Cassandra. Per migrazione si intende il processo di evoluzione dello schema
del database. Ogni migrazione deve essere una semplice operazione, ad esempio la creazione di una tabella o l'inserimento di un indice. All'interno di una
migrazione devono essere presenti un tag univoco a tutte le migrazioni, una query per creare la struttura dati elementare e una query per distruggerla. Le
migrazioni vengono eseguite mantenendo l'ordine stabilito. Lo strumento tiene traccia di tutte le migrazioni effettuate salvando lo stato dello strumento nella
tabella `migrations` del database. È possibile inserire nuove migrazioni in qualsiasi momento; quando ne si inseriscono di nuove occorre rieseguire la migrazione
in modo da creare soltanto le strutture mancanti. Non è possibile alterare vecchie migrazioni già effettuate o modificare tag perché si comprometterebbero i
risultati dello strumento.  

## Struttura
Ogni migrazione deve essere contenuta dentro il package [migrations](/migrate/src/main/scala/smack/database/migrations) e deve implementare il trait
`Migration`; devono quindi essere definiti i seguenti metodi:
```scala
def up: String
def down: String
def tag: String
```
Il metodo `up` viene chiamato quando la migrazione deve essere effettuata e la struttura dati creata. Il metodo `down`, all'opposto, viene chiamato in fase
di rollback o reset quando la struttura dati deve essere distrutta. Il metodo `tag` definisce il codice identificativo della migrazione che deve essere univoco.
Un esempio di migrazione può essere il seguente:
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

Occorre infine inserire l'oggetto creato nella lista ordinata delle migrazioni da effettuare, contenuta nel file
[MigrateSequence](/migrate/src/main/scala/smack/database/MigrateSequence.scala).
