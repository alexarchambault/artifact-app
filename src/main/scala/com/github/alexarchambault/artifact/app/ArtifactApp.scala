package com.github.alexarchambault.artifact.app

import java.io.File
import java.net.{ URLClassLoader, URI }
import org.slf4j.{Logger, LoggerFactory}
import scalaz._, Scalaz._
import jove.sbt.{ ModuleID, DefaultMavenRepository, Resolver, CrossVersion, Path }
import jove.jvmfork.Fork

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
  ): String \/ (Seq[String] => Unit) =
    temporary(
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
      quiet = quiet,
      extraResolvers = Nil
    )

  @deprecated("Temporary method, apply should be used instead if possible")
  def temporary(
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
    quiet: Boolean,
    extraResolvers: List[Resolver]
  ): String \/ (Seq[String] => Unit) = {
    val defaultResolvers = List(
      Resolver.defaultLocal,
      DefaultMavenRepository,
      Resolver sonatypeRepo "releases"
    )

    val defaultSnapshotResolvers = List(
      Resolver sonatypeRepo "snapshots"
    )

    def forcedCp(_cp: Seq[File], _scalaVersion: Option[String], resolvers: List[Resolver], logger: Logger): String \/ Seq[File] = {
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

        IvyUtil.classPath(_scalaVersion, resolvers, scalaModules, logger) .map { forced =>
          val filter = { f: File =>
            val name = f.getName
            name.startsWith("scala-library-") || name.startsWith("scala-compiler-") || name.startsWith("scala-reflect-")
          }

          _cp.filterNot(filter) ++ forced.filter(filter)
        }
      } else {
        if (!quiet)
          Console.err println s"Warning: no scala JAR found in calculated class path, cannot force scala version"
        \/-(_cp)
      }
    }

    for {
      mainClass0 <- Some(mainClass.trim).filter(_.nonEmpty) toRightDisjunction "No main class specified"
      userResolvers <- Parsers parseResolvers resolvers
      modules <- Parsers parseModules modules
      logger = LoggerFactory getLogger "ArtifactApp"
      resolvers = extraResolvers ++ userResolvers ++ (if (noDefaultResolvers) Nil else defaultResolvers) ++ (if (snapshotResolvers || fork) defaultSnapshotResolvers else Nil)
      _scalaVersion = Some(scalaVersion).filter(_.nonEmpty)
      _cp <- IvyUtil.classPath(
        _scalaVersion,
        resolvers,
        modules ++ (if (fork) Seq(ModuleID("com.github.alexarchambault.jove", "jvm-fork", "0.1.0-SNAPSHOT", Some("compile")).cross(CrossVersion.binary)) else Seq()),
        logger
      )
      forcedOpt <- {
        if (_scalaVersion.nonEmpty && forceScalaVersion) {
          forcedCp(_cp, _scalaVersion, resolvers, logger).map(Some(_))
        } else
          \/-(None)
      }
    } yield {
      if (forceScalaVersion && _scalaVersion.isEmpty && !quiet)
        Console.err println s"Warning: no scala version specified, cannot force scala version"

      val cp = forcedOpt getOrElse _cp

      if (printClassPath) {
        Console.out println "Class path:"
        cp.map(_.getAbsolutePath).sorted.distinct foreach Console.out.println
        Console.out println ""
      }

      if (fork) {
        import concurrent.ExecutionContext.Implicits.global

        args: Seq[String] =>
          val process = Fork(new File("."), forkJavaOption, cp.map(_.getAbsolutePath), mainClass0, args)
          process.waitFor()
          val ret = process.exitValue()
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
