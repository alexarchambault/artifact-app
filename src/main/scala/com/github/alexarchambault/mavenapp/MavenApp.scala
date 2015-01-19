package com.github.alexarchambault.mavenapp

import java.io.File
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scalaz._, Scalaz._
import sbt.{ ModuleID, DefaultMavenRepository, Resolver, CrossVersion }
import org.slf4j.LoggerFactory
import jove.jvmfork.Fork
import caseapp._

object Parsers {
  def parseResolver(s: String): String \/ Resolver =
    if (s startsWith "sonatype:")
      Resolver.sonatypeRepo(s drop "sonatype:".length).right
    else if (s startsWith "file:")
      Resolver.file(s drop "file:".length).right
    else if (s == "local")
      Resolver.defaultLocal.right
    else if (s == "default-maven")
      DefaultMavenRepository.right
    else
      s"Unrecognized resolver: $s".left

  def parseResolvers(l: List[String]): String \/ List[Resolver] =
    l.map(_.trim).filter(_.nonEmpty).flatMap(_ split ',') traverseU parseResolver

  def parseModule(s: String): String \/ ModuleID =
    s.split('%').map(_.trim) match {
      case Array(organization, "", name, revision) =>
        ModuleID(organization, name, revision, Some("compile")).cross(CrossVersion.binary).right
      case Array(organization, name, revision) =>
        ModuleID(organization, name, revision, Some("compile")).right
      case _ =>
        s"Unrecognized module: $s".left
    }

  def parseModules(l: List[String]): String \/ List[ModuleID] =
    l.map(_.trim).filter(_.nonEmpty).flatMap(_ split ',') traverseU parseModule
}

case class MavenApp(
  @ExtraName("R") resolvers: List[String],
  @ExtraName("N") noDefaultResolvers: Boolean,
  @ExtraName("S") snapshotResolvers: Boolean,
  @ExtraName("M") modules: List[String],
  @ExtraName("m") mainClass: String,
  scalaVersion: String,
  @ExtraName("J") javaOption: List[String],
  fork: Boolean
) extends App {

  val defaultResolvers = List(
    Resolver.defaultLocal,
    DefaultMavenRepository,
    Resolver sonatypeRepo "releases"
  )

  val defaultSnapshotResolvers = List(
    Resolver sonatypeRepo "snapshots"
  )

  val r =
    for {
      mainClass0 <- Some(mainClass.trim).filter(_.nonEmpty) toRightDisjunction "No main class specified"
      userResolvers <- Parsers parseResolvers resolvers
      modules <- Parsers parseModules modules
    } yield {
      val cp = IvyUtil.classPath(
        Some(scalaVersion).filter(_.nonEmpty),
        userResolvers ++ (if (noDefaultResolvers) Nil else defaultResolvers) ++ (if (snapshotResolvers) defaultSnapshotResolvers else Nil),
        modules,
        LoggerFactory getLogger "MavenApp"
      )

      if (fork) {
        import concurrent.ExecutionContext.Implicits.global

        args: Seq[String] =>
          val f = Fork(new File("."), javaOption, cp.map(_.getAbsolutePath), mainClass0, args)
          Await.result(f, Duration.Inf)
          ()
      } else {
        val classLoader = IvyUtil.classLoader(cp, None)
        val clazz = classLoader.loadClass(mainClass0)
        val mainMethod = clazz.getMethod("main", classOf[Array[String]])

        args: Seq[String] =>
          mainMethod.invoke(null, args.toArray)
          ()
      }
    }

  r match {
    case -\/(err) =>
      Console.err println s"$err"
      sys exit 1
    case \/-(main) =>
      main(remainingArgs)
  }
}

object MavenApp extends AppOf[MavenApp] {
  val parser = default
}
