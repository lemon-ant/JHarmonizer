package io.github.lemon_ant.jharmonizer.core.e2e;

import static io.github.lemon_ant.jharmonizer.core.e2e.JavaCompileTestUtils.compileJavaSourceWithRelease21;
import static io.github.lemon_ant.jharmonizer.core.e2e.JavaRunMainTestUtils.runJavaMainMethod;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.lemon_ant.jharmonizer.core.SourceProcessor;
import io.github.lemon_ant.jharmonizer.core.config.input.jharmonizer.JHarmonizerConfigurationManager;
import io.github.lemon_ant.jharmonizer.core.config.unified.FlexibleUnifiedConfig;
import io.github.lemon_ant.jharmonizer.core.config.unified.UnifiedConfig;
import io.github.lemon_ant.jharmonizer.core.files_handler.SourceFilesHandler;
import io.github.lemon_ant.jharmonizer.core.flow.FlowType;
import io.github.lemon_ant.jharmonizer.core.testutils.TestCaseResourceUtils;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class SourceProcessorE2EFixtureTest {

    private static final String FIXTURES_RESOURCE = "/test-cases/core/e2e/restructure/";
    private static final Path PROJECT_TEST_RESOURCES_ROOT = Path.of("src/test/resources");
    private static final URL FIXTURE_RESOURCES_ROOT_DIR =
            TestCaseResourceUtils.requireClasspathDirectoryUrl(FIXTURES_RESOURCE);
    private static final Path FIXTURES_ROOT = resolveFixturesRoot();
    private static final String INPUT_DIRECTORY = "input";
    private static final String EXPECTED_DIRECTORY = "expected";
    private static final String CONFIG_FILE = "config.yml";
    private static final String WORKING_DIRECTORY_NAME = "SourceProcessorE2E-working-dir";
    private static final String COMPILE_BEFORE_DIRECTORY_NAME = "SourceProcessorE2E-compile-before";
    private static final String COMPILE_AFTER_DIRECTORY_NAME = "SourceProcessorE2E-compile-after";

    @TempDir
    Path temporaryDirectory;

    @ParameterizedTest(name = "[{index}] {0}/{1}")
    @MethodSource("fixtureInputFiles")
    void processFixtureInputFile_matchesExpectedAndCompileAfter(Path scenarioDir, Path sourceFile)
            throws Exception {
        Path fixtureScenario = FIXTURES_ROOT.resolve(scenarioDir);
        Path fixtureInputFile = resolveInput(fixtureScenario).resolve(sourceFile);
        Path expectedSourceFile = resolveExpected(fixtureScenario).resolve(sourceFile);
        String scenarioName = scenarioDir.toString();

        Path workingScenarioRoot =
                temporaryDirectory.resolve(WORKING_DIRECTORY_NAME).resolve(scenarioName);
        Path workingInputFile = copyInputJavaFile(fixtureInputFile, workingScenarioRoot);

        Path compileBeforeOutput =
                temporaryDirectory.resolve(COMPILE_BEFORE_DIRECTORY_NAME).resolve(scenarioName);
        Path compileAfterOutput =
                temporaryDirectory.resolve(COMPILE_AFTER_DIRECTORY_NAME).resolve(scenarioName);

        JavaCompileTestUtils.CompileResult compileBeforeResult =
                compileJavaSourceWithRelease21(workingInputFile, compileBeforeOutput);
        assertThat(compileBeforeResult.getExitCode())
                .as(
                        "Expected javac --release 21 to compile file %s. Diagnostics:%n%s",
                        workingInputFile, compileBeforeResult.getOutput())
                .isZero();

        JavaRunMainTestUtils.RunResult runBeforeResult = runJavaMainMethod(workingInputFile, compileBeforeOutput);
        assertThat(runBeforeResult.getExitCode())
                .as(
                        "Expected main method execution to succeed for %s. Output:%n%s",
                        runBeforeResult.getClassName(), runBeforeResult.getOutput())
                .isZero();

        assertFileIsNotProcessedYet(fixtureScenario, workingScenarioRoot, workingInputFile);

        runProcessorForSingleFile(workingInputFile, resolveConfig(fixtureScenario), FlowType.RESTRUCTURE);

        assertFileProcessingIsDeterministic(fixtureScenario, workingScenarioRoot, workingInputFile);

        JavaCompileTestUtils.CompileResult compileAfterResult =
                compileJavaSourceWithRelease21(workingInputFile, compileAfterOutput);
        assertThat(compileAfterResult.getExitCode())
                .as(
                        "Expected javac --release 21 to compile file %s. Diagnostics:%n%s",
                        workingInputFile, compileAfterResult.getOutput())
                .isZero();

        assertThat(workingInputFile).hasSameTextualContentAs(expectedSourceFile, StandardCharsets.UTF_8);

        JavaRunMainTestUtils.RunResult runAfterResult = runJavaMainMethod(workingInputFile, compileAfterOutput);
        assertThat(runAfterResult.getExitCode())
                .as(
                        "Expected main method execution to succeed for %s. Output:%n%s",
                        runAfterResult.getClassName(), runAfterResult.getOutput())
                .isZero();
    }

    @Test
    @Disabled("Utility only. Run manually to regenerate all e2e expected fixtures")
    void regenerateExpectedFixtures_whenRun_overwritesExpectedSources() throws Exception {
        Path regenerateWorkspace = temporaryDirectory.resolve("SourceProcessorE2E-regenerate-expected");

        fixtureInputFiles().forEach(fixture -> {
            Object[] argumentValues = fixture.get();
            Path scenarioDir = (Path) argumentValues[0];
            Path sourceFile = (Path) argumentValues[1];
            Path fixtureScenario = FIXTURES_ROOT.resolve(scenarioDir);
            Path fixtureInputFile = resolveInput(fixtureScenario).resolve(sourceFile);
            Path projectExpectedSourceFile = resolveProjectExpectedSourceFile(scenarioDir, sourceFile);
            String scenarioName = scenarioDir.toString();

            Path workingScenarioRoot = regenerateWorkspace.resolve(scenarioName);
            Path workingInputFile = copyInputJavaFile(fixtureInputFile, workingScenarioRoot);

            runProcessorForSingleFile(workingInputFile, resolveConfig(fixtureScenario), FlowType.RESTRUCTURE);

            try {
                Files.createDirectories(projectExpectedSourceFile.getParent());
                Files.copy(workingInputFile, projectExpectedSourceFile, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException exception) {
                throw new IllegalStateException(
                        "Failed to regenerate expected fixture in project resources: " + projectExpectedSourceFile,
                        exception);
            }

            assertThat(projectExpectedSourceFile)
                    .as("Expected source file should exist after regeneration: %s", projectExpectedSourceFile)
                    .exists();
        });
    }

    private static Stream<Arguments> fixtureInputFiles() throws IOException {
        return SourceFilesHandler.findJavaFiles(FIXTURES_ROOT, List.of("**/" + INPUT_DIRECTORY + "/*.java"), List.of())
                .sorted()
                .map(fixtureInputFile -> {
                    Path scenarioDir = fixtureInputFile.getParent().getParent().getFileName();
                    Path sourceFile = fixtureInputFile.getFileName();
                    return Arguments.of(scenarioDir, sourceFile);
                });
    }

    private static Path resolveFixturesRoot() {
        try {
            return Path.of(FIXTURE_RESOURCES_ROOT_DIR.toURI());
        } catch (URISyntaxException exception) {
            throw new IllegalArgumentException(
                    "Cannot convert fixtures URL to URI: " + FIXTURE_RESOURCES_ROOT_DIR, exception);
        }
    }

    private static void assertFileIsNotProcessedYet(
            Path fixtureScenario, Path workingScenarioRoot, Path workingInputFile) {
        assertThatThrownBy(() -> runProcessorForSingleFile(
                        workingInputFile, resolveConfig(fixtureScenario), FlowType.CHECK_FAIL_FAST))
                .isInstanceOf(RuntimeException.class);
    }

    private static void assertFileProcessingIsDeterministic(
            Path fixtureScenario, Path workingScenarioRoot, Path workingInputFile) {
        assertThatCode(() ->
                        runProcessorForSingleFile(workingInputFile, resolveConfig(fixtureScenario), FlowType.CHECK_ALL))
                .doesNotThrowAnyException();
    }

    private static void runProcessorForSingleFile(Path sourceFilePath, Path config, FlowType flowType) {
        UnifiedConfig unifiedConfig =
                JHarmonizerConfigurationManager.parseUnifiedConfigFromClasspathResource(E2EFileUtils.toUrl(config));
        FlexibleUnifiedConfig flexibleConfig = new FlexibleUnifiedConfig(
                unifiedConfig.getTopLevelTypesOrdering(),
                unifiedConfig.getFormatting(),
                unifiedConfig.isBackupsEnabled(),
                unifiedConfig.getHeaderLine(),
                unifiedConfig.getRootMemberGroups());
        SourceProcessor sourceProcessor = new SourceProcessor(flexibleConfig);
        sourceProcessor.processSources(
                sourceFilePath.getParent(), List.of(sourceFilePath.getFileName().toString()), List.of(), flowType);
    }

    private static Path copyInputJavaFile(Path fixtureInputFile, Path workingScenarioRoot) {
        Path targetFile = workingScenarioRoot.resolve(fixtureInputFile.getFileName());
        try {
            Files.createDirectories(targetFile.getParent());
            Files.copy(fixtureInputFile, targetFile);
            return targetFile;
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to copy fixture input file: " + fixtureInputFile, exception);
        }
    }

    private static Path resolveConfig(Path scenario) {
        return scenario.resolve(CONFIG_FILE);
    }

    private static Path resolveExpected(Path scenario) {
        return scenario.resolve(EXPECTED_DIRECTORY);
    }

    private static Path resolveProjectExpectedSourceFile(Path scenarioDir, Path sourceFile) {
        String fixturesResourceRelative = FIXTURES_RESOURCE.substring(1);
        return PROJECT_TEST_RESOURCES_ROOT
                .resolve(fixturesResourceRelative)
                .resolve(scenarioDir)
                .resolve(EXPECTED_DIRECTORY)
                .resolve(sourceFile);
    }

    private static Path resolveInput(Path scenario) {
        return scenario.resolve(INPUT_DIRECTORY);
    }
}
