## Configurazione filewall

Nella tabella di seguito sono elencate tutte le porte utilizzate dall'intero sistema.

| Port number  | Protocol | Service       | Source             | Destination  | Description       |
| ------------ | -------- | ------------- | ------------------ | ------------ | ----------------- |
| 22           | tcp      | ssh           | admin              | master,agent | admin port        |
| 80           | tcp      | http          | all                | master       | service port      |
| 443          | tcp      | https         | all                | master       | service port      |
| 2181         | tcp      | zookeeper     | master,agent       | master       | client port       |
| 2888         | tcp      | zookeeper     | master             | master       | leader connection |
| 3888         | tcp      | zookeeper     | master             | master       | leader election   |
| 4040         | tcp      | spark         | admin              | master       | admin port        |
| 5050         | tcp      | mesos-master  | master,agent,admin | master       | master port       |
| 5051         | tcp      | mesos-slave   | master             | agent        | agent port        |
| 7000         | tcp      | cassandra     | agent              | agent        | internal port     |
| 7199         | tcp      | cassandra     | agent,jmx          | agent        | jmx port          |
| 8080         | tcp      | marathon      | master,admin       | master       | admin port        |
| 8082         | tcp      | haproxy       | admin              | master       | stats port        |
| 9042         | tcp      | cassandra     | agent,app          | agent        | client port       |
| 9092         | tcp      | kafka-manager | admin              | master       | admin port        |
| 9092         | tcp      | kafka         | agent,app          | agent        | client port       |
| 9093         | tcp      | kafka         | agent,jmx          | agent        | jmx port          |
| 9160         | tcp      | cassandra     | agent              | agent        | thrift port       |
| 1025-32000   | tcp,udp  | mesos-task    | master,agent       | agent        | mesos task port   |
