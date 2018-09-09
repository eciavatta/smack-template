# Apache Mesos installation
The configuration concerns two types of nodes: mesos-master and mesos-slave (or agent).
The official documentation for the installation and configuration of Mesos is available on the [Apache Mesos](http://mesos.apache.org/) website.

## Prerequisites
To refer to this guide, each node must have installed the Ubuntu 16.04 distribution.
Most operations are performed with a user with root privileges, so it is assumed that the shell is started in sudo mode.
It is advisable to set the hostname of the machine so that it can be resolved by an external DNS server or by using BIND in a valid domain name
and that the resolved address can be reachable from the other nodes of the cluster.
The [list of port used](firewall_configuration.md) is available for configuring the firewall.

## Installation

### Installation of Java 8
First you need to install a version of Java 8. The easiest way to install it is to download it directly from the default Ubuntu repository,
which contains the latest version of OpenJDK 8. To do this you need to update the package index and install the JDK.
```bash
apt-get update
apt-get install -y default-jdk
```
Instead, it is recommended that you install the Oracle version of Java 8,
but you must first add the Oracle private repository to the local repositories and accept the license.
```bash
add-apt-repository -y ppa:webupd8team/java
apt-get update
echo "oracle-java8-installer shared/accepted-oracle-license-v1-1 select true" | debconf-set-selections
apt-get install -y oracle-java8-installer
```
You must also add the environment variable that indicates the path to the Java directory.
```bash
echo -e "JAVA_HOME=""/usr/lib/jvm/java-8-oracle""" >> /etc/environment
source /etc/environment
```
You can change which version of Java to use in the following way.
```bash
update-alternatives --config java
```

### Installation of Apache Mesos

Both the master nodes and the agent nodes need to install the latest available version of Apache Mesos.
You can download the Mesos sources from the official website and compile them in manually.
Instructions for compiling sources and installing Mesos in this way are available here:
[Building Apache Mesos](http://mesos.apache.org/documentation/latest/building/).

To install Mesos through the Ubuntu repository it is necessary to perform the following operations.
```bash
apt-key adv --keyserver keyserver.ubuntu.com --recv E56151BF
DISTRO=$(lsb_release -is | tr '[:upper:]' '[:lower:]')
CODENAME=$(lsb_release -cs)
echo "deb http://repos.mesosphere.io/${DISTRO} ${CODENAME} main" | tee /etc/apt/sources.list.d/mesosphere.list
apt-get update
apt-get install -y mesos
```

The procedure for configuring master nodes and agent nodes is different. Examples are provided for two types of configurations:
* Only one master node. In the following configuration it is assumed that the master is reachable from the other nodes from the address `master1.example.com`
* Multiple master nodes. It is advisable to use this type of configuration in production environments and have an odd number of master nodes (eg 3, 5, 7).
  In the example configuration it is assumed that there are 3 master nodes reachable from the other nodes from the addresses `master[1-3].example.com`

#### Mesos Master
For the coordination and the permanently saving of data Mesos uses Apache Zookeeper,
which is installed automatically when you install Apache Mesos through the method indicated above.
The Zookeeper configuration files are located in the directory `/etc/zookeeper/conf`.
It is necessary to modify:
* The id of the Zookeeper service. It must be an integer and must be unique within the cluster. Open the file `myid` with an editor and enter the selected id.
* The Zookeeper settings contained within the configuration file `zoo.cfg`. You can view a list of all the possible options in the
  [Zookeeper documentation](http://hadoop.apache.org/zookeeper/docs/current/zookeeperAdmin.html). The minimum configuration is shown below.
```bash
# the directory where the snapshots are saved
dataDir=/var/lib/zookeeper

# the port to which clients connect
clientPort=2181

# other zookeeper servers, to be set only if multiple master nodes are used
# the first port is used to connect to the server leader
# the second port is used for the election of the leader
server.1=master1.example.com:2888:3888
server.2=master2.example.com:2888:3888
server.3=master3.example.com:2888:3888
```

Mesos to connect to Zookeeper must know all the addresses of the Zookeeper services.
Open the file `/etc/mesos/zk` with an editor and insert the string `zk://master1.example.com:2181/mesos`if you are using a configuration with only one master
node, or `zk://master1.example.com:2181,master2.example.com:2181,master3.example.com:2181/mesos` if you are using, for example 3 master nodes.

The configuration of the Mesos master service is located in the directory `/etc/mesos-master`.
The name of each file created in this directory will be passed as argument on service startup, while the content will be passed as value.
You can view the list of arguments available on the [documentation](http://mesos.apache.org/documentation/latest/configuration/master/)
on the Apache Mesos website. The main parameters are listed below.
* `quorum`: the minimum number of active master nodes. You should observe the following rule: `quorum > (number of masters)/2`.
  **Required** parameter only if a configuration with multiple master nodes is selected.
* `work_dir`: the path of the master directory. Set up `/var/lib/mesos`. **Required** parameter.
* `cluster`: the name to be used for the cluster.
* `hostname`: the domain name communicated to the other masters. If not specified parameter `ip` or `advertise_ip`,
  the master must be reachable by other master through this value. The system hostname is used by default.
* `ip`: the IP address that the other masters must use to communicate and which is used instead of `hostname`.
  To obtain the IP address of the system on the interface `eth0` use the command `ifconfig eth0 | awk '/inet addr/{split($2,a,":"); print a[2]}'`.
* `advertise_ip`: the IP address that the other masters must use to communicate if solving `hostname` the master can not be reached.

When installing Mesos via Ubuntu repository as in the above procedure, the automatically start of service `mesos-slave` is enabled.
Disable this service on master nodes using the following commands.
```bash
service mesos-slave stop
echo "manual" > /etc/init/mesos-slave.override
```

Finally, you must restart the services `zookeeper` and `mesos-master` after editing the configuration.
```bash
service zookeeper restart
service mesos-master restart
```

By connecting to the address `http://master1.example.com:5050` the Mesos master user interface is available, shown below.

![Mesos master user interface](https://i.imgur.com/O69r0S4.png)

#### Mesos Agent
To connect to the master nodes, the Mesos agents must know the Zookeeper addresses of the masters.
You need to edit the file `/etc/mesos/zk` and insert the string `zk://master1.example.com:2181/mesos` if you are using a configuration with only one master node,
or `zk://master1.example.com:2181,master2.example.com:2181,master3.example.com:2181/mesos` if you are using, for example, 3 master nodes.

The configuration of the Mesos slave service is located in the directory `/etc/mesos-slave`.
The name of each file created in this directory will be passed as argument on service startup, while the content will be passed as value.
You can view the list of arguments available on the [documentation](http://mesos.apache.org/documentation/latest/configuration/agent/)
on the Apache Mesos website. The main parameters are listed below.
* `work_dir`: the path of the master directory. Set up `/var/lib/mesos`. **Required** parameter.
* `hostname`: the domain name communicated to the master and used by the tasks. SIf not specified parameter `ip` or `advertise_ip`,
  the agent must be reachable by the master through this value. The system hostname is used by default.
* `ip`: the ip address that the master must use to communicate and which is used instead of `hostname`.
  To obtain the IP address of the system on the interface `eth0` use the command `ifconfig eth0 | awk '/inet addr/{split($2,a,":"); print a[2]}'`.
* `advertise_ip`: the IP address that the master must use to communicate if solving `hostname` the agent can not be reached.
* `containerizers`: the type of container that the tasks can use.
  Enter `docker,mesos` to use both native Mesos containers and Docker containers.

When installing Mesos via Ubuntu repository as in the above procedure, the automatically start of services `zookeeper` and `mesos-master` is enabled.
Disable these services on the agent nodes using the following commands.
```bash
service zookeeper stop
echo "manual" > /etc/init/zookeeper.override
service mesos-master stop
echo "manual" > /etc/init/mesos-master.override
```

Finally, the service `mesos-slave` must be restarted after editing the configuration.
```bash
service mesos-slave restart
```

## Conclusion
The Mesos cluster is ready to be used. It is now possible to [install the Marathon framework](marathon_installation.md) for task management.
