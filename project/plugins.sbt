// Examines your Scala code and indicates potential problems with it
addSbtPlugin("org.scalastyle" %% "scalastyle-sbt-plugin" % "1.0.0")

// Provides a customizable release process
addSbtPlugin("com.github.gseitz" % "sbt-release" % "1.0.9")

// Builds and pushes Docker images of the project.
addSbtPlugin("se.marcuslonnberg" % "sbt-docker" % "1.5.0")

// Deploy fat JARs. Restart processes.
addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "0.14.7")
