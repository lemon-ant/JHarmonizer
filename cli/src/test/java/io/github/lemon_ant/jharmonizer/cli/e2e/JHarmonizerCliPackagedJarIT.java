package io.github.lemon_ant.jharmonizer.cli.e2e;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class JHarmonizerCliPackagedJarIT {

    private static final String BASIC_PROJECT_RESOURCE = "e2e/projects/basic-project";
    private static final String APP_JAVA = "src/main/java/com/example/App.java";
    private static final String STABLE_SERVICE_JAVA = "src/main/java/com/example/service/StableService.java";
    private static final String FEATURE_SERVICE_JAVA = "src/main/java/com/example/service/nested/FeatureService.java";
    private static final String INTERNAL_TOOL_JAVA = "src/main/java/com/example/internal/InternalTool.java";
    private static final String EXCLUDED_SAMPLE_JAVA = "src/main/java/com/example/excluded/ExcludedSample.java";
    private static final String APP_TEST_JAVA = "src/test/java/com/example/AppTest.java";
    private static final String EXPECTED_HARMONIZED_CLASS = """
            package %s;

            public class %s {

                int a = 1;

                int b = 2;

                void aMethod() {}

                void zMethod() {}
            }
            """;

    @TempDir
    Path temporaryDirectory;

    private Path executableJar;

    @BeforeEach
    void setUp() {
        executableJar = ExecutableJarLocator.locateExecutableJar();
    }

    @Test
    void helpCommand_rootHelpRequested_shouldPrintUsageInformation() throws IOException, InterruptedException {
        // Given
        Path workingDirectory = temporaryDirectory;

        // When
        ExternalCliProcessResult result = ExternalCliProcessRunner.run(executableJar, workingDirectory, "--help");

        // Then
        assertCompleted(result);
        assertThat(result.exitCode()).as(result.diagnostics()).isZero();
        assertThat(result.stdout())
                .as(result.diagnostics())
                .contains("Usage: jharmonizer")
                .contains("restructure")
                .contains("check")
                .contains("check-fast");
        assertThat(result.stderr()).as(result.diagnostics()).isBlank();
    }

    @ParameterizedTest
    @ValueSource(strings = {"restructure", "check", "check-fast"})
    void helpCommand_subcommandHelpRequested_shouldPrintUsageInformation(String command)
            throws IOException, InterruptedException {
        // Given
        Path workingDirectory = temporaryDirectory;

        // When
        ExternalCliProcessResult result =
                ExternalCliProcessRunner.run(executableJar, workingDirectory, command, "--help");

        // Then
        assertCompleted(result);
        assertThat(result.exitCode()).as(result.diagnostics()).isZero();
        assertThat(result.stdout())
                .as(result.diagnostics())
                .contains("Usage: jharmonizer " + command)
                .contains("--base-dir")
                .contains("--include")
                .contains("--exclude");
        assertThat(result.stderr()).as(result.diagnostics()).isBlank();
    }

    @Test
    void restructureCommand_singleIncludeProvided_shouldModifyAllMatchingFiles()
            throws IOException, InterruptedException, URISyntaxException {
        // Given
        Path projectDirectory = copyBasicProject();
        DirectorySnapshot beforeSnapshot = DirectorySnapshot.capture(projectDirectory);

        // When
        ExternalCliProcessResult result = ExternalCliProcessRunner.run(
                executableJar, projectDirectory, "restructure", "-b", ".", "-i", "**/*.java");
        DirectorySnapshot afterSnapshot = DirectorySnapshot.capture(projectDirectory);

        // Then
        assertCompleted(result);
        assertThat(result.exitCode()).as(result.diagnostics()).isZero();
        assertThat(result.combinedOutput())
                .as(result.diagnostics())
                .contains("Harmonization result:")
                .doesNotContain("SLF4J(W)");
        FileContentAssertions.assertFileChanged(beforeSnapshot, afterSnapshot, APP_JAVA);
        FileContentAssertions.assertFileChanged(beforeSnapshot, afterSnapshot, FEATURE_SERVICE_JAVA);
        FileContentAssertions.assertFileChanged(beforeSnapshot, afterSnapshot, INTERNAL_TOOL_JAVA);
        FileContentAssertions.assertFileChanged(beforeSnapshot, afterSnapshot, EXCLUDED_SAMPLE_JAVA);
        FileContentAssertions.assertFileChanged(beforeSnapshot, afterSnapshot, APP_TEST_JAVA);
        FileContentAssertions.assertFileUnchanged(beforeSnapshot, afterSnapshot, STABLE_SERVICE_JAVA);
        assertThat(afterSnapshot.content(APP_JAVA))
                .as(result.diagnostics())
                .isEqualTo(expectedHarmonizedContent("com.example", "App"));
        assertThat(afterSnapshot.content(FEATURE_SERVICE_JAVA))
                .as(result.diagnostics())
                .isEqualTo(expectedHarmonizedContent("com.example.service.nested", "FeatureService"));
    }

    @Test
    void restructureCommand_multipleIncludesAndExcludesProvided_shouldModifyOnlySelectedFiles()
            throws IOException, InterruptedException, URISyntaxException {
        // Given
        Path projectDirectory = copyBasicProject();
        DirectorySnapshot beforeSnapshot = DirectorySnapshot.capture(projectDirectory);

        // When
        ExternalCliProcessResult result = ExternalCliProcessRunner.run(
                executableJar,
                projectDirectory,
                "restructure",
                "-b",
                ".",
                "-i",
                "src/main/java/**/*.java",
                "-i",
                "src/test/java/**/*.java",
                "-e",
                "**/internal/**",
                "-e",
                "**/excluded/**",
                "-e",
                "**/*Test.java");
        DirectorySnapshot afterSnapshot = DirectorySnapshot.capture(projectDirectory);

        // Then
        assertCompleted(result);
        assertThat(result.exitCode()).as(result.diagnostics()).isZero();
        FileContentAssertions.assertFileChanged(beforeSnapshot, afterSnapshot, APP_JAVA);
        FileContentAssertions.assertFileChanged(beforeSnapshot, afterSnapshot, FEATURE_SERVICE_JAVA);
        FileContentAssertions.assertFileUnchanged(beforeSnapshot, afterSnapshot, STABLE_SERVICE_JAVA);
        FileContentAssertions.assertFileUnchanged(beforeSnapshot, afterSnapshot, INTERNAL_TOOL_JAVA);
        FileContentAssertions.assertFileUnchanged(beforeSnapshot, afterSnapshot, EXCLUDED_SAMPLE_JAVA);
        FileContentAssertions.assertFileUnchanged(beforeSnapshot, afterSnapshot, APP_TEST_JAVA);
    }

    @Test
    void restructureCommand_alreadyHarmonizedInputProvided_shouldLeaveFilesUnchanged()
            throws IOException, InterruptedException, URISyntaxException {
        // Given
        Path projectDirectory = copyBasicProject();
        ExternalCliProcessResult initialResult = ExternalCliProcessRunner.run(
                executableJar, projectDirectory, "restructure", "-b", ".", "-i", "**/*.java");
        assertCompleted(initialResult);
        assertThat(initialResult.exitCode()).as(initialResult.diagnostics()).isZero();
        DirectorySnapshot beforeSnapshot = DirectorySnapshot.capture(projectDirectory);

        // When
        ExternalCliProcessResult secondResult = ExternalCliProcessRunner.run(
                executableJar, projectDirectory, "restructure", "-b", ".", "-i", "**/*.java");
        DirectorySnapshot afterSnapshot = DirectorySnapshot.capture(projectDirectory);

        // Then
        assertCompleted(secondResult);
        assertThat(secondResult.exitCode()).as(secondResult.diagnostics()).isZero();
        assertThat(afterSnapshot.fileContents())
                .as(secondResult.diagnostics())
                .isEqualTo(beforeSnapshot.fileContents());
    }

    @Test
    void checkCommand_nonHarmonizedFilesPresent_shouldReturnSuccessWithoutModifyingFiles()
            throws IOException, InterruptedException, URISyntaxException {
        // Given
        Path projectDirectory = copyBasicProject();
        DirectorySnapshot beforeSnapshot = DirectorySnapshot.capture(projectDirectory);

        // When
        ExternalCliProcessResult result =
                ExternalCliProcessRunner.run(executableJar, projectDirectory, "check", "-b", ".", "-i", "**/*.java");
        DirectorySnapshot afterSnapshot = DirectorySnapshot.capture(projectDirectory);

        // Then
        assertCompleted(result);
        assertThat(result.exitCode()).as(result.diagnostics()).isZero();
        assertThat(result.combinedOutput())
                .as(result.diagnostics())
                .contains("Harmonization result:")
                .contains("App.java")
                .containsAnyOf("REORDERED", "FORMATTED");
        assertThat(afterSnapshot.fileContents()).as(result.diagnostics()).isEqualTo(beforeSnapshot.fileContents());
    }

    @Test
    void checkCommand_includeAndExcludeProvided_shouldRestrictCheckedScope()
            throws IOException, InterruptedException, URISyntaxException {
        // Given
        Path projectDirectory = copyBasicProject();
        DirectorySnapshot beforeSnapshot = DirectorySnapshot.capture(projectDirectory);

        // When
        ExternalCliProcessResult result = ExternalCliProcessRunner.run(
                executableJar,
                projectDirectory,
                "check",
                "-b",
                ".",
                "-i",
                "src/main/java/**/*.java",
                "-i",
                "src/test/java/**/*.java",
                "-e",
                "**/internal/**",
                "-e",
                "**/excluded/**",
                "-e",
                "**/*Test.java");
        DirectorySnapshot afterSnapshot = DirectorySnapshot.capture(projectDirectory);

        // Then
        assertCompleted(result);
        assertThat(result.exitCode()).as(result.diagnostics()).isZero();
        assertThat(result.combinedOutput())
                .as(result.diagnostics())
                .contains("App.java")
                .contains("FeatureService.java")
                .contains("StableService.java")
                .doesNotContain("InternalTool.java")
                .doesNotContain("ExcludedSample.java")
                .doesNotContain("AppTest.java");
        assertThat(afterSnapshot.fileContents()).as(result.diagnostics()).isEqualTo(beforeSnapshot.fileContents());
    }

    @Test
    void checkCommand_afterRestructureExecuted_shouldReportCleanState()
            throws IOException, InterruptedException, URISyntaxException {
        // Given
        Path projectDirectory = copyBasicProject();
        ExternalCliProcessResult restructureResult = ExternalCliProcessRunner.run(
                executableJar, projectDirectory, "restructure", "-b", ".", "-i", "**/*.java");
        assertCompleted(restructureResult);
        assertThat(restructureResult.exitCode())
                .as(restructureResult.diagnostics())
                .isZero();
        DirectorySnapshot beforeSnapshot = DirectorySnapshot.capture(projectDirectory);

        // When
        ExternalCliProcessResult result =
                ExternalCliProcessRunner.run(executableJar, projectDirectory, "check", "-b", ".", "-i", "**/*.java");
        DirectorySnapshot afterSnapshot = DirectorySnapshot.capture(projectDirectory);

        // Then
        assertCompleted(result);
        assertThat(result.exitCode()).as(result.diagnostics()).isZero();
        assertThat(result.combinedOutput())
                .as(result.diagnostics())
                .contains("CHECKED")
                .doesNotContain("REORDERED")
                .doesNotContain("FORMATTED");
        assertThat(afterSnapshot.fileContents()).as(result.diagnostics()).isEqualTo(beforeSnapshot.fileContents());
    }

    @Test
    void checkFastCommand_nonHarmonizedFilesPresent_shouldReturnFailureExitCode()
            throws IOException, InterruptedException, URISyntaxException {
        // Given
        Path projectDirectory = copyBasicProject();
        DirectorySnapshot beforeSnapshot = DirectorySnapshot.capture(projectDirectory);

        // When
        ExternalCliProcessResult result = ExternalCliProcessRunner.run(
                executableJar, projectDirectory, "check-fast", "-b", ".", "-i", "**/*.java");
        DirectorySnapshot afterSnapshot = DirectorySnapshot.capture(projectDirectory);

        // Then
        assertCompleted(result);
        assertThat(result.exitCode()).as(result.diagnostics()).isEqualTo(3);
        assertThat(result.combinedOutput())
                .as(result.diagnostics())
                .contains("Flow CHECK_FAIL_FAST stopped early")
                .containsAnyOf(
                        "App.java", "FeatureService.java", "InternalTool.java", "ExcludedSample.java", "AppTest.java");
        assertThat(afterSnapshot.fileContents()).as(result.diagnostics()).isEqualTo(beforeSnapshot.fileContents());
    }

    @Test
    void restructureCommand_missingBaseDirProvided_shouldReturnInvalidUsageExitCode()
            throws IOException, InterruptedException {
        // Given
        Path workingDirectory = temporaryDirectory;

        // When
        ExternalCliProcessResult result =
                ExternalCliProcessRunner.run(executableJar, workingDirectory, "restructure", "-i", "**/*.java");

        // Then
        assertCompleted(result);
        assertThat(result.exitCode()).as(result.diagnostics()).isEqualTo(2);
        assertThat(result.combinedOutput())
                .as(result.diagnostics())
                .contains("Missing required option: '--base-dir=<baseDir>'")
                .contains("Usage: jharmonizer restructure");
    }

    @Test
    void checkCommand_nonexistentBaseDirProvided_shouldReturnProcessingError()
            throws IOException, InterruptedException {
        // Given
        Path workingDirectory = temporaryDirectory;

        // When
        ExternalCliProcessResult result = ExternalCliProcessRunner.run(
                executableJar, workingDirectory, "check", "-b", "missing-directory", "-i", "**/*.java");

        // Then
        assertCompleted(result);
        assertThat(result.exitCode()).as(result.diagnostics()).isEqualTo(1);
        assertThat(result.combinedOutput())
                .as(result.diagnostics())
                .contains("Base directory does not exist or is not a directory");
    }

    private Path copyBasicProject() throws IOException, URISyntaxException {
        return TemporaryProjectCopier.copyProject(BASIC_PROJECT_RESOURCE, temporaryDirectory.resolve("project"));
    }

    private static void assertCompleted(ExternalCliProcessResult result) {
        assertThat(result.timedOut()).as(result.diagnostics()).isFalse();
    }

    private static String expectedHarmonizedContent(String packageName, String className) {
        return EXPECTED_HARMONIZED_CLASS.formatted(packageName, className);
    }
}
