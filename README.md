# maven-app

Run app from Ivy/maven modules

## Quick start

    $ git clone https://github.com/alexarchambault/maven-app.git
    $ cd maven-app
    $ sbt pack
    $ ./target/pack/bin/maven-app --help

[conscript](https://github.com/n8han/conscript) install coming soon.

## Usage

Run `maven-app --help` to get a list of the available options.

Examples:

Run maven-app with itself, e.g.:

    $ maven-app -S -M "com.github.alexarchambault %% maven-app % 0.1.0-SNAPSHOT" \
        -m com.github.alexarchambault.mavenapp.MavenApp \
        --scala-version "2.11.5" \
        -- --help

or,

    $ maven-app -S -M "com.github.alexarchambault %% maven-app % 0.1.0-SNAPSHOT" \
        -m com.github.alexarchambault.mavenapp.MavenApp \
        --scala-version "2.11.5" \
        -- -S -M "com.github.alexarchambault %% maven-app % 0.1.0-SNAPSHOT" \
        -m com.github.alexarchambault.mavenapp.MavenApp \
        --scala-version "2.11.5" \
        -- -S -M "com.github.alexarchambault %% maven-app % 0.1.0-SNAPSHOT" \
        -m com.github.alexarchambault.mavenapp.MavenApp \
        --scala-version "2.11.5" \
        -- --help

which is maven-app, itself launching maven-app, itself launching maven-app, itself launching maven-app, itself printing its help message.

More useful examples coming soon.
