/*
 * SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 */
// Verifies that when baseDirs is explicitly configured to a non-default directory,
// only files in that directory are checked — not files in the default src/main/java.
// The configured directory (src/custom/java) contains only conforming files, so the
// build must succeed even though src/main/java contains a non-conforming file.
File buildLog = new File(basedir, "build.log")
assert buildLog.exists() : "build.log must exist"
String buildLogContent = buildLog.text
assert buildLogContent.contains("CHECK_ALL completed") :
        "Build log should contain the JHarmonizer CHECK_ALL completion message, but got:\n" + buildLogContent

// The violation file in the default directory must be untouched (not modified or reported).
File violationFile = new File(basedir, "src/main/java/sample/ViolationInDefaultDir.java")
assert violationFile.exists() : "ViolationInDefaultDir.java should still exist"
String violationContent = violationFile.text
assert violationContent.contains("void zeta()") :
        "ViolationInDefaultDir.java should be unchanged (not reordered), but got:\n" + violationContent
int zetaIndex = violationContent.indexOf("void zeta()")
int fieldIndex = violationContent.indexOf("int a = 1;")
assert zetaIndex < fieldIndex :
        "ViolationInDefaultDir.java should remain non-conforming (zeta before field), confirming it was not scanned"
