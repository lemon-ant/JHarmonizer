// Verifies that the reorder goal rewrote the source file so that the field now precedes the methods.
File reorderedFile = new File(basedir, "src/main/java/sample/ReorderSample.java")
assert reorderedFile.exists() : "ReorderSample.java should still exist after reorder"

String content = reorderedFile.text
assert content.indexOf("int a = 1;") < content.indexOf("void alpha()") :
        "Field 'a' should appear before method 'alpha' after reorder, but got:\n" + content
assert content.indexOf("void alpha()") < content.indexOf("void zeta()") :
        "Method 'alpha' should appear before method 'zeta' after reorder, but got:\n" + content
