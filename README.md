# artifact-app

Run app from Ivy/artifact modules

-- 

artifact-app runs app from Ivy/Maven modules, a bit like
[conscript](https://github.com/n8han/conscript) does, but
in a more flexible way. Scala 2.11 applications are supported,
and you don't have to add extra dependencies to your applications,
these are run with standard `main` methods.

## Quick start

For now, the easiest way to setup artifact-app is to bootstrap it
with [conscript](https://github.com/n8han/conscript),

    $ cs alexarchambault/artifact-app

Then run it with

    $ artifact-app # arguments ...

## Usage

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
