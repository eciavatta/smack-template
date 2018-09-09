# smack-template
[![Build Status](https://travis-ci.com/eciavatta/smack-template.svg?token=nfQhLp6KpfsSDXWSqbxN&branch=develop)](https://travis-ci.com/eciavatta/smack-template)

smack-template is a case study of an application built with the SMACK stack.

## Content
Within the project there are several modules:
* [smack-template](/docs/smack_template.md), the main module that contains the application sources
* [smack-analysis](/docs/smack_analysis.md), the Spark application to perform analysis on the model
* [smack-client](/docs/smack_client.md), a tool that can be used to test the smack system
* [smack-migrate](/docs/smack_migrate.md), a tool to migrate the database schema

## Commands
The project was developed using sbt. Several commands are available, or tasks; the main ones are listed below.
* `sbt clean`, clean the compiled project files
* `sbt compile`, compile all the project modules
* `sbt dependencyList`, shows the dependencies of the project
* `sbt test`, performs the project tests
* `sbt multi-jvm:test`, performs integration tests
* `sbt clean coverage test multi-jvm:test coverageReport`, performs all tests and generates the coverage ratio
* `sbt assembly`, generates the fat jar for each module
* `sbt-docker`, creates a Docker image of the application

## Information
The list of project dependencies, subdivided by modules, is contained in [Dependencies](/project/Dependencies.scala),
while the related versions are specified in [Versions](/project/Versions.scala).
To change the project name, the project version or the name of the Docker user to create the application image,
you can change the contents of the variables in [build.sbt](build.sbt).
ScalaStyle is used to verify the style of the code; the list of rules used is available in [scalastyle-config.xml](scalastyle-config.xml).

## Test
To perform the tests it is necessary to have at least one instance of Apache Kafka and an instance of Apache Cassandra active in the local machine.
You can launch the necessary components through the command `docker-compose up -d`.
On [docker-compose.testing.yml](docker-compose.testing.yml) it is contained the configuration of docker-compose to create an application test environment,
which can be activated with the command `docker-compose -f docker-compose.yml -f docker-compose.testing.yml up -d`.

## Other documentation
* [Apache Mesos installation](/docs/mesos_installation.md)
* [Marathon installation](/docs/marathon_installation.md)
* [Firewall configuration](/docs/firewall_configuration.md)
* [Cluster deployment](/docs/cluster_deployment.md)
