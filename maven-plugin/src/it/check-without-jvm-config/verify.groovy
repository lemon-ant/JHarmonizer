// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
// Verifies that the plugin works correctly when the user project has no .mvn/jvm.config
// with add-opens flags.  PalantirModuleOpener must have opened jdk.compiler internals
// programmatically so that palantir-java-format's ImportOrderer can access
// com.sun.tools.javac.parser.Tokens$TokenKind without IllegalAccessError.
File buildLog = new File(basedir, "build.log")
assert buildLog.exists() : "build.log must exist"
String buildLogContent = buildLog.text
assert buildLogContent.contains("BUILD SUCCESS") :
        "Build must succeed when the plugin runs without add-opens in .mvn/jvm.config, but got:\n" + buildLogContent
assert !buildLogContent.contains("IllegalAccessError") :
        "Build log must not contain IllegalAccessError — PalantirModuleOpener should have opened jdk.compiler internals:\n" + buildLogContent
assert buildLogContent.contains("CHECK_ALL completed") :
        "Build log should contain the JHarmonizer CHECK_ALL completion message — palantir must have actually run:\n" + buildLogContent
