package io.github.lemon_ant.jharmonizer.cli.e2e;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class JHarmonizerCliPackagedJarIT {

    private static final String BASIC_PROJECT_RESOURCE = "test-cases/cli/e2e/projects/basic-project";
    private static final String APP_JAVA = "src/main/java/io/github/lemon_ant/jharmonizer/cli/e2e/sample/App.java";
    private static final String STABLE_SERVICE_JAVA =
            "src/main/java/io/github/lemon_ant/jharmonizer/cli/e2e/sample/service/StableService.java";
    private static final String FEATURE_SERVICE_JAVA =
            "src/main/java/io/github/lemon_ant/jharmonizer/cli/e2e/sample/service/nested/FeatureService.java";
    private static final String INTERNAL_TOOL_JAVA =
            "src/main/java/io/github/lemon_ant/jharmonizer/cli/e2e/sample/internal/InternalTool.java";
    private static final String EXCLUDED_SAMPLE_JAVA =
            "src/main/java/io/github/lemon_ant/jharmonizer/cli/e2e/sample/excluded/ExcludedSample.java";
    private static final String APP_TEST_JAVA =
            "src/test/java/io/github/lemon_ant/jharmonizer/cli/e2e/sample/AppTest.java";
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
    private Path originalProjectDirectory;

    @BeforeEach
    void setUp() throws URISyntaxException {
        executableJar = ExecutableJarLocator.locateExecutableJar();
        originalProjectDirectory = TemporaryProjectCopier.locateProject(BASIC_PROJECT_RESOURCE);
    }

    @Test
    void helpCommand_rootHelpRequested_shouldPrintUsageInformation() throws IOException, InterruptedException {
        // Given
        Path workingDirectory = temporaryDirectory;

        // When
        ExternalCliProcessResult result = ExternalCliProcessRunner.run(executableJar, workingDirectory, "--help");

        // Then
        assertCompleted(result);
        assertThat(result.getExitCode()).as(result.toString()).isZero();
        assertThat(result.getStdout())
                .as(result.toString())
                .contains("Usage: jharmonizer")
                .contains("restructure")
                .contains("check")
                .contains("check-fast");
        assertThat(result.getStderr()).as(result.toString()).isBlank();
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
        assertThat(result.getExitCode()).as(result.toString()).isZero();
        assertThat(result.getStdout())
                .as(result.toString())
                .contains("Usage: jharmonizer " + command)
                .contains("--base-dir")
                .contains("--include")
                .contains("--exclude");
        assertThat(result.getStderr()).as(result.toString()).isBlank();
    }

    @Test
    void restructureCommand_baseDirOmitted_shouldUseCurrentWorkingDirectory()
            throws IOException, InterruptedException, URISyntaxException {
        // Given
        Path projectDirectory = copyBasicProject("project-default-base-dir");

        // When
        ExternalCliProcessResult result =
                ExternalCliProcessRunner.run(executableJar, projectDirectory, "restructure", "-i", "**/*.java");

        // Then
        assertCompleted(result);
        assertThat(result.getExitCode()).as(result.toString()).isZero();
        assertThat(result.combinedOutput())
                .as(result.toString())
                .contains("Harmonization result:")
                .doesNotContain("SLF4J(W)");
        FileContentAssertions.assertFileChanged(originalProjectDirectory, projectDirectory, APP_JAVA);
        FileContentAssertions.assertFileChanged(originalProjectDirectory, projectDirectory, FEATURE_SERVICE_JAVA);
        FileContentAssertions.assertFileChanged(originalProjectDirectory, projectDirectory, INTERNAL_TOOL_JAVA);
        FileContentAssertions.assertFileChanged(originalProjectDirectory, projectDirectory, EXCLUDED_SAMPLE_JAVA);
        FileContentAssertions.assertFileChanged(originalProjectDirectory, projectDirectory, APP_TEST_JAVA);
        FileContentAssertions.assertFileUnchanged(originalProjectDirectory, projectDirectory, STABLE_SERVICE_JAVA);
        assertThat(FileContentAssertions.readFile(projectDirectory.resolve(APP_JAVA)))
                .as(result.toString())
                .isEqualTo(expectedHarmonizedContent("io.github.lemon_ant.jharmonizer.cli.e2e.sample", "App"));
        assertThat(FileContentAssertions.readFile(projectDirectory.resolve(FEATURE_SERVICE_JAVA)))
                .as(result.toString())
                .isEqualTo(expectedHarmonizedContent(
                        "io.github.lemon_ant.jharmonizer.cli.e2e.sample.service.nested", "FeatureService"));
    }

    @Test
    void restructureCommand_multipleIncludesAndExcludesProvided_shouldModifyOnlySelectedFiles()
            throws IOException, InterruptedException, URISyntaxException {
        // Given
        Path projectDirectory = copyBasicProject("project-filtered-restructure");

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

        // Then
        assertCompleted(result);
        assertThat(result.getExitCode()).as(result.toString()).isZero();
        FileContentAssertions.assertFileChanged(originalProjectDirectory, projectDirectory, APP_JAVA);
        FileContentAssertions.assertFileChanged(originalProjectDirectory, projectDirectory, FEATURE_SERVICE_JAVA);
        FileContentAssertions.assertFileUnchanged(originalProjectDirectory, projectDirectory, STABLE_SERVICE_JAVA);
        FileContentAssertions.assertFileUnchanged(originalProjectDirectory, projectDirectory, INTERNAL_TOOL_JAVA);
        FileContentAssertions.assertFileUnchanged(originalProjectDirectory, projectDirectory, EXCLUDED_SAMPLE_JAVA);
        FileContentAssertions.assertFileUnchanged(originalProjectDirectory, projectDirectory, APP_TEST_JAVA);
    }

    @Test
    void restructureCommand_alreadyHarmonizedInputProvided_shouldLeaveFilesUnchanged()
            throws IOException, InterruptedException, URISyntaxException {
        // Given
        Path projectDirectory = copyBasicProject("project-already-harmonized");
        ExternalCliProcessResult initialResult = ExternalCliProcessRunner.run(
                executableJar, projectDirectory, "restructure", "-b", ".", "-i", "**/*.java");
        assertCompleted(initialResult);
        assertThat(initialResult.getExitCode()).as(initialResult.toString()).isZero();
        Path expectedProjectDirectory = copyDirectory(projectDirectory, "project-already-harmonized-expected");

        // When
        ExternalCliProcessResult secondResult = ExternalCliProcessRunner.run(
                executableJar, projectDirectory, "restructure", "-b", ".", "-i", "**/*.java");

        // Then
        assertCompleted(secondResult);
        assertThat(secondResult.getExitCode()).as(secondResult.toString()).isZero();
        FileContentAssertions.assertFileUnchanged(expectedProjectDirectory, projectDirectory, APP_JAVA);
        FileContentAssertions.assertFileUnchanged(expectedProjectDirectory, projectDirectory, STABLE_SERVICE_JAVA);
        FileContentAssertions.assertFileUnchanged(expectedProjectDirectory, projectDirectory, FEATURE_SERVICE_JAVA);
        FileContentAssertions.assertFileUnchanged(expectedProjectDirectory, projectDirectory, INTERNAL_TOOL_JAVA);
        FileContentAssertions.assertFileUnchanged(expectedProjectDirectory, projectDirectory, EXCLUDED_SAMPLE_JAVA);
        FileContentAssertions.assertFileUnchanged(expectedProjectDirectory, projectDirectory, APP_TEST_JAVA);
    }

    @Test
    void checkCommand_nonHarmonizedFilesPresent_shouldReturnSuccessWithoutModifyingFiles()
            throws IOException, InterruptedException, URISyntaxException {
        // Given
        Path projectDirectory = copyBasicProject("project-check-dirty");

        // When
        ExternalCliProcessResult result =
                ExternalCliProcessRunner.run(executableJar, projectDirectory, "check", "-b", ".", "-i", "**/*.java");

        // Then
        assertCompleted(result);
        assertThat(result.getExitCode()).as(result.toString()).isZero();
        assertThat(result.combinedOutput())
                .as(result.toString())
                .contains("Harmonization result:")
                .contains("App.java")
                .containsAnyOf("REORDERED", "FORMATTED");
        FileContentAssertions.assertFileUnchanged(originalProjectDirectory, projectDirectory, APP_JAVA);
        FileContentAssertions.assertFileUnchanged(originalProjectDirectory, projectDirectory, STABLE_SERVICE_JAVA);
        FileContentAssertions.assertFileUnchanged(originalProjectDirectory, projectDirectory, FEATURE_SERVICE_JAVA);
        FileContentAssertions.assertFileUnchanged(originalProjectDirectory, projectDirectory, INTERNAL_TOOL_JAVA);
        FileContentAssertions.assertFileUnchanged(originalProjectDirectory, projectDirectory, EXCLUDED_SAMPLE_JAVA);
        FileContentAssertions.assertFileUnchanged(originalProjectDirectory, projectDirectory, APP_TEST_JAVA);
    }

    @Test
    void checkCommand_includeAndExcludeProvided_shouldRestrictCheckedScope()
            throws IOException, InterruptedException, URISyntaxException {
        // Given
        Path projectDirectory = copyBasicProject("project-check-filtered");

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

        // Then
        assertCompleted(result);
        assertThat(result.getExitCode()).as(result.toString()).isZero();
        assertThat(result.combinedOutput())
                .as(result.toString())
                .contains("App.java")
                .contains("FeatureService.java")
                .contains("StableService.java")
                .doesNotContain("InternalTool.java")
                .doesNotContain("ExcludedSample.java")
                .doesNotContain("AppTest.java");
        FileContentAssertions.assertFileUnchanged(originalProjectDirectory, projectDirectory, APP_JAVA);
        FileContentAssertions.assertFileUnchanged(originalProjectDirectory, projectDirectory, STABLE_SERVICE_JAVA);
        FileContentAssertions.assertFileUnchanged(originalProjectDirectory, projectDirectory, FEATURE_SERVICE_JAVA);
        FileContentAssertions.assertFileUnchanged(originalProjectDirectory, projectDirectory, INTERNAL_TOOL_JAVA);
        FileContentAssertions.assertFileUnchanged(originalProjectDirectory, projectDirectory, EXCLUDED_SAMPLE_JAVA);
        FileContentAssertions.assertFileUnchanged(originalProjectDirectory, projectDirectory, APP_TEST_JAVA);
    }

    @Test
    void checkCommand_afterRestructureExecuted_shouldReportCleanState()
            throws IOException, InterruptedException, URISyntaxException {
        // Given
        Path projectDirectory = copyBasicProject("project-check-clean");
        ExternalCliProcessResult restructureResult = ExternalCliProcessRunner.run(
                executableJar, projectDirectory, "restructure", "-b", ".", "-i", "**/*.java");
        assertCompleted(restructureResult);
        assertThat(restructureResult.getExitCode())
                .as(restructureResult.toString())
                .isZero();
        Path expectedProjectDirectory = copyDirectory(projectDirectory, "project-check-clean-expected");

        // When
        ExternalCliProcessResult result =
                ExternalCliProcessRunner.run(executableJar, projectDirectory, "check", "-b", ".", "-i", "**/*.java");

        // Then
        assertCompleted(result);
        assertThat(result.getExitCode()).as(result.toString()).isZero();
        assertThat(result.combinedOutput())
                .as(result.toString())
                .contains("CHECKED")
                .doesNotContain("REORDERED")
                .doesNotContain("FORMATTED");
        FileContentAssertions.assertFileUnchanged(expectedProjectDirectory, projectDirectory, APP_JAVA);
        FileContentAssertions.assertFileUnchanged(expectedProjectDirectory, projectDirectory, STABLE_SERVICE_JAVA);
        FileContentAssertions.assertFileUnchanged(expectedProjectDirectory, projectDirectory, FEATURE_SERVICE_JAVA);
        FileContentAssertions.assertFileUnchanged(expectedProjectDirectory, projectDirectory, INTERNAL_TOOL_JAVA);
        FileContentAssertions.assertFileUnchanged(expectedProjectDirectory, projectDirectory, EXCLUDED_SAMPLE_JAVA);
        FileContentAssertions.assertFileUnchanged(expectedProjectDirectory, projectDirectory, APP_TEST_JAVA);
    }

    @Test
    void checkFastCommand_nonHarmonizedFilesPresent_shouldReturnFailureExitCode()
            throws IOException, InterruptedException, URISyntaxException {
        // Given
        Path projectDirectory = copyBasicProject("project-check-fast-dirty");

        // When
        ExternalCliProcessResult result = ExternalCliProcessRunner.run(
                executableJar, projectDirectory, "check-fast", "-b", ".", "-i", "**/*.java");

        // Then
        assertCompleted(result);
        assertThat(result.getExitCode()).as(result.toString()).isEqualTo(3);
        assertThat(result.combinedOutput())
                .as(result.toString())
                .contains("Flow CHECK_FAIL_FAST stopped early")
                .containsAnyOf(
                        "App.java",
                        "StableService.java",
                        "FeatureService.java",
                        "InternalTool.java",
                        "ExcludedSample.java",
                        "AppTest.java");
        FileContentAssertions.assertFileUnchanged(originalProjectDirectory, projectDirectory, APP_JAVA);
        FileContentAssertions.assertFileUnchanged(originalProjectDirectory, projectDirectory, STABLE_SERVICE_JAVA);
        FileContentAssertions.assertFileUnchanged(originalProjectDirectory, projectDirectory, FEATURE_SERVICE_JAVA);
        FileContentAssertions.assertFileUnchanged(originalProjectDirectory, projectDirectory, INTERNAL_TOOL_JAVA);
        FileContentAssertions.assertFileUnchanged(originalProjectDirectory, projectDirectory, EXCLUDED_SAMPLE_JAVA);
        FileContentAssertions.assertFileUnchanged(originalProjectDirectory, projectDirectory, APP_TEST_JAVA);
    }

    @Test
    void restructureCommand_invalidOptionProvided_shouldReturnInvalidUsageExitCode()
            throws IOException, InterruptedException, URISyntaxException {
        // Given
        Path projectDirectory = copyBasicProject("project-invalid-option");

        // When
        ExternalCliProcessResult result =
                ExternalCliProcessRunner.run(executableJar, projectDirectory, "restructure", "--unknown-option");

        // Then
        assertCompleted(result);
        assertThat(result.getExitCode()).as(result.toString()).isEqualTo(2);
        assertThat(result.combinedOutput())
                .as(result.toString())
                .contains("Unknown option: '--unknown-option'")
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
        assertThat(result.getExitCode()).as(result.toString()).isEqualTo(1);
        assertThat(result.combinedOutput())
                .as(result.toString())
                .contains("Base directory does not exist or is not a directory");
    }

    private Path copyBasicProject(String targetDirectoryName) throws IOException, URISyntaxException {
        return TemporaryProjectCopier.copyProject(
                BASIC_PROJECT_RESOURCE, temporaryDirectory.resolve(targetDirectoryName));
    }

    private Path copyDirectory(Path sourceDirectory, String targetDirectoryName) throws IOException {
        Path targetDirectory = temporaryDirectory.resolve(targetDirectoryName);
        Files.createDirectories(targetDirectory);
        FileUtils.copyDirectory(sourceDirectory.toFile(), targetDirectory.toFile());
        return targetDirectory;
    }

    private static void assertCompleted(ExternalCliProcessResult result) {
        assertThat(result.isTimedOut()).as(result.toString()).isFalse();
    }

    private static String expectedHarmonizedContent(String packageName, String className) {
        return EXPECTED_HARMONIZED_CLASS.formatted(packageName, className);
    }
}
