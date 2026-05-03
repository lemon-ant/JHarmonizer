/*
 * SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 */
// Verifies that the check goal fails with the JHarmonizer ordering violation message.
// The invoker.buildResult=failure in invoker.properties declares that this build is expected to fail.
File buildLog = new File(basedir, "build.log")
assert buildLog.exists() : "build.log must exist"
String buildLogContent = buildLog.text
assert buildLogContent.contains("do not conform to the configured ordering") :
        "Build log should contain the JHarmonizer violation message, but got:\n" + buildLogContent
