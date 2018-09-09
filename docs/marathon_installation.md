# Marathon installation
Marathon is a Mesos framework maintained by Mesosphere.
The official documentation is available on the [Mesosphere Marathon](https://mesosphere.github.io/marathon/) website.

## Prerequisites
To refer to this guide, an Ubuntu 16.04 distribution must be installed on each master node
and the service `mesos-master` must be installed as indicated in the [Apache Mesos installation](mesos_installation.md) guide.
Most operations are performed with a user with root privileges, so it is assumed that the shell is started in sudo mode.
The [list of port used](firewall_configuration.md) is available for configuring the firewall.

## Installation
There are two ways to install Marathon: by downloading it from the [official website](https://mesosphere.github.io/marathon/)
or installing it from the Ubuntu repository.
If you choose the second option and you have already followed the procedure indicated in the [Apache Mesos installation](mesos_installation.md) guide
for adding the repository where Mesos is also included, skip this step.

Note: Marathon must be installed only on nodes where the Mesos master service is present.

To add the Ubuntu repository where Marathon is contained, perform the following steps.
```bash
apt-key adv --keyserver keyserver.ubuntu.com --recv E56151BF
DISTRO=$(lsb_release -is | tr '[:upper:]' '[:lower:]')
CODENAME=$(lsb_release -cs)
echo "deb http://repos.mesosphere.io/${DISTRO} ${CODENAME} main" | tee /etc/apt/sources.list.d/mesosphere.list
apt-get update
```
To install Marathon run:
```bash
apt-get install -y marathon
```

The Marathon configuration is contained in the directory `/etc/marathon/conf`.
The name of each file created in this directory will be passed as argument on service startup, while the content will be passed as value.
Below are listed only the mandatory parameters, all the possible options are available in the official documentation in the
[Command line flags](https://mesosphere.github.io/marathon/docs/command-line-flags.html) section.
* `hostname`: the domain name communicated to Mesos and to the other Marathon instances. If not specified, the system hostname is used.
* `master`: the Zookeeper address of the Mesos master in a single master configuration (`zk://master1.example.com:2181/mesos`)
  or the Zookeeper addresses of the Mesos masters in a configuration with multiple masters
  (`zk://master1.example.com:2181,master2.example.com:2181,master3.example.com:2181/mesos`).
* `zk`: the Zookeeper address of all nodes where the Marathon service is executed. In a single master configuration indicate
  `zk://master1.example.com:2181/marathon`, while in a configuration with multiple masters indicate
  `zk://master1.example.com:2181,master2.example.com:2181,master3.example.com:2181/marathon`.

To apply the configuration, you must restart the service `marathon`.
```bash
service marathon restart
```

Marathon to perform tasks does not use the root user. In each mesos agent, you need to add a new user named `marathon`.
```bash
useradd -m -s /bin/bash marathon
```

## Conclusion
After completing the installation, by connecting to the address `http://master1.example.com:8080` you can view the Marathon user interface.

![Marathon user interface](https://i.imgur.com/RDcHxl7.png)
