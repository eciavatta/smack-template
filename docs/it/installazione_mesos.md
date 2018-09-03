# Installazione Apache Mesos
La configurazione riguarda due tipi di nodi: mesos-master e mesos-slave (o agent). La documentazione ufficiale è disponibile sul sito
[Apache Mesos](http://mesos.apache.org/).

## Prerequisiti
Per fare riferimento a questa guida, in ogni nodo deve installata una distribuzione Ubuntu 16.04.
La maggior parte delle operazioni sono da effettuare con utente con privilegi root, si presuppone quindi che la shell sia avviata in modalità sudo.
È consigliabile impostare l'hostname della macchina in modo che possa essere risolto da un server DNS esterno o utilizzando BIND in un nome di dominio valido
e che l'indirizzo risolto possa essere raggiungibile dagli altri nodi del cluster.
Per la configurazione del firewall è possibile vedere sotto quali porte è necessario lasciare aperte.

## Installazione

### Installazione di Java8
È innanzitutto necessario installare una versione di Java8. Il modo più facile per installare Java8 è utilizzare la versione disponibile nei repository di
default di Ubuntu. In particolare, per ottenere l'ultima versione di OpenJDK 8 è necessario aggiornare l'indice dei pacchetti e installare il JDK.
```bash
apt-get update
apt-get install -y default-jdk
```
È consigliato invece installare la vesione Oracle di Java 8, ma è necessario aggiungere prima l'archivio privato Oracle e accettare la licenza.
```bash
add-apt-repository -y ppa:webupd8team/java
apt-get update
echo "oracle-java8-installer shared/accepted-oracle-license-v1-1 select true" | debconf-set-selections
apt-get install -y oracle-java8-installer
```
Aggiungere anche la variabile d'ambiente che indica il percorso della directory di Java.
```bash
echo -e "JAVA_HOME=""/usr/lib/jvm/java-8-oracle""" >> /etc/environment
source /etc/environment
```
È possibile modificare quale versione di Java utilizzare nel seguente modo.
```bash
update-alternatives --config java
```

### Installazione di Apache Mesos

Sia nei nodi master sia nei nodi agent occorre installare l'ultima versione disponibile di Apache Mesos.
È possibile scaricare i sorgenti di Mesos dal sito ufficiale e compilarli manualmente. Le istruzioni per compilare i sorgenti e installare Mesos in questo modo
sono disponibili qui: [Building Apache Mesos](http://mesos.apache.org/documentation/latest/building/).

Per installare Mesos attraverso il repository di Ubuntu è necessario effettuare le seguenti operazioni.
```bash
apt-key adv --keyserver keyserver.ubuntu.com --recv E56151BF
DISTRO=$(lsb_release -is | tr '[:upper:]' '[:lower:]')
CODENAME=$(lsb_release -cs)
echo "deb http://repos.mesosphere.io/${DISTRO} ${CODENAME} main" | tee /etc/apt/sources.list.d/mesosphere.list
apt-get update
apt-get install -y mesos
```

La procedura per configurare i nodi master e i nodi agent è diversa. Sono forniti gli esempi per due tipi di configurazioni:
* Un sono nodo master. Nella seguente configurazione si assume che il master sia raggiungibile dagli altri nodi dall'indirizzo `master1.example.com`
* Più nodi master. È consigliabile utilizzare questo tipo di configurazione in ambienti di produzione e avere un numero dispari di nodi master (es. 3, 5, 7).
  Nella configurazione di esempio si assume che ci siano 3 nodi master raggiungibili dagli altri nodi dagli indirizzi `master[1-3].example.com`

#### Mesos Master
Per il coordinamento e il salvataggio di dati in modo permanente Mesos utilizza Apache Zookeeper, che viene installato di default quando si installa Apache
Mesos attraverso il metodo indicato sopra. I file di configurazione di Zookeeper sono presenti nella directory `/etc/zookeeper/conf`.
Occorre modificare:
* L'id del servizio Zookeeper. Deve essere un intero e deve essere unico all'interno del cluster. Aprire con un editor il file `myid` e inserire l'id scelto.
* Le impostazioni di Zookeeper contenute all'interno del file di configurazione `zoo.cfg`. È possibile vedere l'elenco di tutte le possibili opzioni nella
  [documentazione Zookeeper](http://hadoop.apache.org/zookeeper/docs/current/zookeeperAdmin.html). Di seguito è presente la configurazione minima.
```
# la directory dove sono salvati gli snapshot
dataDir=/var/lib/zookeeper

# la porta alla quale i client si connettono
clientPort=2181

# gli altri server zookeeper, da impostare soltanto se si utilizzano più nodi master
# la prima porta è usata per connettersi al server leader
# la seconda porta è usata per l'elezione del leader
server.1=master1.example.com:2888:3888
server.2=master2.example.com:2888:3888
server.3=master3.example.com:2888:3888
```

Mesos per connettersi a zookeeper deve conoscere tutti gli indirizzi dei servizi zookeeper presenti. Aprire con un editor il file `/etc/mesos/zk` e inserire
la stringa `zk://master1.example.com:2181/mesos` se si utilizza una configurazione con un solo nodo master, oppure
`zk://master1.example.com:2181,master2.example.com:2181,master3.example.com:2181/mesos` se si utilizzano ad esempio 3 nodi master.

La configurazione del servizio master di mesos è presente nella directory `/etc/mesos-master`. Il nome di ogni file creato in questa directory verrà passato
come argomento durante l'esecuzione del servizio, mentre il contenuto viene passato come valore. È possibile vedere la lista degli argomenti disponibili sulla
[documentazione](http://mesos.apache.org/documentation/latest/configuration/master/) sul sito di Apache Mesos. Di seguito sono elencati i parametri principali.
* `quorum`: la dimensione del quorum delle repliche. È consigliabile rispettare la seguente regola: `quorum > (number of masters)/2 `. Parametro **obbligatorio**
  solo nel caso in cui si scelga una configurazione con più nodi master.
* `work_dir`: il percorso della directory del master. Impostare `/var/lib/mesos`. Parametro  **obbligatorio**.
* `cluster`: il nome da utilizzare per il cluster.
* `hostname`: il nome di dominio comunicato agli altri master. Se non viene specificato parametro `ip` o `advertise_ip`, il master deve essere raggiungibile
  dagli altri master attraverso questo valore. Di default viene utilizzato l'hostname del sistema.
* `ip`: l'indirizzo ip che gli altri master devono utilizzare per comunicare e che viene utilizzato al posto dell'`hostname`. Per ottenere l'indirizzo ip del
  sistema sull'interfaccia `eth0` utilizzare il comando `ifconfig eth0 | awk '/inet addr/{split($2,a,":"); print a[2]}'`.
* `advertise_ip`: l'indirizzo ip che gli altri master devono utilizzare per comunicare se risolvendo l'`hostname` non è possibile raggiungere il master.

Quando si installa mesos tramite repository Ubuntu come nel procedimento indicato sopra viene impostato l'avvio in automatico del servizio `mesos-slave`.
Disabilitare questo servizio nei nodi master utilizzando i seguenti comandi.
```bash
service mesos-slave stop
echo "manual" > /etc/init/mesos-slave.override
```

Occorre infine riavviare i servizi `zookeeper` e `mesos-master` dopo aver modificato la configurazione.
```bash
service zookeeper restart
service mesos-master restart
```

Collegandosi all'indirizzo `http://master1.example.com:5050` è dispobile l'interfaccia utente di mesos master, come mostrata di seguito.

![Interfaccia utente mesos master](https://i.imgur.com/O69r0S4.png)

#### Mesos Agent
Per connettersi ai nodi master gli agenti mesos devono conoscere gli indirizzi zookeeper dei master. È necessario modificare il file `/etc/mesos/zk` e inserire
la stringa `zk://master1.example.com:2181/mesos` se si utilizza una configurazione con un solo nodo master, oppure
`zk://master1.example.com:2181,master2.example.com:2181,master3.example.com:2181/mesos` se si utilizzano ad esempio 3 nodi master.

La configurazione del servizio slave di mesos è presente nella directory `/etc/mesos-slave`. Il nome di ogni file creato in questa directory verrà passato
come argomento durante l'esecuzione del servizio, mentre il contenuto viene passato come valore. È possibile vedere la lista degli argomenti disponibili sulla
[documentazione](http://mesos.apache.org/documentation/latest/configuration/master/) sul sito di Apache Mesos. Di seguito sono elencati i parametri principali.
* `work_dir`: il percorso della directory del master. Impostare `/var/lib/mesos`. Parametro  **obbligatorio**.
* `hostname`: il nome di dominio comunicato al master e utilizzato dai task. Se non viene specificato parametro `ip` o `advertise_ip`, l'agente deve essere
  raggiungibile dal master attraverso questo valore. Di default viene utilizzato l'hostname del sistema.
* `ip`: l'indirizzo ip che il master deve utilizzare per comunicare e che viene utilizzato al posto dell'`hostname`. Per ottenere l'indirizzo ip del sistema
  sull'interfaccia `eth0` utilizzare il comando `ifconfig eth0 | awk '/inet addr/{split($2,a,":"); print a[2]}'`.
* `advertise_ip`: l'indirizzo ip che il master deve utilizzare per comunicare se risolvendo l'`hostname` non è possibile raggiungere l'agente.

Quando si installa mesos tramite repository Ubuntu come nel procedimento indicato sopra viene impostato l'avvio in automatico dei servizi `zookeeper` e
`mesos-master`. Disabilitare questi servizi nei nodi agent utilizzando i seguenti comandi.
```bash
service zookeeper stop
echo "manual" > /etc/init/zookeeper.override
service mesos-master stop
echo "manual" > /etc/init/mesos-master.override
```

Occorre infine riavviare il servizio `mesos-slave` dopo aver modificato la configurazione.
```bash
service mesos-slave restart
```

## Conclusione
Il cluster mesos è pronto per essere utilizzato. È ora possibile [installare il framework Marathon](docs/it/installazione_marathon.md) per la gestione dei task.
