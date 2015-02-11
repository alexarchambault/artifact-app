package com.github.alexarchambault.artifact.app

import caseapp._
import scalaz.{\/-, -\/}

  @AppName("ArtifactApp")
  @ProgName("artifact-app")
case class ArtifactAppCli(
  @ExtraName("R") resolvers: List[String],
  @ExtraName("N") noDefaultResolvers: Boolean,
  @ExtraName("S") snapshotResolvers: Boolean,
  @ExtraName("M") modules: List[String],
  @ExtraName("m") mainClass: String,
  scalaVersion: String,
  forceScalaVersion: Boolean,
  @ExtraName("J") forkJavaOption: List[String],
  fork: Boolean,
  printClassPath: Boolean,
  @ExtraName("n") dummy: Boolean,
  @ExtraName("q") quiet: Boolean
) extends App {

  ArtifactApp(
    resolvers = resolvers,
    noDefaultResolvers = noDefaultResolvers,
    snapshotResolvers = snapshotResolvers,
    modules = modules,
    mainClass = mainClass,
    scalaVersion = scalaVersion,
    forceScalaVersion = forceScalaVersion,
    forkJavaOption = forkJavaOption,
    fork = fork,
    printClassPath = printClassPath,
    quiet = quiet
  ) match {
    case -\/(err) =>
      Console.err println s"$err"
      sys exit 1
    case \/-(main) =>
      if (!dummy)
        main(remainingArgs)
  }

}

object ArtifactAppCli extends AppOf[ArtifactAppCli] {
  val parser = default
}

class ArtifactAppCliConscriptLaunch extends xsbti.AppMain {
  def run(config: xsbti.AppConfiguration) =
    try {
      ArtifactAppCli.main(config.arguments)
      Exit(0)
    } catch {
      case _: Exception =>
        Exit(1)
    }

  case class Exit(code: Int) extends xsbti.Exit
}
