<!--
SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
SPDX-License-Identifier: Apache-2.0
-->

# Gradle plugin (not implemented)

JHarmonizer ships two front-ends for `jharmonizer-core`:

- the CLI fat JAR (`jharmonizer-cli`) — see [`08-CliRunner.md`](08-CliRunner.md);
- the Maven plugin (`jharmonizer-maven-plugin`) — see [`09-Maven-plugin.md`](09-Maven-plugin.md).

A Gradle plugin is **not** part of the project. There is no `build.gradle*` module and
no published Gradle plugin coordinates. Contributions adding a Gradle front-end on top
of `jharmonizer-core` would be welcome, but until one exists, Gradle users should
either invoke the CLI fat JAR from a custom Gradle task or run JHarmonizer from a
separate Maven invocation.

If or when a Gradle plugin is added, this document will be replaced with the actual
DSL and lifecycle-hook reference.
