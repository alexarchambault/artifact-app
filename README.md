# artifact-app

Run app from Ivy/artifact modules

[![Build Status](https://travis-ci.org/alexarchambault/artifact-app.svg)](https://travis-ci.org/alexarchambault/artifact-app)

-- 

artifact-app runs app from Ivy/Maven modules, a bit like
[conscript](https://github.com/n8han/conscript) does, but
in a more flexible way. Scala 2.11 applications are supported,
and you don't have to add extra dependencies to your applications,
these are run with standard `main` methods.

## Usage as a library

Add to your `build.sbt`
```scala
resolvers += Resolver.sonatypeRepo("snapshots")

libraryDependencies +=
  "com.github.alexarchambault.artifact" %% "artifact-app" % "0.1.0-SNAPSHOT"
```

The main method exposed by artifact-app is the `apply` method
of the singleton `com.github.alexarchambault.artifact.app.ArtifactApp`
```scala
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
): String \/ (Seq[String] => Unit)
```
which returns either a function that can be called to run the
`mainClass` class from a classpath calculated for the modules
`modules` using the resolvers `resolvers`, or an error message.
Some default
resolvers (sonatype, local, ...) are provided by default, and can be disabled
with the `noDefaultResolvers` and `snapshotResolvers` options.
`scalaVersion` suggests a Scala version to be used, and this version
can be forced with the `forcedScalaVersion` flag (which can
force the version of the Scala JARs on the classpath).
The underlying app can be run from the same JVM, or from
a fork with the `fork` flag. The `printClassPath` and `quiet`
options control the verbosity of the launched app.

Example of usage:
```scala
ArtifactApp(
  resolvers = Nil,
  noDefaultResolvers = false,
  snapshotResolvers = true,
  modules = List(
    "sh.jove %% jove-console % 0.1.0-SNAPSHOT",
    "sh.jove %% jove-scala % 0.1.0-SNAPSHOT"
  ),
  mainClass = "jove.console.JoveConsole",
  scalaVersion = "2.11.5",
  forceScalaVersion = false,
  forkJavaOption = Nil,
  fork = false,
  printClassPath = false,
  quiet = false
) match {
  case -\/(err) =>
    // Failed, 
    Console.err println s"Unable to launch underlying app: $err"
    sys exit 1
  case \/-(main) =>
    main(remainingArgs)
}
```

artifact-app is built for both Scala 2.10 and 2.11.
Launching of forked JVM uses
[jvm-fork](https://github.com/jove-sh/jvm-fork).

## Usage as an app

The easiest way to setup artifact-app is to bootstrap it
with [conscript](https://github.com/n8han/conscript),

    $ cs alexarchambault/artifact-app

Then run it with

    $ artifact-app # arguments ...

Run `artifact-app --help` to get a list of the available options.

Examples:

Run artifact-app with itself, e.g.:

    $ artifact-app -S -M "com.github.alexarchambault.artifact %% artifact-app-cli % 0.1.0-SNAPSHOT" \
        -m com.github.alexarchambault.artifact.app.ArtifactAppCli \
        --scala-version "2.11.5" \
        -- --help

or,

    $ artifact-app -S -M "com.github.alexarchambault.artifact %% artifact-app-cli % 0.1.0-SNAPSHOT" \
        -m com.github.alexarchambault.artifact.app.ArtifactAppCli \
        --scala-version "2.11.5" \
        -- -S -M "com.github.alexarchambault.artifact %% artifact-app-cli % 0.1.0-SNAPSHOT" \
        -m com.github.alexarchambault.artifact.app.ArtifactAppCli \
        --scala-version "2.11.5" \
        -- --help

which is artifact-app, itself launching artifact-app, itself launching artifact-app, itself printing its help message.

More useful examples coming soon.

--

Copyright 2015 Alexandre Archambault

Released under the Apache 2 license.
