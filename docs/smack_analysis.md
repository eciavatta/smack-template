# smack-analysis
Applicazione Spark per effettuare analisi sul modello.

## Prerequisiti
Il jar finale di questa applicazione, una volta creato con il comando `sbt assembly`, non contiene al suo interno i file binari di
[Apache Spark](http://spark.apache.org/).
È necessario quindi, per eseguire l'applicazione nel cluster, installare Spark nel nodo utilizzato da driver (dove l'applicazione viene lanciata)
e in tutti i possibili nodi utilizzati come esecutori (dove l'applicazione viene eseguita).
Per installare Spark su tutti i nodi è possibile scaricare la versione già compilata dal sito ufficiale Apache,
nella sezione [Downloads](http://spark.apache.org/downloads.html).
Estrarre quindi l'archivio ottenuto in un percorso comune a tutti i nodi, ad esempio in `/opt/spark`.
È necessario avere Java 8 installato e Apache Mesos configurato in tutti i nodi per avviare Spark in modalità cluster.
La guida di riferimento per poter configurare i nodi si trova qui: [Installazione Mesos](installazione_mesos.md).

## Utilizzo
```
smack-analysis 0.3.0-SNAPSHOT
Usage: smack-analysis [options]

  -c, --cassandra-bootstrap <addr>
                           The cassandra contact point/s (default: 127.0.0.1:9042)
  -k, --keyspace <name>    The keyspace name of the Cassandra database (default: smackdev)
  -l, --loglevel <level>   The log level used by standard output and (optionally) by sentry logger (default: info)
  --sentry-dns <key>       If defined, every logs are sent to sentry servers and can be viewed on Sentry.io. The standard output remain unchanged
  --help                   Display help
Spark application to perform analysis on the model.
```
Per avviare l'applicazione, sia in modalità singola che in modalità cluster, è necessario utilizzare `spark-submit` presente nei binari di Spark.
Un esempio di script per lanciare l'applicazione in modalità cluster attraverso Apache Mesos è disponibile qui: [run-analysis](/scripts/run-analysis).
Al momento dell'avvio devono essere presenti le seguenti variabili d'ambiente:
```bash
# Il percorso dove sono contentuti i binari di Spark, che devono essere presenti in tutti i nodi, sia driver sia esecutori
SPARK_HOME=/opt/spark

# Il percorso dove devono essere presenti le librerie di Mesos
MESOS_NATIVE_JAVA_LIBRARY=/usr/local/lib/libmesos.so

# L'indirizzo ip del nodo, da definire quando l'hostname del nodo è risolto con un indirizzo privato
SPARK_LOCAL_IP=x.x.x.x

# Il nome di dominio pubblico da comunicare agli esecutori
SPARK_PUBLIC_DNS=master1.example.com
```

## Funzionamento
Quando l'applicazione parte si avvia uno scheduler che è utilizzato per avviare job in intervalli di tempo regolari o in orari precisi.
Un job è la rappresentazione di una quantità di lavoro da eseguire in maniera distribuita su più esecutori Spark.
Un esempio di job può essere il raggruppamento di dati in una tabella del database Cassandra per una colonna che non è la chiave di partizione.
Ad ogni job è associato un trigger, che scandisce quando un job deve essere eseguito.
Quando il trigger viene scatenato, il job associato viene inserito nella coda dei job di Spark.
Il driver di Spark si occupa di suddividere il job in steps, ovvero unità di lavoro elementari, che invierà agli esecutori.
All'avvio dell'applicazione all'indirizzo `http://master1.example.com:4040` è disponibile l'interfaccia utente di Spark, mostrata in figura.

![Interfaccia utente Spark](https://i.imgur.com/iuGwUlG.png)

## Struttura
Ogni job deve implementare il trait `SparkJob` e deve essere collocato nel package [jobs](/analysis/src/main/scala/smack/analysis/jobs).
Il job deve implementare il metodo `run(): Unit` che viene chiamato ogni volta che il trigger associato a questo job viene scatenato.
All'interno del job sono disponibili due variabili, `sparkContext: SparkContext` che deve essere utilizzata per eseguire operazioni distribuite nel cluster
e `currentContext: JobExecutionContext` che contiene le informazioni relative all'esecuzione corrente del job.
Tutti i job devono essere inizializzati nella classe [AnalysisSupervisor](/analysis/src/main/scala/smack/analysis/AnalysisSupervisor.scala).
Ad ogni job è possibile iniettare ulteriori informazioni attraverso la `JobDataMap`.
È possibile trovare la documentazione su come effettuare questa operazione sulla
[documentazione di Quartz](http://www.quartz-scheduler.org/documentation/quartz-2.x/tutorials/tutorial-lesson-03.html).
Ogni job per essere eseguito deve avere associato un trigger. Per eseguire un job ogni 5 minuti, l'espressione da usare è `0 0/5 * * * ?`.
Per la documentazione completa si rimanda a [CronTrigger](http://www.quartz-scheduler.org/documentation/quartz-2.x/tutorials/tutorial-lesson-06.html).

La documentazione di Spark su come scrivere correttamente un job è disponibile sul sito ufficiale di [Apache Spark](http://spark.apache.org/docs/latest/).
Per connettersi al database Cassandra è stato utilizzato il driver `spark-cassandra-connector`. La documentazione di questa libreria, mantenuta da Datastax,
è disponile su Github in questo indirizzo [spark-cassandra-connector](https://github.com/datastax/spark-cassandra-connector/blob/master/doc/0_quick_start.md).
È possibile infine trovare un esempio di job nella classe [StatsCollectorJob](/analysis/src/main/scala/smack/analysis/jobs/StatsCollectorJob.scala).
