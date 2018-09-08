# Installazione Marathon
Marathon è un framework di Mesos mantenuto da Mesosphere.
La documentazione ufficiale è disponibile sul sito [Mesosphere Marathon](https://mesosphere.github.io/marathon/).

## Prerequisiti
Per fare riferimento a questa guida, in ogni nodo master deve installata una distribuzione Ubuntu 16.04 
e deve essere installato il servizio `mesos-master` come indicato nella guida [Installazione Apache Mesos](installazione_mesos.md).
La maggior parte delle operazioni sono da effettuare con utente con privilegi root, si presuppone quindi che la shell sia avviata in modalità sudo.
Per la configurazione del firewall è disponibile la [lista delle porte utilizzate](configurazione_firewall.md).

## Installazione
Ci sono due modi per installare Marathon: scaricandolo dal [sito ufficiale](https://mesosphere.github.io/marathon/) o installandolo da repository Ubuntu.
Se si sceglie la seconda opzione e si è già seguito il procedimento indicato nella guida [Installazione Apache Mesos](installazione_mesos.md) per l'aggiunta
del repository dove è contenuto anche Mesos, saltare questo passaggio.

Nota: è necessario installare Marathon soltanto nei nodi dove è presente il servizio master di Mesos.

Per aggiungere il repository di Ubuntu dove è contenuto Marathon effettuare le seguenti operazioni.
```bash
apt-key adv --keyserver keyserver.ubuntu.com --recv E56151BF
DISTRO=$(lsb_release -is | tr '[:upper:]' '[:lower:]')
CODENAME=$(lsb_release -cs)
echo "deb http://repos.mesosphere.io/${DISTRO} ${CODENAME} main" | tee /etc/apt/sources.list.d/mesosphere.list
apt-get update
```
Per installare Marathon eseguire:
```bash
apt-get install -y marathon
```

La configurazione di Marathon è contenuta nella directory `/etc/marathon/conf`.
Il nome di ogni file creato in questa directory verrà passato come argomento durante l'esecuzione del servizio, mentre il contenuto verrà passato come valore.
Di seguito sono elencati soltanto i parametri obbligatori, tutte le possibili opzioni sono disponibili nella documentazione ufficiale nella sezione
[Command line flags](https://mesosphere.github.io/marathon/docs/command-line-flags.html).
* `hostname`: il nome di dominio comunicato a Mesos e alle altre istanze Marathon. Se non specificato viene utilizzato l'hostname del sistema.
* `master`: l'indirizzo Zookeeper del master di Mesos in una configurazione con singolo master (`zk://master1.example.com:2181/mesos`)
  o gli indirizzi Zookeeper dei master di Mesos in una configurazione con master multipli
  (`zk://master1.example.com:2181,master2.example.com:2181,master3.example.com:2181/mesos`).
* `zk`: l'indirizzo Zookeeper di tutti i nodi dove è eseguito il servizio Marathon. In una configurazione con singolo master indicare
  `zk://master1.example.com:2181/marathon`, mentre in una configurazione con master multipli indicare
  `zk://master1.example.com:2181,master2.example.com:2181,master3.example.com:2181/marathon`.

Per applicare la configurazione occorre riavviare il servizio `marathon`.
```bash
service marathon restart
```

Marathon per eseguire i task non utilizza l'utente root. In ogni agente mesos è necessario aggiungere un nuovo utente di nome `marathon`.
```bash
useradd -m -s /bin/bash marathon
```

## Conclusione
Terminata l'installazione, connettendosi all'indirizzo `http://master1.example.com:8080` è possibile visualizzare l'interfaccia utente di Marathon.

![Interfaccia utente Marathon](https://i.imgur.com/RDcHxl7.png)
