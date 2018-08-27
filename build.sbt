import sbt.file
import sbtassembly.AssemblyPlugin.defaultUniversalScript
import sbtassembly.{AssemblyKeys, MergeStrategy}

val projectName = "smack-template"
val projectVersion = "0.3.0-SNAPSHOT"
val projectOrganization = "it.eciavatta"
val dockerUser = "eciavatta"
val akkaScalaVersion = "2.12.6"
val sparkScalaVersion = "2.11.12"

// add scalastyle to compile task
lazy val compileScalastyle = taskKey[Unit]("compileScalastyle")

lazy val root = Project(
  id = projectName,
  base = file(".")
) .enablePlugins(DockerPlugin)
  .enablePlugins(MultiJvmPlugin)
  .enablePlugins(AssemblyPlugin)
  .settings(assemblySettings: _*)
  .settings(dockerSettings: _*)
  .configs(MultiJvm)
  .enablePlugins(BuildInfoPlugin)
  .settings(Seq(
    buildInfoKeys := Seq[BuildInfoKey](name, version, scalaVersion, sbtVersion),
    buildInfoPackage := s"smack"
  ): _*)
  .settings(commonSettings)
  .settings(
    assemblyJarName in assembly := s"${name.value}-${version.value}.jar",
    libraryDependencies ++= Dependencies.rootDependencies,
    mainClass in assembly := Some("smack.entrypoints.Main"),
    name := projectName,
    scalaVersion := akkaScalaVersion,
  )
  .dependsOn(commons, migrate % "test->test")
  .aggregate(analysis, client, migrate)


lazy val commonSettings = Seq(
  version := projectVersion,
  organization := projectOrganization,

  (compile in Compile) := ((compile in Compile) dependsOn compileScalastyle).value,
  compileScalastyle := scalastyle.in(Compile).toTask("").value,
  conflictManager in ThisBuild := ConflictManager.all,
  parallelExecution in Test := false,
  scalacOptions in Compile ++= Seq("-encoding", "UTF-8", "-feature", "-unchecked", "-Xlog-reflective-calls", "-Xlint"),
  updateOptions := updateOptions.value.withCachedResolution(true),

  test in assembly := {},
  coverageEnabled := false,
  fork in Test := true,

  evictionWarningOptions in update := EvictionWarningOptions.default.withWarnTransitiveEvictions(false)
    .withWarnDirectEvictions(false).withWarnScalaVersionEviction(false)
)

lazy val analysis = module("analysis")
  .settings(
    assemblyJarName in assembly := s"smack-${name.value}-${version.value}.jar",
    libraryDependencies ++= Dependencies.analysisDependencies,
    mainClass in assembly := Some("smack.entrypoints.AnalysisMain"),
    scalaVersion := sparkScalaVersion,
  )
  .enablePlugins(AssemblyPlugin)
  .settings(assemblyAnalyticsSettings: _*)

lazy val client = module("client").dependsOn(commons)
  .settings(
    assemblyJarName in assembly := s"smack-${name.value}-${version.value}.jar",
    libraryDependencies ++= Dependencies.clientDependencies,
    mainClass in assembly := Some("smack.entrypoints.ClientMain"),
    scalaVersion := akkaScalaVersion,
  )
  .enablePlugins(AssemblyPlugin)
  .settings(assemblySettings: _*)

lazy val commons = module("commons")
  .settings(
    libraryDependencies ++= Dependencies.commonsDependencies,
    scalaVersion := akkaScalaVersion,

    PB.targets in Compile := Seq(
      scalapb.gen() -> (sourceManaged in Compile).value / "protobuf"
    ),
    PB.includePaths in Compile += file("commons/src/main/protobuf"),
  )

lazy val migrate = module("migrate").dependsOn(commons)
  .settings(
    assemblyJarName in assembly := s"smack-${name.value}-${version.value}.jar",
    libraryDependencies ++= Dependencies.migrateDependencies,
    mainClass in assembly := Some("smack.entrypoints.MigrateMain"),
    scalaVersion := akkaScalaVersion,
  )
  .enablePlugins(AssemblyPlugin)
  .settings(assemblySettings: _*)

lazy val assemblySettings = Seq(
  assemblyOption in assembly := (assemblyOption in assembly).value.copy(prependShellScript = Some(defaultUniversalScript(shebang = false))),
  assemblyMergeStrategy in assembly := {
    case PathList("META-INF", "io.netty.versions.properties") => MergeStrategy.first
    case PathList("META-INF", "aop.xml") => aopMerge
    case x =>
      val oldStrategy = (assemblyMergeStrategy in assembly).value
      oldStrategy(x)
  }
)

// Taken from http://queirozf.com/entries/creating-scala-fat-jars-for-spark-on-sbt-with-sbt-assembly-plugin
lazy val assemblyAnalyticsSettings = Seq(
  assemblyMergeStrategy in assembly := {
    case PathList("org","aopalliance", xs @ _*) => MergeStrategy.last
    case PathList("javax", "inject", xs @ _*) => MergeStrategy.last
    case PathList("javax", "servlet", xs @ _*) => MergeStrategy.last
    case PathList("javax", "activation", xs @ _*) => MergeStrategy.last
    case PathList("org", "apache", xs @ _*) => MergeStrategy.last
    case PathList("com", "google", xs @ _*) => MergeStrategy.last
    case PathList("com", "esotericsoftware", xs @ _*) => MergeStrategy.last
    case PathList("com", "codahale", xs @ _*) => MergeStrategy.last
    case PathList("com", "yammer", xs @ _*) => MergeStrategy.last
    case "about.html" => MergeStrategy.rename
    case "META-INF/ECLIPSEF.RSA" => MergeStrategy.last
    case "META-INF/mailcap" => MergeStrategy.last
    case "META-INF/mimetypes.default" => MergeStrategy.last
    case "plugin.properties" => MergeStrategy.last
    case "log4j.properties" => MergeStrategy.last
    case "git.properties" => MergeStrategy.last // org.apache.arrow
    case x =>
      val oldStrategy = (assemblyMergeStrategy in assembly).value
      oldStrategy(x)
  }
)

lazy val dockerSettings = Seq(
  docker := (docker dependsOn assembly).value,
  imageNames in docker := Seq(ImageName(s"$dockerUser/$projectName:$projectVersion"), ImageName(s"$dockerUser/$projectName:latest")),
  dockerfile in docker := {
    val mainArtifact = (AssemblyKeys.assemblyOutputPath in assembly).value
    val clientArtifact = ((AssemblyKeys.assemblyOutputPath in client) / assembly).value
    val analysisArtifact = ((AssemblyKeys.assemblyOutputPath in analysis) / assembly).value
    val migrateArtifact = ((AssemblyKeys.assemblyOutputPath in migrate) / assembly).value
    val aspectWeaverArtifact = file("agents/aspectjweaver-1.9.1.jar")
    val runScript = file("scripts/smack-run")

    new sbtdocker.mutable.Dockerfile {
      from("openjdk:8u181-jre")
      copy(Seq(aspectWeaverArtifact, mainArtifact, clientArtifact, analysisArtifact, migrateArtifact, aspectWeaverArtifact, runScript), "/app/")
      // env("JAVA_AGENTS", s"/app/${aspectWeaverArtifact.name}")
      env("VERSION", projectVersion)
      entryPoint("/app/smack-run")
    }
  }
)

// Create a new MergeStrategy for aop.xml files
val aopMerge: MergeStrategy = new MergeStrategy {
  val name = "aopMerge"
  import scala.xml._
  import scala.xml.dtd._

  def apply(tempDir: File, path: String, files: Seq[File]): Either[String, Seq[(File, String)]] = {
    val dt = DocType("aspectj", PublicID("-//AspectJ//DTD//EN", "http://www.eclipse.org/aspectj/dtd/aspectj.dtd"), Nil)
    val file = MergeStrategy.createMergeTarget(tempDir, path)
    val xmls: Seq[Elem] = files.map(XML.loadFile)
    val aspectsChildren: Seq[Node] = xmls.flatMap(_ \\ "aspectj" \ "aspects" \ "_")
    val weaverChildren: Seq[Node] = xmls.flatMap(_ \\ "aspectj" \ "weaver" \ "_")
    val options: String = xmls.map(x => (x \\ "aspectj" \ "weaver" \ "@options").text).mkString(" ").trim
    val weaverAttr = if (options.isEmpty) Null else new UnprefixedAttribute("options", options, Null)
    val aspects = new Elem(null, "aspects", Null, TopScope, false, aspectsChildren: _*)
    val weaver = new Elem(null, "weaver", weaverAttr, TopScope, false, weaverChildren: _*)
    val aspectj = new Elem(null, "aspectj", Null, TopScope, false, aspects, weaver)
    XML.save(file.toString, aspectj, "UTF-8", xmlDecl = false, dt)
    IO.append(file, IO.Newline.getBytes(IO.defaultCharset))
    Right(Seq(file -> path))
  }
}

lazy val buildInfoSettings = Seq(
  buildInfoKeys := Seq[BuildInfoKey]("name" -> s"smack-${name.value}", version, scalaVersion, sbtVersion),
  buildInfoPackage := s"smack.${name.value}"
)

def module(name: String): Project =
  Project(id = name, base = file(name))
    .settings(commonSettings: _*)
    .enablePlugins(BuildInfoPlugin)
    .settings(buildInfoSettings: _*)
