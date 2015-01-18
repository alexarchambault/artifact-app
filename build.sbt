organization := "com.github.alexarchambault"

name := "maven-app"

version := "0.1.0-SNAPSHOT"

scalaVersion := "2.11.5"

crossScalaVersions := Seq("2.10.4", "2.11.5")

resolvers ++= Seq(
  Resolver.sonatypeRepo("releases"),
  Resolver.sonatypeRepo("snapshots")
)

libraryDependencies ++= Seq(
  "com.github.alexarchambault.sbt" %% "ivy" % "0.13.8-SNAPSHOT",
  "com.github.alexarchambault.sbt" %% "classpath" % "0.13.8-SNAPSHOT",
  "com.github.alexarchambault.jove" %% "jvm-fork" % "0.1.0-SNAPSHOT",
  "com.github.alexarchambault" %% "case-app" % "0.1.1-SNAPSHOT",
  "org.scalaz" %% "scalaz-core" % "7.1.0",
  "ch.qos.logback" % "logback-classic" % "1.0.13"
)

libraryDependencies ~= { _.map(_.excludeAll(ExclusionRule("org.slf4j", "slf4j-log4j12"))) }

// For case-app to work properly
libraryDependencies ++= {
  if (scalaVersion.value startsWith "2.10.")
    Seq(
      compilerPlugin("org.scalamacros" % "paradise" % "2.0.1" cross CrossVersion.full)
    )
  else
    Seq()
}

xerial.sbt.Sonatype.sonatypeSettings

publishMavenStyle := true

publishTo := {
  val nexus = "https://oss.sonatype.org/"
  if (isSnapshot.value)
    Some("snapshots" at nexus + "content/repositories/snapshots")
  else
    Some("releases"  at nexus + "service/local/staging/deploy/maven2")
}

pomExtra := {
  <url>https://github.com/alexarchambault/maven-app</url>
    <licenses>
      <license>
        <name>Apache 2.0</name>
        <url>http://opensource.org/licenses/Apache-2.0</url>
      </license>
    </licenses>
    <scm>
      <connection>scm:git:github.com/alexarchambault/maven-app.git</connection>
      <developerConnection>scm:git:git@github.com:alexarchambault/maven-app.git</developerConnection>
      <url>github.com/alexarchambault/maven-app.git</url>
    </scm>
    <developers>
      <developer>
        <id>alexarchambault</id>
        <name>Alexandre Archambault</name>
        <url>https://github.com/alexarchambault</url>
      </developer>
    </developers>
}