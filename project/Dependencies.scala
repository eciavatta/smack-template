import Versions._
import sbt._
import scalapb.compiler.Version._

object Dependencies {

  // project direct dependencies
  val akkaActor       = "com.typesafe.akka"          %% "akka-actor"                % akkaVersion
  val akkaCluster     = "com.typesafe.akka"          %% "akka-cluster"              % akkaVersion
  val akkaClusterSeed = "com.sclasen"                %% "akka-zk-cluster-seed"      % akkaZkClusterSeedVersion akkaClusterSeedExclusions()
  val akkaHttp        = "com.typesafe.akka"          %% "akka-http"                 % akkaHttpVersion
  val akkaMetrics     = "com.typesafe.akka"          %% "akka-cluster-metrics"      % akkaVersion
  val akkaRemote      = "com.typesafe.akka"          %% "akka-remote"               % akkaVersion
  val akkaSprayJson   = "com.typesafe.akka"          %% "akka-http-spray-json"      % akkaHttpVersion
  val akkaStream      = "com.typesafe.akka"          %% "akka-stream"               % akkaVersion
  val akkaStreamKafka = "com.typesafe.akka"          %% "akka-stream-kafka"         % akkaStreamKafkaVersion   akkaExclude() kafkaClientsExclude()
  val cassandraDriver = "com.datastax.cassandra"     %  "cassandra-driver-core"     % cassandraDriverVersion   guavaExclude()
  val kamonAkkaRemote = "io.kamon"                   %% "kamon-akka-remote-2.5"     % kamonAkkaRemoteVersion   configExclude() akkaExclude() sourceCodeExclude()
  val kamonDatadog    = "io.kamon"                   %% "kamon-datadog"             % kamonDatadogVersion      kamonCoreExclude()
  val kamonExecutors  = "io.kamon"                   %% "kamon-executors"           % kamonExecutorsVersion    configExclude() sourceCodeExclude()
  val scopt           = "com.github.scopt"           %% "scopt"                     % scoptVersion
  val sentry          = "io.sentry"                  %  "sentry"                    % sentryVersion            jacksonCoreExclude() slf4jExclude()
  val uuidGenerator   = "com.fasterxml.uuid"         %  "java-uuid-generator"       % uuidGeneratorVersion

  // project test depedencies
  val akkaHttpTestkit = "com.typesafe.akka"          %% "akka-http-testkit"         % akkaHttpVersion   % Test akkaExclude() akkaTestKitExclude()
  val akkaMulTestkit  = "com.typesafe.akka"          %% "akka-multi-node-testkit"   % akkaVersion       % Test
  val akkaTestkit     = "com.typesafe.akka"          %% "akka-testkit"              % akkaVersion       % Test
  val embeddedKafka   = "net.manub"                  %% "scalatest-embedded-kafka"  % embKafkaVersion   % Test slf4jExclude() snappyExclude() zookeeperExclude()
  val scalacheck      = "org.scalacheck"             %% "scalacheck"                % scalaCheckVersion % Test
  val scalatest       = "org.scalatest"              %% "scalatest"                 % scalaTestVersion  % Test

  // project compile dpendencies
  val scalapb         = "com.thesamet.scalapb"       %% "scalapb-runtime"           % scalapbVersion    % "protobuf"

  // project intransitve dependencies
  val guava           = "com.google.guava"           %  "guava"                     % guavaVersion
  val jacksonCore     = "com.fasterxml.jackson.core" %  "jackson-core"              % jacksonCoreVersion
  val kafkaClients    = "org.apache.kafka"           %  "kafka-clients"             % kafkaClientsVersion

  // analysis dependenies
  val sparkCassandra  = "com.datastax.spark"         %% "spark-cassandra-connector" % sparkVersion exclude("io.netty", "netty-all")
  val sparkCore       = "org.apache.spark"           %% "spark-core"                % sparkVersion exclude("commons-beanutils", "commons-beanutils")
  val sparkSql        = "org.apache.spark"           %% "spark-sql"                 % sparkVersion             jacksonCoreExclude()
  val quartz          = "org.quartz-scheduler"       % "quartz"                     % quartzVersion slf4jExclude()

  val rootDependencies= Seq(akkaActor, akkaCluster, akkaClusterSeed, akkaHttp, akkaMetrics, akkaRemote, akkaSprayJson, akkaStream, akkaStreamKafka,
                             cassandraDriver, kamonAkkaRemote, kamonDatadog, kamonExecutors, scopt, uuidGenerator,
                             akkaHttpTestkit, akkaMulTestkit, akkaTestkit, embeddedKafka, scalacheck, scalatest,
                             guava, jacksonCore, kafkaClients)

  val analysisDependencies = Seq(sparkCassandra, sparkCore, scopt, quartz)

  val clientDependencies = Seq(akkaActor, akkaActor, akkaHttp, akkaSprayJson, akkaStream, scopt)

  val commonsDependencies = Seq(akkaActor, akkaStream, akkaSprayJson, sentry, scalapb, scopt)

  val migrateDependencies = Seq(akkaActor, cassandraDriver, akkaTestkit, scalatest, scopt)

  implicit class Exclusions(module: ModuleID) {

    def akkaExclude(): ModuleID = module.exclude("com.typesafe.akka", "akka-actor_2.12")
      .exclude("com.typesafe.akka", "akka-cluster_2.12")
      .exclude("com.typesafe.akka", "akka-protobuf_2.12")
      .exclude("com.typesafe.akka", "akka-remote_2.12")
      .exclude("com.typesafe.akka", "akka-stream_2.12")
    def akkaTestKitExclude(): ModuleID = module.exclude("com.typesafe.akka", "akka-testkit_2.12")
    def configExclude(): ModuleID = module.exclude("com.typesafe", "config")
    def guavaExclude(): ModuleID = module.exclude("com.google.guava", "guava")
    def jacksonCoreExclude(): ModuleID = module.exclude("com.fasterxml.jackson.core", "jackson-core")
    def kafkaClientsExclude(): ModuleID = module.exclude("org.apache.kafka", "kafka-clients")
    def kamonCoreExclude(): ModuleID = module.exclude("io.kamon", "kamon-core_2.12")
    def nettyExclude(): ModuleID = module.exclude("io.netty", "netty")
    def slf4jExclude(): ModuleID = module.exclude("org.slf4j", "slf4j-api")
    def snappyExclude(): ModuleID = module.exclude("org.xerial.snappy", "snappy-java")
    def sourceCodeExclude(): ModuleID = module.exclude("com.lihaoyi", "sourcecode_2.12")
    def zookeeperExclude(): ModuleID = module.exclude("org.apache.zookeeper", "zookeeper")
    def akkaClusterSeedExclusions(): ModuleID = akkaExclude() slf4jExclude() guavaExclude() nettyExclude()

  }

}
