// Verifies that when excludes is configured, matching files are not checked.
// ViolationSample.java is non-conforming but excluded — so the build must succeed.
File buildLog = new File(basedir, "build.log")
assert buildLog.exists() : "build.log must exist"
String buildLogContent = buildLog.text
assert buildLogContent.contains("CHECK_ALL completed") :
        "Build log should contain the JHarmonizer CHECK_ALL completion message, but got:\n" + buildLogContent
