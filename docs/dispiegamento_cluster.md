# Dispiegamento cluster
Per lanciare l'applicazione è necessario avere a disposizione un insieme di nodi che devono formare un cluster Mesos.

## Prerequisiti
- Avere a disposizione un insieme di nodi che andranno a formare il cluster. È possibile utilizzare qualsiasi microservizio per la creazione di istanze.
  Nel paragrafo sotto è spiegato come creare semplici droplet con DigitalOcean.
- Suddividere le risorse in nodi master e nodi agent, e [installare Apache Mesos](installazione_mesos.md) su tutti i nodi.
- [Installare Marathon](installazione_marathon.md) sui nodi dove è stato installato il servizio mesos-master.
- [Configurare il firewall](configurazione_firewall.md) per permettere ai nodi di dialogare.

### Creazione droplet con DigitalOcean
Negli scripts allegati è presente un semplice tool per la creazione di droplet su DigitalOcean. Si chiama `droplets-compose` e ha due sotto comandi:
- `./droplets-compose up <num>` per la creazione di `num` droplet
- `./droplets-compose down <num>` per la distruzione di `num` droplet

A ciascuna istanza verrà assegnato il nome `mesos-agent$index`, con `$index` che va da 1 a `num`.
I nodi `mesos-master` devono essere creati in precedenza manualmente.
Ad ogni droplet viene associato un record di dominio che ha nome uguale a `mesos-agent$index.do.example.com`.
È possibile modificare le opzioni del droplet e del record di dominio da creare rispettivamente nei file
[create-droplet.json.template](/scripts/digitalocean/create-droplet.json.template)
e [create-domain-record.json.template](/scripts/digitalocean/create-domain-record.json.template).
Per eseguire lo script è necessario prima modificare le seguenti variabili d'ambiente all'interno del file [smack-env.sh](/scripts/smack-env.sh).
```bash
# Il token fornito da DigitalOcean per l'autenticazione
DO_TOKEN=""

# L'id della chiave ssh caricata su DigitalOcean
DO_SSH_KEY_ID=0

# L'id dell'immagine snapshot di mesos-agent creata in precedenza
DO_IMAGE_ID=0

# Il numero massimo di droplets che è possibile creare
MAX_DROPLETS=10

# Il dominio registrato su DigitalOcean da utilizzare come riferimento per le istanze
DO_DOMAIN="do.example.com"
```

## Formazione del cluster
Dopo aver configurato tutti i nodi e avendo a disposizione risorse Mesos è possibile lanciare task su Marathon attraverso degli oggetti JSON.
Gli oggetti per questa applicazione e per le sue dipendenze sono disponibili nella directory [marathon](/marathon).
Sono disponibili anche tre script per la formazione automatica del cluster,
e sono [cluster-init-compose](/scripts/cluster-init-compose), [cluster-smack-compose](/scripts/cluster-smack-compose)
e [cluster-full-compose](/scripts/cluster-full-compose).
Ciascuno script contiene due sotto-comandi, `up` utilizzabile per la formazione del cluster e `down` per la sua distruzione.
Di default, `cluster-init-compose` si occupa di:
- Generare il cluster id, che viene utilizzato nel nome del cluster Cassandra (`cassandra_$id`) e come percorso Zookeeper di Kafka (`kafka_$id`)
- Lanciare una istanza `kafka-seed` che si occupa della formazione e del congiungimento di broker Kafka  
- Lanciare una istanza `cassandra-seed` che si occupa della formazione e del congiungimento di nodi Cassandra
- Lanciare due istanze `kafka-node` che portano il numero di istanze Kafka a 3
- Lanciare due istanze `cassandra-node` che portano il numero di istanze Cassandra a 3
- Eseguire la migrazione dello schema del database Cassandra, utilizzando [smack-migrate](smack_migrate.md)
- Creazione dei topic di Kafka con le relative partizioni

Sempre di default, `cluster-smack-compose` si occupa di:
- Lanciare una istanza `smack-seed` che si occupa di congiungere i nuovi nodi con i nodi già esistenti che formano il cluster
- Lanciare tre istanze `smack-frontend` che si occupano di avviare un'istanza di un web server per raccogliere le richieste dei client e di fornire loro una risposta
- Lanciare tre istanze `smack-backend` che si occupano di raccogliere le richieste degli utenti inviate al frontend e trasformarle in risposte
- Lanciare tre istanze `smack-service` che si occupano di processare le richieste che non vengono immediatamente processate dal backend

Lo script `cluster-full-compose` non fa altro che eseguire `cluster-init-compose` prima e `cluster-smack-compose` dopo.

È possibile modificare i parametri di questi script modificando le variabili d'ambiante contenute all'interno del file [smack-env.sh](/scripts/smack-env.sh).
```bash
# L'indirizzo del master di Mesos dove è presente anche il servizio Zookeeper
MESOS_HOST=127.0.0.1

# La chiave privata di Sentry se si vogliono inviare i log dell'applicazione a questo servizio
SENTRY_DNS=""

# Il percorso locale dove sono contenuti i pacchetti in formato jar dell'applicazione
APP_PATH=/app

# Il percorso dove sono presenti i binari di Apache Kafka
KAFKA_HOME=/opt/kafka

# La versione corrente del progetto
VERSION="0.3.0-SNAPSHOT"

# Attiva o disattiva il debug dell'applicazione
DEBUG=false

# Imposta l'ambiente di produzione
ENVIRONMENT=production

# Imposta il livello dei log
LOG_LEVEL=warning

# Imposta il numero dei nodi Cassandra
CASSANDRA_NODE_INSTANCES=2

#Imposta il numero dei nodi Kafka
KAFKA_NODE_INSTANCES=2

# Imposta il numero delle istanze smack-frontend
SMACK_FRONTEND_INSTANCES=3

# Imposta il numero delle istanze smack-backend
SMACK_BACKEND_INSTANCES=3

# Imposta il numero delle istanze smack-service
SMACK_SERVICE_INSTANCES=3

# Il nome del topic relativo ai log
LOG_TOPIC="logs"

# Il numero delle partizioni del topic relativo ai log
LOG_PARTITIONS=3

# Il fattore replicante delle partizioni del topic relativo ai log
LOG_REPLICATION_FACTOR=2
```
