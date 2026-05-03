/*
 * SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 */
// Verifies that the check goal succeeds when all source files already conform to the configured ordering.
File buildLog = new File(basedir, "build.log")
assert buildLog.exists() : "build.log must exist"
String buildLogContent = buildLog.text
assert buildLogContent.contains("CHECK_ALL completed") :
        "Build log should contain the JHarmonizer CHECK_ALL completion message, but got:\n" + buildLogContent
