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
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

// TODO Create a method for expected fixtures regeneration
class SourceProcessorE2EFixtureTest {

    private static final String FIXTURES_RESOURCE = "/test-cases/core/e2e/restructure/";
    private static final URL FIXTURE_RESOURCES_ROOT_DIR =
            TestCaseResourceUtils.requireClasspathDirectoryUrl(FIXTURES_RESOURCE);
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
    void processFixtureInputFile_matchesExpectedAndCompileAfter(String scenarioName, String inputFileName)
            throws Exception {
        Path fixturesRoot = resolveFixturesRoot();
        Path fixtureScenario = fixturesRoot.resolve(scenarioName);
        Path fixtureInputFile = resolveInput(fixtureScenario).resolve(inputFileName);
        Path expectedSourceFile = resolveExpected(fixtureScenario).resolve(inputFileName);

        Path workingScenarioRoot = temporaryDirectory.resolve(WORKING_DIRECTORY_NAME).resolve(scenarioName);
        Path workingInputFile = copyInputJavaFile(fixtureInputFile, workingScenarioRoot);

        Path compileBeforeOutput = temporaryDirectory.resolve(COMPILE_BEFORE_DIRECTORY_NAME).resolve(scenarioName);
        Path compileAfterOutput = temporaryDirectory.resolve(COMPILE_AFTER_DIRECTORY_NAME).resolve(scenarioName);
        prepareOutputDirectoryForCurrentRun(compileBeforeOutput);
        prepareOutputDirectoryForCurrentRun(compileAfterOutput);

        JavaCompileTestUtils.CompileResult compileBeforeResult =
                compileJavaSourceWithRelease21(workingInputFile, compileBeforeOutput);
        assertThat(compileBeforeResult.exitCode())
                .as("Expected javac --release 21 to compile file %s. Diagnostics:%n%s", workingInputFile, compileBeforeResult.output())
                .isZero();
        JavaRunMainTestUtils.RunResult runBeforeResult = runJavaMainMethod(workingInputFile, compileBeforeOutput);
        assertThat(runBeforeResult.exitCode())
                .as(
                        "Expected main method execution to succeed for %s. Output:%n%s",
                        runBeforeResult.className(),
                        runBeforeResult.output())
                .isZero();
        assertFixtureIsNotStable(fixtureScenario, workingScenarioRoot);

        runProcessor(workingScenarioRoot, resolveConfig(fixtureScenario), FlowType.RESTRUCTURE);

        assertFixtureIsStable(fixtureScenario, workingScenarioRoot);
        JavaCompileTestUtils.CompileResult compileAfterResult =
                compileJavaSourceWithRelease21(workingInputFile, compileAfterOutput);
        assertThat(compileAfterResult.exitCode())
                .as("Expected javac --release 21 to compile file %s. Diagnostics:%n%s", workingInputFile, compileAfterResult.output())
                .isZero();
        assertOutputMatchesExpected(expectedSourceFile, workingInputFile);
        JavaRunMainTestUtils.RunResult runAfterResult = runJavaMainMethod(workingInputFile, compileAfterOutput);
        assertThat(runAfterResult.exitCode())
                .as(
                        "Expected main method execution to succeed for %s. Output:%n%s",
                        runAfterResult.className(),
                        runAfterResult.output())
                .isZero();
    }

    private static Stream<Arguments> fixtureInputFiles() throws IOException {
        Path fixturesRoot = resolveFixturesRoot();
        return SourceFilesHandler.findJavaFiles(fixturesRoot, List.of("**/" + INPUT_DIRECTORY + "/*.java"), List.of())
                .sorted()
                .map(inputFilePath -> Arguments.of(
                        inputFilePath.getName(inputFilePath.getNameCount() - 3).toString(),
                        inputFilePath.getFileName().toString()));
    }

    private static Path resolveFixturesRoot() {
        try {
            return Path.of(FIXTURE_RESOURCES_ROOT_DIR.toURI());
        } catch (URISyntaxException exception) {
            throw new IllegalArgumentException(
                    "Cannot convert fixtures URL to URI: " + FIXTURE_RESOURCES_ROOT_DIR, exception);
        }
    }

    private static void assertFixtureIsNotStable(Path fixtureScenario, Path workingScenarioRoot) {
        assertThatThrownBy(() -> runProcessor(workingScenarioRoot, resolveConfig(fixtureScenario), FlowType.CHECK_FAIL_FAST))
                .isInstanceOf(RuntimeException.class);
    }

    private static void assertFixtureIsStable(Path fixtureScenario, Path workingScenarioRoot) {
        assertThatCode(() -> runProcessor(workingScenarioRoot, resolveConfig(fixtureScenario), FlowType.CHECK_ALL))
                .doesNotThrowAnyException();
    }

    private static void runProcessor(Path scenarioSourcesRoot, Path config, FlowType flowType) {
        UnifiedConfig unifiedConfig =
                JHarmonizerConfigurationManager.parseUnifiedConfigFromClasspathResource(toUrl(config));
        FlexibleUnifiedConfig flexibleConfig = new FlexibleUnifiedConfig(
                unifiedConfig.getTopLevelTypesOrdering(),
                unifiedConfig.getFormatting(),
                unifiedConfig.isBackupsEnabled(),
                unifiedConfig.getHeaderLine(),
                unifiedConfig.getRootMemberGroups());
        SourceProcessor sourceProcessor = new SourceProcessor(flexibleConfig);
        sourceProcessor.processSources(scenarioSourcesRoot, List.of(), List.of(), flowType);
    }

    private static void assertOutputMatchesExpected(Path expectedSourceFile, Path actualSourceFile) {
        assertThat(expectedSourceFile).as("Expected source must exist: %s", expectedSourceFile).exists();
        assertThat(actualSourceFile).as("Processed source must exist: %s", actualSourceFile).exists();
        assertThat(actualSourceFile).hasSameTextualContentAs(expectedSourceFile, StandardCharsets.UTF_8);
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

    private static Path resolveInput(Path scenario) {
        return scenario.resolve(INPUT_DIRECTORY);
    }

    private static Path resolveConfig(Path scenario) {
        return scenario.resolve(CONFIG_FILE);
    }

    private static Path resolveExpected(Path scenario) {
        return scenario.resolve(EXPECTED_DIRECTORY);
    }

    private static void prepareOutputDirectoryForCurrentRun(Path outputDirectoryPath) throws IOException {
        FileUtils.forceMkdir(outputDirectoryPath.toFile());
        FileUtils.cleanDirectory(outputDirectoryPath.toFile());
    }

    private static URL toUrl(Path path) {
        try {
            return path.toUri().toURL();
        } catch (MalformedURLException exception) {
            throw new IllegalArgumentException("Cannot convert path to URL: " + path, exception);
        }
    }
}
