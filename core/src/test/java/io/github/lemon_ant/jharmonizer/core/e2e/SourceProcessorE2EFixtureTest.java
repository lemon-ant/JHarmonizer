package io.github.lemon_ant.jharmonizer.core.e2e;

import static io.github.lemon_ant.jharmonizer.core.e2e.JavaCompileTestUtils.assertJavaSourcesCompileWithRelease21;
import static io.github.lemon_ant.jharmonizer.core.e2e.JavaRunMainTestUtils.assertJavaMainMethodsRunSuccessfully;
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
import java.util.function.BiConsumer;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

// TODO Create a method for expected fixtures regeneration
class SourceProcessorE2EFixtureTest {

    private static final String FIXTURES_RESOURCE = "/test-cases/core/e2e/restructure/";
    private static final URL FIXTURE_RESOURCES_ROOT_DIR =
            TestCaseResourceUtils.requireClasspathDirectoryUrl(FIXTURES_RESOURCE);
    private static final String INPUT_DIRECTORY = "input";
    private static final String EXPECTED_DIRECTORY = "expected";
    private static final String CONFIG_FILE = "config.yml";

    @TempDir
    Path temporaryDirectory;

    @Test
    // TODO Make it parameterized
    void processFixtureScenarios_allScenarios_matchExpectedAndCompileAfter() throws Exception {
        // Given
        Path fixturesRoot = resolveFixturesRoot();
        Path workingRoot = temporaryDirectory.resolve("SourceProcessorE2E-working-dir");
        copyInputJavaFiles(fixturesRoot, workingRoot);
        Path compileBeforeOutput = temporaryDirectory.resolve("SourceProcessorE2E-compile-before");
        Path compileAfterOutput = temporaryDirectory.resolve("SourceProcessorE2E-compile-after");
        assertJavaSourcesCompileWithRelease21(workingRoot, compileBeforeOutput);
        assertJavaMainMethodsRunSuccessfully(workingRoot, compileBeforeOutput);
        assertScenariosAreNotStable(fixturesRoot, workingRoot);

        // When
        runFlowForScenarios(fixturesRoot, workingRoot, FlowType.RESTRUCTURE);

        // Then
        assertScenariosAreStable(fixturesRoot, workingRoot);
        assertJavaSourcesCompileWithRelease21(workingRoot, compileAfterOutput);
        assertOutputsMatchExpected(fixturesRoot, workingRoot);
        assertJavaMainMethodsRunSuccessfully(workingRoot, compileAfterOutput);
    }

    private static Path resolveFixturesRoot() {
        try {
            return Path.of(FIXTURE_RESOURCES_ROOT_DIR.toURI());
        } catch (URISyntaxException exception) {
            throw new IllegalArgumentException(
                    "Cannot convert fixtures URL to URI: " + FIXTURE_RESOURCES_ROOT_DIR, exception);
        }
    }

    private static void assertScenariosAreNotStable(Path fixtureRoot, Path workingRoot) throws IOException {
        assertThatThrownBy(() -> runFlowForScenarios(fixtureRoot, workingRoot, FlowType.CHECK_FAIL_FAST))
                .isInstanceOf(RuntimeException.class);
    }

    private static void assertScenariosAreStable(Path fixtureRoot, Path workingRoot) throws IOException {
        assertThatCode(() -> runFlowForScenarios(fixtureRoot, workingRoot, FlowType.CHECK_ALL))
                .doesNotThrowAnyException();
    }

    private static void runFlowForScenarios(Path fixtureRoot, Path workingRoot, FlowType flowType) throws IOException {
        forEachScenario(
                fixtureRoot,
                workingRoot,
                (fixtureScenario, workingScenario) ->
                        runProcessor(workingScenario, resolveConfig(fixtureScenario), flowType));
    }

    private static void runProcessor(Path sourcesRoot, Path config, FlowType flowType) {
        UnifiedConfig unifiedConfig =
                JHarmonizerConfigurationManager.parseUnifiedConfigFromClasspathResource(toUrl(config));
        FlexibleUnifiedConfig flexibleConfig = new FlexibleUnifiedConfig(
                unifiedConfig.getTopLevelTypesOrdering(),
                unifiedConfig.getFormatting(),
                unifiedConfig.isBackupsEnabled(),
                unifiedConfig.getHeaderLine(),
                unifiedConfig.getRootMemberGroups());
        SourceProcessor sourceProcessor = new SourceProcessor(flexibleConfig);
        sourceProcessor.processSources(sourcesRoot, List.of(), List.of(), flowType);
    }

    private static void assertOutputsMatchExpected(Path fixturesRoot, Path workingRoot) throws IOException {
        forEachScenario(fixturesRoot, workingRoot, SourceProcessorE2EFixtureTest::assertScenarioOutputMatchesExpected);
    }

    private static void assertScenarioOutputMatchesExpected(Path fixtureScenario, Path workingScenario) {
        Path expectedRoot = resolveExpected(fixtureScenario);

        try (Stream<Path> expectedPaths = Files.walk(expectedRoot)) {
            expectedPaths.filter(path -> path.toString().endsWith(".java")).forEach(expectedSource -> {
                Path relativeSource = expectedRoot.relativize(expectedSource);
                Path actualSource = workingScenario.resolve(relativeSource);
                assertThat(actualSource)
                        .as("Processed source must exist: %s", relativeSource)
                        .exists();
                try {
                    // TODO Может есть готовый assert
                    String expectedCode = Files.readString(expectedSource, StandardCharsets.UTF_8);
                    String actualCode = Files.readString(actualSource, StandardCharsets.UTF_8);
                    assertThat(actualCode).isEqualToNormalizingNewlines(expectedCode);
                } catch (IOException ioException) {
                    throw new IllegalStateException("Failed to compare sources for " + relativeSource, ioException);
                }
            });
        } catch (IOException ioException) {
            throw new IllegalStateException(
                    "Failed to verify fixtureScenario output for " + fixtureScenario.getFileName(), ioException);
        }
    }

    private static void copyInputJavaFiles(Path fixturesRoot, Path workingRoot) throws IOException {
        try (Stream<Path> inputSources = SourceFilesHandler.findJavaFiles(
                fixturesRoot, List.of("**/" + INPUT_DIRECTORY + "/*.java"), List.of())) {
            inputSources.forEach(inputSource -> copyInputJavaFile(inputSource, workingRoot));
        }
    }

    private static void copyInputJavaFile(Path inputSource, Path workingRoot) {
        Path scenarioRelative = inputSource.getName(inputSource.getNameCount() - 3);
        Path target = workingRoot.resolve(scenarioRelative).resolve(inputSource.getFileName());

        try {
            Files.createDirectories(target.getParent());
            Files.copy(inputSource, target);
        } catch (IOException ioException) {
            throw new IllegalStateException("Failed to copy scenario input file: " + inputSource, ioException);
        }
    }

    private static void forEachScenario(Path fixturesRoot, Path workingRoot, BiConsumer<Path, Path> action)
            throws IOException {
        // TODO Parallel stream after debugging
        try (Stream<Path> scenarios = Files.list(workingRoot)) {
            scenarios.filter(Files::isDirectory).forEach(workingScenario -> {
                Path scenarioRelative = workingScenario.getName(workingScenario.getNameCount() - 1);
                Path fixtureScenario = fixturesRoot.resolve(scenarioRelative);
                action.accept(fixtureScenario, workingScenario);
            });
        }
    }

    private static Path resolveConfig(Path scenario) {
        return scenario.resolve(CONFIG_FILE);
    }

    private static Path resolveExpected(Path scenario) {
        return scenario.resolve(EXPECTED_DIRECTORY);
    }

    // TODO Проверить чтобы не было дублей кода
    private static URL toUrl(Path path) {
        try {
            return path.toUri().toURL();
        } catch (MalformedURLException exception) {
            throw new IllegalArgumentException("Cannot convert path to URL: " + path, exception);
        }
    }
}
