import sbt._, Keys._

object ArtifactAppBuild extends Build {

  lazy val publishSettings = Seq[Setting[_]](
    publishMavenStyle := true,
    publishTo := {
      val nexus = "https://oss.sonatype.org/"
      if (isSnapshot.value)
        Some("snapshots" at nexus + "content/repositories/snapshots")
      else
        Some("releases"  at nexus + "service/local/staging/deploy/maven2")
    },
    pomExtra := {
      <url>https://github.com/alexarchambault/artifact-app</url>
        <licenses>
          <license>
            <name>Apache 2.0</name>
            <url>http://opensource.org/licenses/Apache-2.0</url>
          </license>
        </licenses>
        <scm>
          <connection>scm:git:github.com/alexarchambault/artifact-app.git</connection>
          <developerConnection>scm:git:git@github.com:alexarchambault/artifact-app.git</developerConnection>
          <url>github.com/alexarchambault/artifact-app.git</url>
        </scm>
        <developers>
          <developer>
            <id>alexarchambault</id>
            <name>Alexandre Archambault</name>
            <url>https://github.com/alexarchambault</url>
          </developer>
        </developers>
    }
  )

  lazy val root = Project(id = "root", base = file("."))
    .settings(
      organization := "com.github.alexarchambault.artifact",
      name := "artifact-app",
      version := "0.1.0-SNAPSHOT",
      scalaVersion := "2.11.5",
      crossScalaVersions := Seq("2.10.4", "2.11.5"),
      resolvers ++= Seq(
        Resolver.sonatypeRepo("releases"),
        Resolver.sonatypeRepo("snapshots"),
        "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/"
      ),
      libraryDependencies ++= Seq(
        "com.github.alexarchambault.jove.sbt" %% "ivy" % "0.13.8-SNAPSHOT",
        "com.github.alexarchambault.jove.sbt" %% "classpath" % "0.13.8-SNAPSHOT",
        "com.github.alexarchambault.jove" %% "jvm-fork" % "0.1.1-SNAPSHOT",
        "org.scalaz" %% "scalaz-core" % "7.1.0"
      )
    )
    .settings(xerial.sbt.Sonatype.sonatypeSettings: _*)
    .settings(publishSettings: _*)

  lazy val cli = Project(id = "cli", base = file("cli"))
    .settings(
      organization := "com.github.alexarchambault.artifact",
      name := "artifact-app-cli",
      version := "0.1.0-SNAPSHOT",
      scalaVersion := "2.11.5",
      crossScalaVersions := Seq("2.10.4", "2.11.5"),
      resolvers ++= Seq(
        Resolver.sonatypeRepo("releases"),
        Resolver.sonatypeRepo("snapshots"),
        "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/"
      ),
      libraryDependencies ++= Seq(
        "com.github.alexarchambault" %% "case-app" % "0.2.1",
        "ch.qos.logback" % "logback-classic" % "1.0.13"
      ),
      libraryDependencies ++= {
        if (scalaVersion.value startsWith "2.10.")
          Seq(
            compilerPlugin("org.scalamacros" % "paradise" % "2.0.1" cross CrossVersion.full)
          )
        else
          Seq()
      }
    )
    .settings(conscript.Harness.conscriptSettings: _*)
    .settings(xerial.sbt.Sonatype.sonatypeSettings: _*)
    .settings(publishSettings: _*)
    .settings(xerial.sbt.Pack.packSettings: _*)
    .settings(xerial.sbt.Pack.packMain := Map(
      "artifact-app" -> "com.github.alexarchambault.artifact.app.ArtifactAppCli"
    ))
    .dependsOn(root)

}
