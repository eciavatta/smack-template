# Cluster deployment
To launch the application it is necessary to have available a set of nodes that must form a Mesos cluster.

## Prerequisites
- Having a set of nodes available that will form the cluster. For creating instances can be used any microservice.
  The paragraph below explains how to create simple droplets with DigitalOcean.
- Divide resources into master nodes and agent nodes, and [install Apache Mesos](mesos_installation.md) on all nodes.
- [Install Marathon](marathon_installation.md) on the nodes where the mesos-master service was installed.
- [Configure the firewall](firewall_configuration.md) to allow the nodes to talk.

### Droplet creation with DigitalOcean
In the attached scripts there is a simple tool for creating droplets on DigitalOcean. It is called `droplets-compose` and has two sub-commands:
- `./droplets-compose up <num>` for creating `num` droplets
- `./droplets-compose down <num>` for the destruction of `num` droplets

Each instance will be assigned the name `mesos-agent$index`, with `$index` ranging from 1 to `num`.
Nodes `mesos-master` must be created previously manually.
Each droplet is associated with a domain record that has the name equals to `mesos-agent$index.do.example.com`. `mesos-agent$index.do.example.com`.
You can change the options of the droplet and the domain record to create on [create-droplet.json.template](/scripts/digitalocean/create-droplet.json.template)
and [create-domain-record.json.template](/scripts/digitalocean/create-domain-record.json.template) respectively.
To run the script you must first edit the following environment variables in [smack-env.sh](/scripts/smack-env.sh).
```bash
# The token provided by DigitalOcean for authentication 
DO_TOKEN=""

# The id of the ssh key uploaded on DigitalOcean
DO_SSH_KEY_ID=0

# The id of the snapshot image of mesos-agent previously created
DO_IMAGE_ID=0

# The maximum number of droplets that can be created
MAX_DROPLETS=10

# The domain registered on DigitalOcean to be used as a reference for the instances
DO_DOMAIN="do.example.com"
```

## Cluster formation
After having configured all the nodes and having Mesos resources available, it is possible to launch tasks on Marathon through JSON objects.
The objects for this application and its dependencies are available in the [marathon](/marathon) directory.
There are also three scripts for automatic cluster formation,
and they are [cluster-init-compose](/scripts/cluster-init-compose), [cluster-smack-compose](/scripts/cluster-smack-compose)
and [cluster-full-compose](/scripts/cluster-full-compose).
Each script contains two sub-commands, `up` can be used for cluster formation and `down` for its destruction.
By default, `cluster-init-compose` it deals with:
- Generate the cluster id, which is used in the Cassandra cluster name (`cassandra_$id`) and as Kafka's Zookeeper path (`kafka_$id`)
- Launch an instance `kafka-seed` that deals with the formation and joining of Kafka brokers
- Launch an instance `cassandra-seed` that deals with the formation and joining of Cassandra nodes
- Launch two instances `kafka-node` that bring the number of Kafka instances to 3
- Launch two instances `cassandra-node` that bring the number of Cassandra instances to 3
- Migrate the Cassandra database schema, using [smack-migrate](smack_migrate.md)
- Creating Kafka topics with their partitions

Always by default, `cluster-smack-compose` it deals with:
- Launch an instance `smack-seed` that deals with joining the new nodes with the already existing nodes that form the cluster
- Launch three instances `smack-frontend` that initiate an instance of a web server to collect client requests and provide them with an answer
- Launch three instances `smack-backend` that collect user requests sent to the frontend and turn them into responses
- Launch three instances `smack-service` that process requests that are not immediately processed by the backend

The script `cluster-full-compose` does nothing but run `cluster-init-compose` before and `cluster-smack-compose` after.

You can modify the parameters of these scripts by modifying the environment variables contained in [smack-env.sh](/scripts/smack-env.sh).
```bash
# The address of the Mesos master where the Zookeeper service is also present
MESOS_HOST=127.0.0.1

# Sentry's private key if you want to send application logs to this service
SENTRY_DNS=""

# The local path where the packages in the jar format of the application are located
APP_PATH=/app

# The path where the Apache Kafka binaries are present
KAFKA_HOME=/opt/kafka

# The current version of the project
VERSION="0.3.0-SNAPSHOT"

# Enable or disable application debugging
DEBUG=false

# Set the production environment
ENVIRONMENT=production

# Set the level of the logs
LOG_LEVEL=warning

# Set the number of Cassandra nodes
CASSANDRA_NODE_INSTANCES=2

# Set the number of Kafka nodes
KAFKA_NODE_INSTANCES=2

# Set the number of smack-frontend instances
SMACK_FRONTEND_INSTANCES=3

# Set the number of smack-backend instances
SMACK_BACKEND_INSTANCES=3

# Set the number of smack-service instances
SMACK_SERVICE_INSTANCES=3

# The name of the log topic
LOG_TOPIC="logs"

# The number of log topic partitions
LOG_PARTITIONS=3

# The replicating factor of the log topic partitions
LOG_REPLICATION_FACTOR=2
```
