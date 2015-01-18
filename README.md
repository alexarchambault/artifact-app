# maven-app

Run app from Ivy/maven modules

## Install

Install with [conscript]

TODO

## Usage

Run `maven-app --help` to get a list of the available options.

Examples:

    $ maven-app -M ""


maven-app can run itself, e.g.:

    $ maven-app -S -M "com.github.alexarchambault %% maven-app % 0.1.0-SNAPSHOT" \
        -m com.github.alexarchambault.mavenapp.MavenApp \
        -- --help

or,

    $ maven-app -S -M "com.github.alexarchambault %% maven-app % 0.1.0-SNAPSHOT" \
        -m com.github.alexarchambault.mavenapp.MavenApp \
        -- -S -M "com.github.alexarchambault %% maven-app % 0.1.0-SNAPSHOT" \
        -m com.github.alexarchambault.mavenapp.MavenApp \
        -- -S -M "com.github.alexarchambault %% maven-app % 0.1.0-SNAPSHOT" \
        -m com.github.alexarchambault.mavenapp.MavenApp \
        -- --help

which is maven-app, launching maven-app, launching maven-app, launching maven-app, printing its help message.