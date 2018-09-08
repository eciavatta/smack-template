# smack-client
Strumento che può essere utilizzato per testare il sistema smack.

## Utilizzo
```
smack-client 0.3.0-SNAPSHOT
Usage: smack-client [options]

  -c, --count <num>        The number of requests to do before stop (0 for infinite)
  -d, --debug <bool>       True if debug should be enabled
  -h, --host <addr>        The host of the service to test (default: localhost)
  -l, --loglevel <level>   The log level used by standard output and (optionally) by sentry logger (default: info)
  -r, --parallelism <num>  The number of actors to start (default: 2)
  -p, --port <port>        The port of the service to test (default: 80)
  -n, --requests-per-second <num>
                           The number of requests per second that each actor should send (default: 20)
  --sentry-dns <key>       If defined, every logs are sent to sentry servers and can be viewed on Sentry.io. The standard output remain unchanged
  --help                   Display help
Tool that can be used to stress test the smack system.
```

Un esempio di script per lanciare il comando è disponibile qui: [run-client](/scripts/run-client).

## Funzionamento
All'avvio del client vengono creati un numero di attori pari al parametro _parallelism_.
Ogni attore contiene una coda nella quale vengono inseriti ogni secondo un valore di _requests-per-second_ richieste.
Ogni richiesta viene effettuata all'indirizzo `http://$host:$port/$uri`. Il client elabora solo le richieste che riesce ad effettuare, le altre le scarta.
Ogni 5 secondi vengono stampate le statistiche parziali che contengono:
* Il numero di risposte ottenute, suddivise per il codice di risposta
* Il numero di richieste inserite nella coda delle richieste
* Il numero di richieste scartate dalla coda delle richieste (queue overflow)
* Il numero di richieste fallite
* Il numero totale di richieste
* Il tempo minimo, in millisecondi, fra richiesta e risposta
* Il tempo massimo, in millisecondi, fra richiesta e risposta
* Il tempo medio, in millisecondi, fra richiesta e risposta.

Al termine dell'esecuzione del client, quando sono state effettuate _count_ richieste o quando il programma è interrotto forzatamente,
vengono stampate le statistiche globali.

Il dettaglio di ogni richiesta è definito all'interno dei `Worker`.

## Struttura
Per creare un nuovo `Worker` utilizzare come esempio il [LogWorker](/client/src/main/scala/smack/client/LogWorker.scala).
Un worker deve essere un attore che viene inizializzato nel [WebClient](/client/src/main/scala/smack/client/WebClient.scala).
Il worker, tramite costruttore, deve ricevere i seguenti parametri: `host: String, port: Int, requestsPerSecond: Int`.
Internamente deve creare una coda di richieste, definire il tipo di richiesta da effettuare
e avviare lo scheduler che invia all'attore stesso un oggetto `ExecuteRequest(request: HttpRequest)` ogni volta che una richiesta deve essere effettuata.
Quando viene ricevuto il messaggio `GetStatistics` dall'attore padre (`WebClient`), il worker deve rispondere con un messaggio
`Statistics(responses: Map[Int, Long], enqueued: Long, dropped: Long, failed: Long, bt: BenchTimes)` che contiene le statistiche parziali locali.

Nota: questo tool non era lo scopo del progetto principale ed è stato realizzato velocemente senza progettare criteri di estendibilità.
Dovrebbe essere quindi migliorato perché ora presenta diversi problemi ed è difficilmente estendibile.
