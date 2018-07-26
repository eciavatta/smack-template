// Constants
val akkaVersion = "2.5.0"

// Managed dependencies
val akkaActor = "com.typesafe.akka" %% "akka-actor"  % akkaVersion
val akkaHttp = "com.typesafe.akka" %% "akka-http" % "10.1.3"
val akkaStream = "com.typesafe.akka" %% "akka-stream" % "2.5.12"
val alpakka = "com.typesafe.akka" %% "akka-stream-kafka" % "0.22"
val sprayJson = "com.typesafe.akka" %% "akka-http-spray-json" % "10.1.3"

val akkaRemote = "com.typesafe.akka" %% "akka-remote" % akkaVersion
val scalatest = "org.scalatest"     %% "scalatest"   % "3.0.0"     % "test"

lazy val scalacheck = "org.scalacheck" %% "scalacheck" % "1.14.0"

// add scalastyle to compile task
lazy val compileScalastyle = taskKey[Unit]("compileScalastyle")
// (scalastyleConfig in Compile) := baseDirectory.value /  "project/scalastyle-config.xml"

lazy val commonSettings = Seq(
  organization := "it.eciavatta",
  scalaVersion := "2.12.6",
  version := (version in ThisBuild).value,
  compileScalastyle := scalastyle.in(Compile).toTask("").value,
  (compile in Compile) := ((compile in Compile) dependsOn compileScalastyle).value,
  scalacOptions in Compile ++= Seq("-encoding", "UTF-8", "-feature", "-unchecked", "-Xlog-reflective-calls", "-Xlint")
)

lazy val root = Project(
  id = "smack-template",
  base = file(".")
).aggregate(backend, model)
 .settings(commonSettings: _*)
 .settings(
    libraryDependencies ++= Seq(akkaActor, akkaRemote, scalatest)
  )

lazy val backend = module("backend")
  .dependsOn(model)
  .settings(
    libraryDependencies ++= Seq(alpakka, akkaActor, akkaHttp, akkaStream, sprayJson, scalatest)
  )

lazy val model = module("model")
  .settings(
    libraryDependencies ++= Seq(scalatest)
  )


// add scalastyle to test task
// lazy val testScalastyle = taskKey[Unit]("testScalastyle")
// testScalastyle := scalastyle.in(Test).toTask("").value
// (test in Test) := ((test in Test) dependsOn testScalastyle).value
// (scalastyleConfig in Test) := baseDirectory.value /  "project/scalastyle-test-config.xml"

def module(name: String): Project =
  Project(id = name, base = file(name))
    .settings(commonSettings)
