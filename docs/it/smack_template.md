# smack-template
smack-template è un caso di studio di un'applicazione costruita con lo stack SMACK.

## Utilizzo
```
smack-template 0.3.0-SNAPSHOT
Usage: smack-template [options] <role>

  -a, --akka-seeds <addr>  The akka seed node/s
  -c, --cassandra-contact-points <addr>
                           The cassandra contact point/s (default: 127.0.0.1:9042)
  --datadog-agent <addr>   The address of the Datadog agent (default: 127.0.0.1:8126)
  --datadog-api <key>      The Datadog api key
  -d, --debug <bool>       True if debug should be enabled
  -e, --environment <env>  The environment to be used (default: development)
  -k, --kafka-bootstraps <addr>
                           The kafka bootstrap server/s (default: 127.0.0.1:9092)
  -l, --loglevel <level>   The log level used by standard output and (optionally) by sentry logger (default: info)
  --sentry-dns <key>       If defined, every logs are sent to sentry servers and can be viewed on Sentry.io. The standard output remain unchanged
  <role>                   The role of the instance to start
  --help                   Display help
Run the instance specified by role and connect to the cluster.
```

## Funzionamento
L'applicazione è composta da quattro parti (o ruoli), *backend*, *frontend*, *service* e *seed*. Ciascun ruolo contribuisce a una parte del sistema e per
funzionare l'applicazione ha bisogno almeno un'instanza di tutti i ruoli. Ogni ruolo è indipendente dagli altri e non è necessaria una relazione 1:1 fra le
istanze. È quindi possibile ad esempio scalare soltanto la parte dell'applicazione che è soggetta a maggior carico. Di seguito è spiegato in dettaglio il
funzionamento di ogni ruolo.

### Backend
È il ruolo che si occupa di raccogliere le richieste degli utenti inviate al Frontend e trasformarle in risposte. Le richieste ricevute vengono inviate dal
frontend sotto forma di messaggi che contengono il tipo di azione da compiere e il contenuto della richiesta. Il backend è dotato di un supervisore che
intercetta il tipo di azione da eseguire riconoscendo la classe dell'oggetto che contiene il messaggio, e lo inoltra al controllore che deve gestire quel tipo
di azione. Il controllore ha il compito di verificare innanzitutto se la richiesta è valida; in caso contrario un messaggio di errore deve inviato al frontend
che si occupa di rispondere al client che ha effettuato la richiesta. Per le richieste valide, il controllore può scegliere fra le seguenti opzioni:
- Gestire localmente la richiesta ed effettuare la computazione necessaria, rispondendo immediatamente al frontend con i risultati
- Inviare un nuovo evento ad un broker Kafka se non è necessario processare la richiesta subito e notificare il frontend della presa in carico
- Salvare in modo permanente i dati ricevuti nel cluster Cassandra e fornire al frontend i riferimenti della transazione

In caso di errore all'interno del controllore, il frontend viene avvisato del problema e il controllore viene riavviato. Le connessioni con i broker Kafka e
con i nodi Cassandra vengono quindi ristabilite.

### Frontend
Avvia un'istanza di un web server che si occupa di raccogliere le richieste dei client e di fornire loro una risposta. Il web server può essere esposto
direttamente all'esterno del cluster o può essere collegato ad un load balancer hardware o software tipo [HAProxy](http://www.haproxy.org/).
Il frontend non gestisce direttamente le richieste ma le inoltra al backend. Se all'interno del cluster sono disponibili più backend, la richiesta viene
inviata al nodo che ha meno carico di lavoro, in modo da ridurre i tempi di processamento. Il web server può gestire più richieste concorrentemente. È stata
utilizzata l'architettura REST per implementare l'interazione dell'applicazione con gli utenti. Ogni risorsa deve essere dichiarare un percorso (route) e una
funzione che costruisce il messaggio con la richiesta da inviare al backend. Alla ricezione di una richiesta, il web server seleziona la route corrispondente
per la risorsa voluta: se esiste la richiesta viene processata, altrimenti viene generata una risposta di errore. Se non ci sono backend attivi, ci sono
ma sono congestionati o il backend si interrompe per un'eccezione non prevista, viene generata una risposta di errore. Se il frontend viene avviato in modalità
debug i dettagli dell'errore sono mostrati all'interno dell'oggetto JSON di risposta. All'interno del frontend è implementata una semplice validazione delle
richieste prima che esse vengano inoltrate al backend, in modo tale da scartare immediatamente le richieste non valide.

### Service
È il servizio che si occupa di processare le richieste che non vengono immediatamente processate dal backend. Serve ad esempio per consumare eventi dai broker
Kafka, processarli e salvarli in modo permanente nel cluster Cassandra. Il ruolo service è dotato di un supervisore che si occupa di monitorare lo stato
dei servizi attivi. Quando in un servizio succede un errore imprevisto, il supervisore ha il compito di riavviare il servizio e di ristabilire
le connessioni con i servizi esterni.

### Seed
È il ruolo che si occupa di congiungere i nuovi nodi con i nodi già esistenti che formano il cluster. Ogni nodo del cluster, all'avvio, deve specificare
l'indirizzo di un seed. È possibile utilizzare il ruolo seed per effettuare questa operazione. Il primo nodo seed avviato all'interno di un nuovo cluster
deve fare congiungimento con se stesso, quindi deve utilizzare come parametro `akka-seeds` il proprio indirizzo o il proprio nome di dominio.

## Estensione

### Backend
Il backend ha il compito di processare i messaggi inviati dal backend. I messaggi vengono scambiati via rete e devono essere serializzati. Per la
serializzazione degli oggetti è usata la libreria [Protocol Buffers](https://developers.google.com/protocol-buffers/) sviluppata da Google. Le definizioni
dei messaggi possoni essere inseriti dentro il file [messages.proto](/commons/src/main/protobuf/messages.proto) all'interno del modulo commons, mentre le
definizioni delle strutture possono essere inseriti dentro il file [structures.proto](/commons/src/main/protobuf/structures.proto). Le classi definite
vengono autogenerate al momento della compilazione. Per serializzare e deserializzare oggetti correttamente è necessario definire un manifesto per ogni
classe, da definire rispettivamente dentro la classe [MessageSerializer](/commons/src/main/scala/smack/commons/serialization/MessageSerializer.scala) e
[StructureSerializer](/commons/src/main/scala/smack/commons/serialization/StructureSerializer.scala), sempre dentro il modulo commons.
Infine occorre dichiarare dentro il file di configurazione [serialization.conf](/commons/src/main/resources/serialization.conf) chi si occupa di serializzare
e deserializzare una determinata classe. Per la documentazione e la sintassi da utilizzare per la definizione di classi serializzabili si rimanda a
[Language Guide](https://developers.google.com/protocol-buffers/docs/proto3).

Definiti i messaggi è possibile creare un nuovo controllore estendendo l'interfaccia base `Controller`. Se il controllore necessita di interagire con i broker
Kafka, è disponibile l'estensione `KafkaController` dove è contenuto il metodo `sendToKafka(message: AnyRef): Future[Done]` utilizzabile per inviare eventi
alle partizioni Kafka di un determinato topic, definito da `kafkaTopic`. Per salvare permanentemente dati o per selezionarli è possibile estendere il trait
`CassandraController` che contiene il metodo `executeQuery(cassandraMessage: CassandraMessage): Future[ResultSet]` per eseguire query e per ottenere
risultati.

Il controllore deve essere un attore che deve essere creato nel [BackendSupervisor](/src/main/scala/smack/backend/BackendSupervisor.scala). Sempre nel
supervisore occorre dichiarare che i messaggi relativi al controllore che si è creato devono essere inoltrati al controllore stesso. Il controllore ha il
compito di trasformare le richieste (inviate originalmente dal frontend sotto forma di messaggi) in risposte. Ogni risposta deve incapsulare l'oggetto
`ResponseStatus` che contiene il codice HTTP della risposta, un messaggio opzionale e lo stack trace in caso di eccezione. All'interno del trait `Controller`
sono già disponibili dei metodi per la creazione di rispose di default, come `success()`, `created()`, `accepted()`, `notFound()`, `badRequest(message)`,
`serviceUnavailable()`, `internalServerError(throwable, message)`. In caso di eccezione è fornito il metodo `responseRecovery(throwable): Option[ResponseStatus]`
per la creazione dell'oggetto `ResponseStatus` dal tipo di eccezione.

### Frontend
Nel frontend sono contenuti un insieme di percorsi (routes) che servono per definire l'interfaccia delle API REST. 


### Service
