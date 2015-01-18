package com.github.alexarchambault.mavenapp

import java.io.File
import org.slf4j.Logger
import sbt.Configurations._
import sbt.cross.CrossVersionUtil
import sbt._

object IvyUtil {
  private def fakeModule(deps: Seq[ModuleID], scalaFullVersion: Option[String], ivySbt: IvySbt): IvySbt#Module = {
    val ivyScala = scalaFullVersion map { fv =>
      new IvyScala(
        scalaFullVersion = fv,
        scalaBinaryVersion = CrossVersionUtil.binaryScalaVersion(fv),
        configurations = Nil,
        checkExplicit = true,
        filterImplicit = false,
        overrideScalaVersion = false
      )
    }

    val moduleSetting: ModuleSettings = InlineConfiguration(
      module = ModuleID("com.example.temp", "fake", "0.1.0-SNAPSHOT", Some("compile")),
      moduleInfo = ModuleInfo(""),
      dependencies = deps,
      configurations = Seq(Compile, Test, Runtime),
      ivyScala = ivyScala
    )

    new ivySbt.Module(moduleSetting)
  }

  private def update(scalaVersion: Option[String], resolvers: Seq[Resolver], modules: Seq[ModuleID], logger: Logger): UpdateReport = {
    lazy val ivyConfiguration: IvyConfiguration =
      new InlineIvyConfiguration(
        new IvyPaths(new File("."), Option(sys props "user.home").map(p => new File(new File(p), ".ivy2"))),
        resolvers,
        Nil,
        Nil,
        false,
        None,
        Nil,
        None,
        UpdateOptions(),
        logger
      )

    val ivySbt = new IvySbt(ivyConfiguration)

    IvyActions.update(
      fakeModule(modules, scalaVersion, ivySbt),
      new UpdateConfiguration(None, false, UpdateLogging.DownloadOnly),
      logger
    )
  }

  def classPath(scalaVersion: Option[String], resolvers: Seq[Resolver], modules: Seq[ModuleID], logger: Logger): Seq[File] =
    update(scalaVersion, resolvers, modules, logger).toSeq.map(_._4)

  def classLoader(cp: Seq[File], sharedLoaderOption: Option[ClassLoader]): ClassLoader =
    sbt.classpath.ClasspathUtilities.makeLoader(
      cp,
      sharedLoaderOption.orNull,
      sharedLoaderOption.orNull,
      Nil,
      { val dir = IO.createTemporaryDirectory; dir.deleteOnExit(); dir }
    )
}
