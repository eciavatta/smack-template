# smack-analysis
Spark application to perform analysis on the model.

## Prerequisites
The final jar of this application, once created with the command `sbt assembly`, does not contain the [Apache Spark](http://spark.apache.org/) binaries.
Therefore, to run the application in the cluster, it is necessary to install Spark in the node used by the driver (where the application is launched)
and in all possible nodes used as executors (where the application is executed).
To install Spark on all nodes you can download the version already compiled from the official Apache website,
in the [Downloads](http://spark.apache.org/downloads.html) section.
Then extract the archive obtained in a path common to all nodes, for example in `/opt/spark`.
You must have Java 8 installed and Apache Mesos configured on all nodes to start Spark in clustered mode.
The reference guide for configuring the nodes is here: [Mesos installation](mesos_installation.md).

## Usage
```
smack-analysis 0.3.0-SNAPSHOT
Usage: smack-analysis [options]

  -c, --cassandra-bootstrap <addr>
                           The cassandra contact point/s (default: 127.0.0.1:9042)
  -k, --keyspace <name>    The keyspace name of the Cassandra database (default: smackdev)
  -l, --loglevel <level>   The log level used by standard output and (optionally) by sentry logger (default: info)
  --sentry-dns <key>       If defined, every logs are sent to sentry servers and can be viewed on Sentry.io. The standard output remain unchanged
  --help                   Display help
Spark application to perform analysis on the model.
```
To start the application, both in single mode and in cluster mode, you must use `spark-submit` located in Spark binaries.
An example of a script to launch the application on cluster mode through Apache Mesos is available here: [run-analysis](/scripts/run-analysis).
At boot time the following environment variables must be present:
```bash
# The path where the Spark binaries are located, which must be present in all the nodes, both drivers and executors
SPARK_HOME=/opt/spark

# The path where the Mesos libraries must be located
MESOS_NATIVE_JAVA_LIBRARY=/usr/local/lib/libmesos.so

# The ip address of the node, to be defined when the node's hostname is resolved with a private address
SPARK_LOCAL_IP=x.x.x.x

# The public domain name to be communicated to the executors
SPARK_PUBLIC_DNS=master1.example.com
```

## Behavior
When the application starts a scheduler is created; the scheduler is used to start jobs in regular time intervals or at specific times.
A job is the representation of a quantity of work to be performed in a distributed manner on several Spark executors.
An example of a job can be the grouping of data into a table in the Cassandra database for a column that is not the partition key.
A trigger is associated to each job, which triggers when a job must be executed.
When the trigger is triggered, the associated job is inserted into the Spark job queue.
Spark's driver takes care of subdividing the job into steps, ie. elementary work units, which it will send to the executors.
When the application is started by connecting to the address `http://master1.example.com:4040` you can view the Spark user interface, as shown in the figure.

![Spark user interface](https://i.imgur.com/iuGwUlG.png)

## Structure
Each job must implement the trait `SparkJob` and must be placed in the [jobs](/analysis/src/main/scala/smack/analysis/jobs) package.
The job must implement the method `run(): Unit` that is called whenever the trigger associated with this job is triggered.
Two variables are available within the job, `sparkContext: SparkContext` which must be used to perform operations that are distributed in the cluster
and `currentContext: JobExecutionContext` that contain information about the current execution of the job.
All jobs must be initialized in the [AnalysisSupervisor](/analysis/src/main/scala/smack/analysis/AnalysisSupervisor.scala) class.
For each job it is possible to inject further information through the `JobDataMap`.
You can find documentation on how to do this on the
[Quartz documentation](http://www.quartz-scheduler.org/documentation/quartz-2.x/tutorials/tutorial-lesson-03.html).
Each job to be executed must have a trigger associated. To run a job every 5 minutes, the expression to use is `0 0/5 * * * ?`.
For complete documentation, please refer to [CronTrigger](http://www.quartz-scheduler.org/documentation/quartz-2.x/tutorials/tutorial-lesson-06.html).

Spark's documentation on how to correctly write a job is available on the official [Apache Spark](http://spark.apache.org/docs/latest/) website.
To connect to the Cassandra database it is used the `spark-cassandra-connector` driver.
The documentation of this library, maintained by Datastax, is available on Github at this address
[spark-cassandra-connector](https://github.com/datastax/spark-cassandra-connector/blob/master/doc/0_quick_start.md).
Finally, you can find an example of a job in the [StatsCollectorJob](/analysis/src/main/scala/smack/analysis/jobs/StatsCollectorJob.scala) class.
