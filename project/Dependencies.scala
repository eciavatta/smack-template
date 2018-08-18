import Versions._
import sbt._
import scalapb.compiler.Version._

object Dependencies {

  lazy val dependencies = Seq(
    // project direct dependencies
    "com.datastax.cassandra"     %  "cassandra-driver-core"    % cassandraDriverVersion   guavaExclusion(),
    "com.github.scopt"           %% "scopt"                    % scoptVersion,
    "com.sclasen"                %% "akka-zk-cluster-seed"     % akkaZkClusterSeedVersion akkaExclusion() slf4jExclusion() guavaExclusion() nettyExclusion(),
    "com.typesafe.akka"          %% "akka-actor"               % akkaVersion,
    "com.typesafe.akka"          %% "akka-cluster"             % akkaVersion,
    "com.typesafe.akka"          %% "akka-cluster-metrics"     % akkaVersion,
    "com.typesafe.akka"          %% "akka-http"                % akkaHttpVersion,
    "com.typesafe.akka"          %% "akka-http-spray-json"     % akkaHttpVersion,
    "com.typesafe.akka"          %% "akka-remote"              % akkaVersion,
    "com.typesafe.akka"          %% "akka-stream"              % akkaVersion,
    "com.typesafe.akka"          %% "akka-stream-kafka"        % akkaStreamKafkaVersion   akkaExclusion() kafkaClientsExclusion(),
    "io.kamon"                   %% "kamon-akka-remote-2.5"    % kamonAkkaRemoteVersion   configExclusion() akkaExclusion() sourceCodeExclusion(),
    "io.kamon"                   %% "kamon-datadog"            % kamonDatadogVersion      kamonCoreExclusion(),
    "io.kamon"                   %% "kamon-executors"          % kamonExecutorsVersion    configExclusion() sourceCodeExclusion(),
    "io.sentry"                  %  "sentry"                   % sentryVersion            jacksonCoreExclusion() slf4jExclusion(),

    // project test dependencies
    "com.typesafe.akka"          %% "akka-http-testkit"        % akkaHttpVersion   % Test akkaExclusion() akkaTestKitExclusion(),
    "com.typesafe.akka"          %% "akka-testkit"             % akkaVersion       % Test,
    "net.manub"                  %% "scalatest-embedded-kafka" % embKafkaVersion   % Test slf4jExclusion() snappyExclusion() zookeeperExclusion(),
    "org.scalacheck"             %% "scalacheck"               % scalaCheckVersion % Test,
    "org.scalatest"              %% "scalatest"                % scalaTestVersion  % Test,

    // project compile dependencies
    "com.thesamet.scalapb"       %% "scalapb-runtime"          % scalapbVersion    % "protobuf",

    // project intransitive dependencies
    "com.fasterxml.jackson.core" %  "jackson-core"             % jacksonCoreVersion,
    "com.google.guava"           %  "guava"                    % guavaVersion,
    "org.apache.kafka"           %  "kafka-clients"            % kafkaClientsVersion,
  )


  implicit class Exclude(module: ModuleID) {

    def akkaExclusion(): ModuleID = module.exclude("com.typesafe.akka", "akka-actor_2.12")
      .exclude("com.typesafe.akka", "akka-cluster_2.12")
      .exclude("com.typesafe.akka", "akka-protobuf_2.12")
      .exclude("com.typesafe.akka", "akka-remote_2.12")
      .exclude("com.typesafe.akka", "akka-stream_2.12")
    def akkaTestKitExclusion(): ModuleID = module.exclude("com.typesafe.akka", "akka-testkit_2.12")
    def configExclusion(): ModuleID = module.exclude("com.typesafe", "config")
    def guavaExclusion(): ModuleID = module.exclude("com.google.guava", "guava")
    def jacksonCoreExclusion(): ModuleID = module.exclude("com.fasterxml.jackson.core", "jackson-core")
    def kafkaClientsExclusion(): ModuleID = module.exclude("org.apache.kafka", "kafka-clients")
    def kamonCoreExclusion(): ModuleID = module.exclude("io.kamon", "kamon-core_2.12")
    def nettyExclusion(): ModuleID = module.exclude("io.netty", "netty")
    def slf4jExclusion(): ModuleID = module.exclude("org.slf4j", "slf4j-api")
    def snappyExclusion(): ModuleID = module.exclude("org.xerial.snappy", "snappy-java")
    def sourceCodeExclusion(): ModuleID = module.exclude("com.lihaoyi", "sourcecode_2.12")
    def zookeeperExclusion(): ModuleID = module.exclude("org.apache.zookeeper", "zookeeper")

  }

}
