package io.github.lemon_ant.jharmonizer.core.e2e;

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
import java.util.function.Consumer;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SourceProcessorE2EFixtureTest {

    private static final String E2E_FIXTURES_ROOT_RESOURCE_PATH = "/test-cases/core/e2e/restructure/";
    private static final String INPUT_DIRECTORY_NAME = "input";
    private static final String EXPECTED_DIRECTORY_NAME = "expected";
    private static final String CONFIG_FILE_NAME = "config.yml";

    @TempDir
    Path temporaryDirectoryPath;

    @Test
    void processFixtureScenarios_allScenarios_matchExpectedAndCompileAfter() throws Exception {
        // Given
        Path e2eFixturesRootPath = resolveFixturesRootPath();
        Path workingInputRootDirectoryPath = temporaryDirectoryPath.resolve("working-input");
        copyOnlyInputJavaFiles(e2eFixturesRootPath, workingInputRootDirectoryPath);
        Path beforeCompileOutputDirectoryPath = temporaryDirectoryPath.resolve("before-compile");
        Path afterCompileOutputDirectoryPath = temporaryDirectoryPath.resolve("after-compile");

        JavaCompileTestUtils.assertJavaSourcesCompileWithRelease21(
                workingInputRootDirectoryPath, beforeCompileOutputDirectoryPath);
        assertAllScenarioInputsAreNotStableForCurrentConfig(e2eFixturesRootPath, workingInputRootDirectoryPath);

        // When
        runFlowForAllScenarios(e2eFixturesRootPath, workingInputRootDirectoryPath, FlowType.RESTRUCTURE);

        // Then
        assertAllScenarioInputsAreStableForCurrentConfig(e2eFixturesRootPath, workingInputRootDirectoryPath);
        JavaCompileTestUtils.assertJavaSourcesCompileWithRelease21(
                workingInputRootDirectoryPath, afterCompileOutputDirectoryPath);
        assertAllScenarioOutputsMatchExpectedExactly(e2eFixturesRootPath, workingInputRootDirectoryPath);
    }

    private static Path resolveFixturesRootPath() {
        URL fixturesRootUrl = TestCaseResourceUtils.requireClasspathDirectoryUrl(E2E_FIXTURES_ROOT_RESOURCE_PATH);
        try {
            return Path.of(fixturesRootUrl.toURI());
        } catch (URISyntaxException exception) {
            throw new IllegalArgumentException("Cannot convert fixtures URL to URI: " + fixturesRootUrl, exception);
        }
    }

    private static void assertAllScenarioInputsAreNotStableForCurrentConfig(
            Path e2eFixturesRootPath, Path workingInputRootDirectoryPath) throws IOException {
        assertThatThrownBy(() -> runFlowForAllScenarios(e2eFixturesRootPath, workingInputRootDirectoryPath, FlowType.CHECK_FAIL_FAST))
                .isInstanceOf(RuntimeException.class);
    }

    private static void assertAllScenarioInputsAreStableForCurrentConfig(
            Path e2eFixturesRootPath, Path workingInputRootDirectoryPath) throws IOException {
        assertThatCode(() -> runFlowForAllScenarios(e2eFixturesRootPath, workingInputRootDirectoryPath, FlowType.CHECK_FAIL_FAST))
                .doesNotThrowAnyException();
    }

    private static void runFlowForAllScenarios(Path e2eFixturesRootPath, Path workingInputRootDirectoryPath, FlowType flowType)
            throws IOException {
        forEachScenarioDirectory(e2eFixturesRootPath, scenarioDirectoryPath -> runSourceProcessor(
                workingInputPathForScenario(workingInputRootDirectoryPath, scenarioDirectoryPath),
                scenarioConfigPath(scenarioDirectoryPath),
                flowType));
    }

    private static void runSourceProcessor(Path sourceDirectoryPath, Path configPath, FlowType flowType) {
        UnifiedConfig unifiedConfig = JHarmonizerConfigurationManager.parseUnifiedConfigFromClasspathResource(toUrl(configPath));
        FlexibleUnifiedConfig flexibleUnifiedConfig = new FlexibleUnifiedConfig(
                unifiedConfig.getTopLevelTypesOrdering(),
                unifiedConfig.getFormatting(),
                unifiedConfig.isBackupsEnabled(),
                unifiedConfig.getHeaderLine(),
                unifiedConfig.getRootMemberGroups());
        SourceProcessor sourceProcessor = new SourceProcessor(flexibleUnifiedConfig);
        sourceProcessor.processSources(sourceDirectoryPath, List.of(), List.of(), flowType);
    }

    private static void assertAllScenarioOutputsMatchExpectedExactly(
            Path e2eFixturesRootPath, Path workingInputRootDirectoryPath) throws IOException {
        forEachScenarioDirectory(
                e2eFixturesRootPath,
                scenarioDirectoryPath -> assertScenarioOutputMatchesExpectedExactly(
                        scenarioDirectoryPath, workingInputRootDirectoryPath));
    }

    private static void assertScenarioOutputMatchesExpectedExactly(
            Path scenarioDirectoryPath, Path workingInputRootDirectoryPath) {
        Path expectedDirectoryPath = scenarioExpectedPath(scenarioDirectoryPath);
        Path actualDirectoryPath = workingInputPathForScenario(workingInputRootDirectoryPath, scenarioDirectoryPath);

        try (Stream<Path> expectedPathStream = Files.walk(expectedDirectoryPath)) {
            expectedPathStream
                    .filter(path -> path.toString().endsWith(".java"))
                    .forEach(expectedSourcePath -> {
                        Path relativeSourcePath = expectedDirectoryPath.relativize(expectedSourcePath);
                        Path actualSourcePath = actualDirectoryPath.resolve(relativeSourcePath);
                        assertThat(actualSourcePath)
                                .as("Processed source must exist: %s", relativeSourcePath)
                                .exists();
                        try {
                            String expectedSourceCode = Files.readString(expectedSourcePath, StandardCharsets.UTF_8);
                            String actualSourceCode = Files.readString(actualSourcePath, StandardCharsets.UTF_8);
                            assertThat(actualSourceCode).isEqualToNormalizingNewlines(expectedSourceCode);
                        } catch (IOException ioException) {
                            throw new IllegalStateException(
                                    "Failed to compare sources for " + relativeSourcePath, ioException);
                        }
                    });
        } catch (IOException ioException) {
            throw new IllegalStateException(
                    "Failed to verify scenario output for " + scenarioDirectoryPath.getFileName(), ioException);
        }
    }

    private static void copyOnlyInputJavaFiles(Path e2eFixturesRootPath, Path workingInputRootDirectoryPath)
            throws IOException {
        try (Stream<Path> inputJavaPathStream = SourceFilesHandler.findJavaFiles(
                e2eFixturesRootPath,
                List.of("**/" + INPUT_DIRECTORY_NAME + "/*.java"),
                List.of())) {
            inputJavaPathStream.forEach(inputJavaPath -> copyInputJavaFile(inputJavaPath, e2eFixturesRootPath, workingInputRootDirectoryPath));
        }
    }

    private static void copyInputJavaFile(Path inputJavaPath, Path e2eFixturesRootPath, Path workingInputRootDirectoryPath) {
        Path relativeSourcePath = e2eFixturesRootPath.relativize(inputJavaPath);
        Path scenarioRelativePath = relativeSourcePath.getParent().getParent();
        Path targetPath = workingInputRootDirectoryPath.resolve(scenarioRelativePath).resolve(inputJavaPath.getFileName());

        try {
            Files.createDirectories(targetPath.getParent());
            Files.copy(inputJavaPath, targetPath);
        } catch (IOException ioException) {
            throw new IllegalStateException("Failed to copy scenario input file: " + inputJavaPath, ioException);
        }
    }

    private static void forEachScenarioDirectory(Path e2eFixturesRootPath, Consumer<Path> scenarioDirectoryConsumer)
            throws IOException {
        try (Stream<Path> scenarioPathStream = Files.list(e2eFixturesRootPath)) {
            scenarioPathStream
                    .filter(Files::isDirectory)
                    .forEach(scenarioDirectoryConsumer);
        }
    }

    private static Path scenarioConfigPath(Path scenarioDirectoryPath) {
        return scenarioDirectoryPath.resolve(CONFIG_FILE_NAME);
    }

    private static Path scenarioExpectedPath(Path scenarioDirectoryPath) {
        return scenarioDirectoryPath.resolve(EXPECTED_DIRECTORY_NAME);
    }

    private static Path workingInputPathForScenario(Path workingInputRootDirectoryPath, Path scenarioDirectoryPath) {
        return workingInputRootDirectoryPath.resolve(scenarioDirectoryPath.getFileName());
    }

    private static URL toUrl(Path path) {
        try {
            return path.toUri().toURL();
        } catch (MalformedURLException exception) {
            throw new IllegalArgumentException("Cannot convert path to URL: " + path, exception);
        }
    }
}
