package com.github.alexarchambault.artifact.app

import java.io.File
import java.net.{ URLClassLoader, URI }
import org.slf4j.LoggerFactory
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scalaz._, Scalaz._
import jove.sbt.{ ModuleID, DefaultMavenRepository, Resolver, CrossVersion, Path }
import jove.jvmfork.{ ProcessInfo, Fork }

object Parsers {
  // TODO Use the resolver parser from conscript?
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
      try {
        val uri = new URI(s)

        if (uri.getScheme == "ssh" || uri.getScheme == "sftp") {
          val params = uri.getQuery.split("&").map(_.split("=", 2)).collect{case Array(k, v) => k -> v }.toMap
          val r =
            if (uri.getScheme == "ssh")
              Resolver.ssh(params.getOrElse("name", s), uri.getHost, uri.getPath)
            else
              Resolver.sftp(params.getOrElse("name", s), uri.getHost, uri.getPath)

          val u = uri.getUserInfo

          {
            if (u contains ":") {
              val t = u.split(":", 2)
              r.as(t(0), t(1))
            } else if (params contains "pkey")
              r.as(u, new File(params("pkey")))
            else
              r
          } .right
        } else
          s"Unrecognized resolver: $s".left
      } catch {
        case _: Exception =>
          s"Unrecognized resolver: $s".left
      }

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

object ArtifactApp {

  def apply(
    resolvers: List[String],
    noDefaultResolvers: Boolean,
    snapshotResolvers: Boolean,
    modules: List[String],
    mainClass: String,
    scalaVersion: String,
    forceScalaVersion: Boolean,
    forkJavaOption: List[String],
    fork: Boolean,
    printClassPath: Boolean,
    quiet: Boolean
  ): String \/ (Seq[String] => Unit) = {
    val defaultResolvers = List(
      Resolver.defaultLocal,
      DefaultMavenRepository,
      Resolver sonatypeRepo "releases"
    )

    val defaultSnapshotResolvers = List(
      Resolver sonatypeRepo "snapshots"
    )

    for {
      mainClass0 <- Some(mainClass.trim).filter(_.nonEmpty) toRightDisjunction "No main class specified"
      userResolvers <- Parsers parseResolvers resolvers
      modules <- Parsers parseModules modules
    } yield {
      val logger = LoggerFactory getLogger "ArtifactApp"

      val resolvers = userResolvers ++ (if (noDefaultResolvers) Nil else defaultResolvers) ++ (if (snapshotResolvers || fork) defaultSnapshotResolvers else Nil)
      val _scalaVersion = Some(scalaVersion).filter(_.nonEmpty)

      val _cp = IvyUtil.classPath(
        _scalaVersion,
        resolvers,
        modules ++ (if (fork) Seq(ModuleID("com.github.alexarchambault.jove", "jvm-fork", "0.1.0-SNAPSHOT", Some("compile")).cross(CrossVersion.binary)) else Seq()),
        logger
      )

      if (forceScalaVersion && _scalaVersion.isEmpty && !quiet)
        Console.err println s"Warning: no scala version specified, cannot force scala version"

      val cp =
        if (_scalaVersion.nonEmpty && forceScalaVersion) {
          // FIXME Filtering could be more specific here

          val hasLibrary = _cp.exists(_.getName startsWith "scala-library-")
          val hasReflect = _cp.exists(_.getName startsWith "scala-reflect-")
          val hasCompiler = _cp.exists(_.getName startsWith "scala-compiler-")

          if (hasLibrary || hasReflect || hasCompiler) {
            var scalaModules = List.empty[ModuleID]

            if (hasLibrary)
              scalaModules = ModuleID("org.scala-lang", "scala-library", _scalaVersion.get, Some("compile")) :: scalaModules

            if (hasReflect)
              scalaModules = ModuleID("org.scala-lang", "scala-reflect", _scalaVersion.get, Some("compile")) :: scalaModules

            if (hasCompiler)
              scalaModules = ModuleID("org.scala-lang", "scala-compiler", _scalaVersion.get, Some("compile")) :: scalaModules

            val forced = IvyUtil.classPath(_scalaVersion, resolvers, scalaModules, logger)

            val filter = { f: File =>
              val name = f.getName
              name.startsWith("scala-library-") || name.startsWith("scala-compiler-") || name.startsWith("scala-reflect-")
            }

            if (!quiet)
              Console.err println s"Forced scala version to ${_scalaVersion.get}"

            _cp.filterNot(filter) ++ forced.filter(filter)
          } else {
            if (!quiet)
              Console.err println s"Warning: no scala JAR found in calculated class path, cannot force scala version"
            _cp
          }
        } else
          _cp

      if (printClassPath) {
        Console.out println "Class path:"
        cp.map(_.getAbsolutePath).sorted.distinct foreach Console.out.println
        Console.out println ""
      }

      if (fork) {
        import concurrent.ExecutionContext.Implicits.global

        args: Seq[String] =>
          val f = Fork(new File("."), forkJavaOption, cp.map(_.getAbsolutePath), mainClass0, args)
          val ProcessInfo(_, completion) = Await.result(f, Duration.Inf)
          val ret = Await.result(completion, Duration.Inf)
          if (ret != 0)
            sys exit ret
          ()
      } else {
        val classLoader = new URLClassLoader(Path.toURLs(cp), null)
        val clazz = classLoader.loadClass(mainClass0)
        val mainMethod = clazz.getMethod("main", classOf[Array[String]])

        args: Seq[String] =>
          Thread.currentThread setContextClassLoader classLoader
          mainMethod.invoke(null, args.toArray)
          ()
      }
    }
  }

}
