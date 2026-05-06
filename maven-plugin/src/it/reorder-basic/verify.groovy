// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
// Verifies that the reorder goal rewrote the source file so that the field now precedes the methods.
File reorderedFile = new File(basedir, "src/main/java/sample/ReorderSample.java")
assert reorderedFile.exists() : "ReorderSample.java should still exist after reorder"

String content = reorderedFile.text
int fieldAIndex = content.indexOf("int a = 1;")
int alphaIndex = content.indexOf("void alpha()")
int zetaIndex = content.indexOf("void zeta()")

assert fieldAIndex >= 0 :
        "Field 'a' should be present after reorder, but got:\n" + content
assert alphaIndex >= 0 :
        "Method 'alpha' should be present after reorder, but got:\n" + content
assert zetaIndex >= 0 :
        "Method 'zeta' should be present after reorder, but got:\n" + content

assert fieldAIndex < alphaIndex :
        "Field 'a' should appear before method 'alpha' after reorder, but got:\n" + content
assert alphaIndex < zetaIndex :
        "Method 'alpha' should appear before method 'zeta' after reorder, but got:\n" + content
