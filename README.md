# smack-template
[![Build Status](https://travis-ci.com/eciavatta/smack-template.svg?token=nfQhLp6KpfsSDXWSqbxN&branch=develop)](https://travis-ci.com/eciavatta/smack-template)

smack-template è un caso di studio di un'applicazione costruita con lo stack SMACK.

## Contenuto
All'interno del progetto sono presenti diversi moduli:
- [smack-template](/docs/smack_template.md), il modulo principale che contiene i sorgenti dell'applicazione
- [smack-analysis](/docs/smack_analysis.md), l'applicazione Spark per effettuare analisi sul modello
- [smack-client](/docs/smack_client.md), uno strumento che può essere utilizzato per testare il sistema smack
- [smack-migrate](/docs/smack_migrate.md), per eseguire la migrazione dello schema del database

## Comandi
Il progetto è stato sviluppato utilizzando sbt. Sono disponibili diversi comandi, o task, di seguito sono elencati i principali.
- `sbt clean`, pulisce i file compilati del progetto
- `sbt compile`, compila tutti i moduli del progetto
- `sbt dependencyList`, mostra le dipendenze del progetto
- `sbt test`, esegue i test del progetto
- `sbt multi-jvm:test`, esegue i test di integrazione
- `sbt clean coverage test multi-jvm:test coverageReport`, esegue tutti i test e genera il rapporto di copertura
- `sbt assembly`, genera il fat jar per ogni modulo
- `sbt-docker`, crea un'immagine Docker dell'applicazione

## Informazioni
L'elenco delle dipendenze del progetto, suddiviso per moduli, è contenuto nel file [Dependencies](/project/Dependencies.scala),
mentre le relative versioni sono specificate nel file [Versions](/project/Versions.scala). Per cambiare il nome del progetto, la versione del progetto o
il nome dell'utente Docker per la creazione dell'immagine dell'applicazione è possibile modificare il contenuto delle variabili presenti nel file
[build.sbt](build.sbt). Per verificare lo stile del codice è utilizzato ScalaStyle; l'elenco delle regole utilizzate è disponibile nel file
[scalastyle-config.xml](scalastyle-config.xml).

## Test
Per eseguire i test è necessario avere attivi nella macchina locale almeno un'istanza di Apache Kafka e un'instanza di Apache Cassandra. È possibile lanciare i
componenti necessari attraverso il comando `docker-compose up -d`. Nel file [docker-compose.testing.yml](docker-compose.testing.yml) è contenuta la configurazione
di docker-compose per creare un ambiente di test dell'applicazione, attivabile con il comando
`docker-compose -f docker-compose.yml -f docker-compose.testing.yml up -d`.

## Altra documentazione
- [Installazione Apache Mesos](/docs/installazione_mesos.md)
- [Installazione Marathon](/docs/installazione_marathon.md)
- [Configurazione del firewall](/docs/configurazione_firewall.md)
- [Dispiegamento del cluster](/docs/dispiegamento_cluster.md)
