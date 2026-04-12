package io.github.lemon_ant.jharmonizer.cli.e2e;

import static io.github.lemon_ant.jharmonizer.cli.e2e.FileContentAssertions.assertFileChanged;
import static io.github.lemon_ant.jharmonizer.cli.e2e.FileContentAssertions.assertFileMatches;
import static io.github.lemon_ant.jharmonizer.cli.e2e.FileContentAssertions.assertFileUnchanged;
import static io.github.lemon_ant.jharmonizer.cli.e2e.TemporaryProjectCopier.locateOriginalTestResource;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import lombok.NonNull;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class JHarmonizerCliPackagedJarIT {

    private static final Path EXECUTABLE_JAR = ExecutableJarLocator.locateExecutableJar();
    private static final Path ORIGINAL_PROJECT_DIRECTORY = locateOriginalTestResource(Constants.BASIC_PROJECT_RESOURCE);
    private static final Path EXPECTED_APP_FILE = locateOriginalTestResource(Constants.EXPECTED_APP_RESOURCE);
    private static final Path EXPECTED_FEATURE_SERVICE_FILE =
            locateOriginalTestResource(Constants.EXPECTED_FEATURE_SERVICE_RESOURCE);

    @TempDir
    Path temporaryDirectory;

    @Test
    void packagedJar_manifestInspected_preserveExecutableMetadata() throws IOException {
        // Given
        String expectedMainClass = "io.github.lemon_ant.jharmonizer.cli.command.JHarmonizerCliApplication";
        String expectedModuleExports = String.join(
                " ",
                "jdk.compiler/com.sun.tools.javac.api",
                "jdk.compiler/com.sun.tools.javac.file",
                "jdk.compiler/com.sun.tools.javac.parser",
                "jdk.compiler/com.sun.tools.javac.tree",
                "jdk.compiler/com.sun.tools.javac.util");

        try (JarFile jarFile = new JarFile(EXECUTABLE_JAR.toFile())) {
            // When
            Attributes manifestAttributes = jarFile.getManifest().getMainAttributes();
            List<String> moduleInfoEntries = jarFile.stream()
                    .filter(thisEntry -> thisEntry.getName().endsWith("module-info.class"))
                    .map(JarEntry::getName)
                    .toList();

            // Then
            assertThat(manifestAttributes.getValue(Attributes.Name.MAIN_CLASS)).isEqualTo(expectedMainClass);
            assertThat(manifestAttributes.getValue("Add-Exports")).isEqualTo(expectedModuleExports);
            assertThat(manifestAttributes.getValue("Add-Opens")).isEqualTo(expectedModuleExports);
            assertThat(moduleInfoEntries).isEmpty();
        }
    }

    @Test
    void helpCommand_rootHelpRequested_printUsageInformation() throws IOException, InterruptedException {
        // Given
        Path workingDirectory = temporaryDirectory;

        // When
        ExternalCliProcessResult result = ExternalCliProcessRunner.run(EXECUTABLE_JAR, workingDirectory, "--help");

        // Then
        assertCompleted(result);
        assertThat(result.getExitCode()).as(result.toString()).isZero();
        assertThat(result.getStdout())
                .as(result.toString())
                .contains("Usage: jharmonizer")
                .contains("reorder")
                .contains("check-all")
                .contains("check-fast");
        assertThat(result.getStderr()).as(result.toString()).isBlank();
    }

    @ParameterizedTest
    @ValueSource(strings = {"reorder", "check-all", "check-fast"})
    void helpCommand_subcommandHelpRequested_printUsageInformation(String command)
            throws IOException, InterruptedException {
        // Given
        Path workingDirectory = temporaryDirectory;

        // When
        ExternalCliProcessResult result =
                ExternalCliProcessRunner.run(EXECUTABLE_JAR, workingDirectory, command, "--help");

        // Then
        assertCompleted(result);
        assertThat(result.getExitCode()).as(result.toString()).isZero();
        assertThat(result.getStdout())
                .as(result.toString())
                .contains("Usage: jharmonizer " + command)
                .contains("-b, --base-dir")
                .contains("-B, --no-backup")
                .contains("-S, --no-statistics")
                .contains("-i, --include")
                .contains("-e, --exclude")
                .contains("Repeat this option or pass multiple patterns as a")
                .contains("comma-separated list.");
        assertThat(result.getStderr()).as(result.toString()).isBlank();
    }

    @Test
    void reorderCommand_baseDirOmitted_useCurrentWorkingDirectory() throws IOException, InterruptedException {
        // Given
        Path projectDirectory = copyBasicProject("project-default-base-dir");

        // When
        ExternalCliProcessResult result = ExternalCliProcessRunner.run(
                EXECUTABLE_JAR, projectDirectory, "reorder", "--no-statistics", "--include", "**/*.java");

        // Then
        assertCompleted(result);
        assertThat(result.getExitCode()).as(result.toString()).isZero();
        assertThat(result.combinedOutput())
                .as(result.toString())
                .doesNotContain("JHarmonizer harmonization summary")
                .doesNotContain("SLF4J(W)");
        assertFileChanged(ORIGINAL_PROJECT_DIRECTORY, projectDirectory, Constants.APP_JAVA);
        assertFileChanged(ORIGINAL_PROJECT_DIRECTORY, projectDirectory, Constants.FEATURE_SERVICE_JAVA);
        assertFileChanged(ORIGINAL_PROJECT_DIRECTORY, projectDirectory, Constants.INTERNAL_TOOL_JAVA);
        assertFileChanged(ORIGINAL_PROJECT_DIRECTORY, projectDirectory, Constants.EXCLUDED_SAMPLE_JAVA);
        assertFileChanged(ORIGINAL_PROJECT_DIRECTORY, projectDirectory, Constants.APP_TEST_JAVA);
        assertFileUnchanged(ORIGINAL_PROJECT_DIRECTORY, projectDirectory, Constants.STABLE_SERVICE_JAVA);
        assertFileMatches(EXPECTED_APP_FILE, projectDirectory, Constants.APP_JAVA);
        assertFileMatches(EXPECTED_FEATURE_SERVICE_FILE, projectDirectory, Constants.FEATURE_SERVICE_JAVA);
    }

    @Test
    void reorderCommand_multipleIncludesAndExcludesProvided_modifyOnlySelectedFiles()
            throws IOException, InterruptedException {
        // Given
        Path projectDirectory = copyBasicProject("project-filtered-reorder");

        // When
        ExternalCliProcessResult result = ExternalCliProcessRunner.run(
                EXECUTABLE_JAR,
                projectDirectory,
                "reorder",
                "--no-statistics",
                "--base-dir",
                ".",
                "--include",
                "src/main/java/**/*.java",
                "--include",
                "src/test/java/**/*.java",
                "--exclude",
                "**/internal/**",
                "--exclude",
                "**/excluded/**",
                "--exclude",
                "**/*Test.java");

        // Then
        assertCompleted(result);
        assertThat(result.getExitCode()).as(result.toString()).isZero();
        assertFileChanged(ORIGINAL_PROJECT_DIRECTORY, projectDirectory, Constants.APP_JAVA);
        assertFileChanged(ORIGINAL_PROJECT_DIRECTORY, projectDirectory, Constants.FEATURE_SERVICE_JAVA);
        assertFileUnchanged(ORIGINAL_PROJECT_DIRECTORY, projectDirectory, Constants.STABLE_SERVICE_JAVA);
        assertFileUnchanged(ORIGINAL_PROJECT_DIRECTORY, projectDirectory, Constants.INTERNAL_TOOL_JAVA);
        assertFileUnchanged(ORIGINAL_PROJECT_DIRECTORY, projectDirectory, Constants.EXCLUDED_SAMPLE_JAVA);
        assertFileUnchanged(ORIGINAL_PROJECT_DIRECTORY, projectDirectory, Constants.APP_TEST_JAVA);
    }

    @Test
    void reorderCommand_alreadyHarmonizedInputProvided_leaveFilesUnchanged() throws IOException, InterruptedException {
        // Given
        Path projectDirectory = copyBasicProject("project-already-harmonized");
        ExternalCliProcessResult initialResult = ExternalCliProcessRunner.run(
                EXECUTABLE_JAR,
                projectDirectory,
                "reorder",
                "--no-statistics",
                "--base-dir",
                ".",
                "--include",
                "**/*.java");
        assertCompleted(initialResult);
        assertThat(initialResult.getExitCode()).as(initialResult.toString()).isZero();
        Path expectedProjectDirectory = copyDirectory(projectDirectory, "project-already-harmonized-expected");

        // When
        ExternalCliProcessResult secondResult = ExternalCliProcessRunner.run(
                EXECUTABLE_JAR,
                projectDirectory,
                "reorder",
                "--no-statistics",
                "--base-dir",
                ".",
                "--include",
                "**/*.java");

        // Then
        assertCompleted(secondResult);
        assertThat(secondResult.getExitCode()).as(secondResult.toString()).isZero();
        assertFileUnchanged(expectedProjectDirectory, projectDirectory, Constants.APP_JAVA);
        assertFileUnchanged(expectedProjectDirectory, projectDirectory, Constants.STABLE_SERVICE_JAVA);
        assertFileUnchanged(expectedProjectDirectory, projectDirectory, Constants.FEATURE_SERVICE_JAVA);
        assertFileUnchanged(expectedProjectDirectory, projectDirectory, Constants.INTERNAL_TOOL_JAVA);
        assertFileUnchanged(expectedProjectDirectory, projectDirectory, Constants.EXCLUDED_SAMPLE_JAVA);
        assertFileUnchanged(expectedProjectDirectory, projectDirectory, Constants.APP_TEST_JAVA);
    }

    @Test
    void checkCommand_nonHarmonizedFilesPresent_returnFailureWithoutModifyingFiles()
            throws IOException, InterruptedException {
        // Given
        Path projectDirectory = copyBasicProject("project-check-dirty");

        // When
        ExternalCliProcessResult result = ExternalCliProcessRunner.run(
                EXECUTABLE_JAR,
                projectDirectory,
                "check-all",
                "--verbose",
                "--base-dir",
                ".",
                "--include",
                "**/*.java");

        // Then
        assertCompleted(result);
        assertThat(result.getExitCode()).as(result.toString()).isEqualTo(1);
        assertThat(result.combinedOutput())
                .as(result.toString())
                .contains("JHarmonizer harmonization summary")
                .contains("App.java")
                .containsAnyOf("REORDERED", "FORMATTED")
                .contains("do not conform")
                .contains("Exit code: 1");
        assertFileUnchanged(ORIGINAL_PROJECT_DIRECTORY, projectDirectory, Constants.APP_JAVA);
        assertFileUnchanged(ORIGINAL_PROJECT_DIRECTORY, projectDirectory, Constants.STABLE_SERVICE_JAVA);
        assertFileUnchanged(ORIGINAL_PROJECT_DIRECTORY, projectDirectory, Constants.FEATURE_SERVICE_JAVA);
        assertFileUnchanged(ORIGINAL_PROJECT_DIRECTORY, projectDirectory, Constants.INTERNAL_TOOL_JAVA);
        assertFileUnchanged(ORIGINAL_PROJECT_DIRECTORY, projectDirectory, Constants.EXCLUDED_SAMPLE_JAVA);
        assertFileUnchanged(ORIGINAL_PROJECT_DIRECTORY, projectDirectory, Constants.APP_TEST_JAVA);
    }

    @Test
    void checkCommand_collectionOptionFormatsProvided_restrictCheckedScope() throws IOException, InterruptedException {
        // Given
        Path projectDirectory = copyBasicProject("project-check-filtered");

        // When
        ExternalCliProcessResult result = ExternalCliProcessRunner.run(
                EXECUTABLE_JAR,
                projectDirectory,
                "check-all",
                "--verbose",
                "--no-statistics",
                "--base-dir",
                ".",
                "--include",
                "src/main/java/**/*.java,src/test/java/**/*.java",
                "--exclude",
                "**/internal/**",
                "--exclude",
                "**/excluded/**,**/*Test.java");

        // Then
        assertCompleted(result);
        assertThat(result.getExitCode()).as(result.toString()).isEqualTo(1);
        assertThat(result.combinedOutput())
                .as(result.toString())
                .contains("App.java")
                .contains("FeatureService.java")
                .contains("StableService.java")
                .doesNotContain("InternalTool.java")
                .doesNotContain("ExcludedSample.java")
                .doesNotContain("AppTest.java");
        assertFileUnchanged(ORIGINAL_PROJECT_DIRECTORY, projectDirectory, Constants.APP_JAVA);
        assertFileUnchanged(ORIGINAL_PROJECT_DIRECTORY, projectDirectory, Constants.STABLE_SERVICE_JAVA);
        assertFileUnchanged(ORIGINAL_PROJECT_DIRECTORY, projectDirectory, Constants.FEATURE_SERVICE_JAVA);
        assertFileUnchanged(ORIGINAL_PROJECT_DIRECTORY, projectDirectory, Constants.INTERNAL_TOOL_JAVA);
        assertFileUnchanged(ORIGINAL_PROJECT_DIRECTORY, projectDirectory, Constants.EXCLUDED_SAMPLE_JAVA);
        assertFileUnchanged(ORIGINAL_PROJECT_DIRECTORY, projectDirectory, Constants.APP_TEST_JAVA);
    }

    @Test
    void checkCommand_afterReorderExecuted_reportCleanState() throws IOException, InterruptedException {
        // Given
        Path projectDirectory = copyBasicProject("project-check-clean");
        ExternalCliProcessResult reorderResult = ExternalCliProcessRunner.run(
                EXECUTABLE_JAR,
                projectDirectory,
                "reorder",
                "--no-statistics",
                "--base-dir",
                ".",
                "--include",
                "**/*.java");
        assertCompleted(reorderResult);
        assertThat(reorderResult.getExitCode()).as(reorderResult.toString()).isZero();
        Path expectedProjectDirectory = copyDirectory(projectDirectory, "project-check-clean-expected");

        // When
        ExternalCliProcessResult result = ExternalCliProcessRunner.run(
                EXECUTABLE_JAR,
                projectDirectory,
                "check-all",
                "--verbose",
                "--no-statistics",
                "--base-dir",
                ".",
                "--include",
                "**/*.java");

        // Then
        assertCompleted(result);
        assertThat(result.getExitCode()).as(result.toString()).isZero();
        assertThat(result.combinedOutput())
                .as(result.toString())
                .contains("CHECKED")
                .contains("completed successfully")
                .contains("Exit code: 0")
                .doesNotContain("REORDERED")
                .doesNotContain("FORMATTED");
        assertFileUnchanged(expectedProjectDirectory, projectDirectory, Constants.APP_JAVA);
        assertFileUnchanged(expectedProjectDirectory, projectDirectory, Constants.STABLE_SERVICE_JAVA);
        assertFileUnchanged(expectedProjectDirectory, projectDirectory, Constants.FEATURE_SERVICE_JAVA);
        assertFileUnchanged(expectedProjectDirectory, projectDirectory, Constants.INTERNAL_TOOL_JAVA);
        assertFileUnchanged(expectedProjectDirectory, projectDirectory, Constants.EXCLUDED_SAMPLE_JAVA);
        assertFileUnchanged(expectedProjectDirectory, projectDirectory, Constants.APP_TEST_JAVA);
    }

    @Test
    void checkFastCommand_nonHarmonizedFilesPresent_returnFailureExitCode() throws IOException, InterruptedException {
        // Given
        Path projectDirectory = copyBasicProject("project-check-fast-dirty");

        // When
        ExternalCliProcessResult result = ExternalCliProcessRunner.run(
                EXECUTABLE_JAR,
                projectDirectory,
                "check-fast",
                "--no-statistics",
                "--base-dir",
                ".",
                "--include",
                "**/*.java");

        // Then
        assertCompleted(result);
        assertThat(result.getExitCode()).as(result.toString()).isEqualTo(3);
        assertThat(result.combinedOutput())
                .as(result.toString())
                .contains("stopped early")
                .contains("violation detected")
                .contains("Exit code: 3");
        assertFileUnchanged(ORIGINAL_PROJECT_DIRECTORY, projectDirectory, Constants.APP_JAVA);
        assertFileUnchanged(ORIGINAL_PROJECT_DIRECTORY, projectDirectory, Constants.STABLE_SERVICE_JAVA);
        assertFileUnchanged(ORIGINAL_PROJECT_DIRECTORY, projectDirectory, Constants.FEATURE_SERVICE_JAVA);
        assertFileUnchanged(ORIGINAL_PROJECT_DIRECTORY, projectDirectory, Constants.INTERNAL_TOOL_JAVA);
        assertFileUnchanged(ORIGINAL_PROJECT_DIRECTORY, projectDirectory, Constants.EXCLUDED_SAMPLE_JAVA);
        assertFileUnchanged(ORIGINAL_PROJECT_DIRECTORY, projectDirectory, Constants.APP_TEST_JAVA);
    }

    @Test
    void reorderCommand_invalidOptionProvided_returnInvalidUsageExitCode() throws IOException, InterruptedException {
        // Given
        Path projectDirectory = copyBasicProject("project-invalid-option");

        // When
        ExternalCliProcessResult result =
                ExternalCliProcessRunner.run(EXECUTABLE_JAR, projectDirectory, "reorder", "--unknown-option");

        // Then
        assertCompleted(result);
        assertThat(result.getExitCode()).as(result.toString()).isEqualTo(2);
        assertThat(result.combinedOutput())
                .as(result.toString())
                .contains("Unknown option: '--unknown-option'")
                .contains("Usage: jharmonizer reorder");
    }

    @Test
    void checkCommand_nonexistentBaseDirProvided_returnProcessingError() throws IOException, InterruptedException {
        // Given
        Path workingDirectory = temporaryDirectory;

        // When
        ExternalCliProcessResult result = ExternalCliProcessRunner.run(
                EXECUTABLE_JAR,
                workingDirectory,
                "check-all",
                "--no-statistics",
                "--base-dir",
                "missing-directory",
                "--include",
                "**/*.java");

        // Then
        assertCompleted(result);
        assertThat(result.getExitCode()).as(result.toString()).isEqualTo(1);
        assertThat(result.combinedOutput())
                .as(result.toString())
                .contains("Base directory does not exist or is not a directory");
    }

    @NonNull
    private Path copyBasicProject(String targetDirectoryName) throws IOException {
        return TemporaryProjectCopier.copyProject(
                Constants.BASIC_PROJECT_RESOURCE, temporaryDirectory.resolve(targetDirectoryName));
    }

    @NonNull
    private Path copyDirectory(Path srcDirectory, String targetDirectoryName) throws IOException {
        Path targetDirectory = temporaryDirectory.resolve(targetDirectoryName);
        Files.createDirectories(targetDirectory);
        FileUtils.copyDirectory(srcDirectory.toFile(), targetDirectory.toFile());
        return targetDirectory;
    }

    private static void assertCompleted(ExternalCliProcessResult result) {
        assertThat(result.isTimedOut()).as(result.toString()).isFalse();
    }

    private static final class Constants {
        private static final String TEST_CASES_DIR = "test-cases";
        private static final String BASIC_PROJECT_RESOURCE = TEST_CASES_DIR + "/cli/e2e/projects/basic-project";
        private static final String EXPECTED_APP_RESOURCE = TEST_CASES_DIR + "/cli/e2e/expected/App.java";
        private static final String EXPECTED_FEATURE_SERVICE_RESOURCE =
                TEST_CASES_DIR + "/cli/e2e/expected/FeatureService.java";
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

        private Constants() {}
    }
}
