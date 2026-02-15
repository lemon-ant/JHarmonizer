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

    private static final String FIXTURES_RESOURCE = "/test-cases/core/e2e/restructure/";
    private static final String INPUT_DIRECTORY = "input";
    private static final String EXPECTED_DIRECTORY = "expected";
    private static final String CONFIG_FILE = "config.yml";

    @TempDir
    Path temporaryDirectory;

    @Test
    void processFixtureScenarios_allScenarios_matchExpectedAndCompileAfter() throws Exception {
        Path fixturesRoot = resolveFixturesRoot();
        Path workingRoot = temporaryDirectory.resolve("working-input");
        copyInputJavaFiles(fixturesRoot, workingRoot);
        Path beforeCompileOutput = temporaryDirectory.resolve("before-compile");
        Path afterCompileOutput = temporaryDirectory.resolve("after-compile");

        JavaCompileTestUtils.assertJavaSourcesCompileWithRelease21(workingRoot, beforeCompileOutput);
        assertScenariosAreNotStable(fixturesRoot, workingRoot);

        runFlowForScenarios(fixturesRoot, workingRoot, FlowType.RESTRUCTURE);

        assertScenariosAreStable(fixturesRoot, workingRoot);
        JavaCompileTestUtils.assertJavaSourcesCompileWithRelease21(workingRoot, afterCompileOutput);
        assertOutputsMatchExpected(fixturesRoot, workingRoot);
    }

    private static Path resolveFixturesRoot() {
        URL fixturesUrl = TestCaseResourceUtils.requireClasspathDirectoryUrl(FIXTURES_RESOURCE);
        try {
            return Path.of(fixturesUrl.toURI());
        } catch (URISyntaxException exception) {
            throw new IllegalArgumentException("Cannot convert fixtures URL to URI: " + fixturesUrl, exception);
        }
    }

    private static void assertScenariosAreNotStable(Path fixturesRoot, Path workingRoot) throws IOException {
        assertThatThrownBy(() -> runFlowForScenarios(fixturesRoot, workingRoot, FlowType.CHECK_FAIL_FAST))
                .isInstanceOf(RuntimeException.class);
    }

    private static void assertScenariosAreStable(Path fixturesRoot, Path workingRoot) throws IOException {
        assertThatCode(() -> runFlowForScenarios(fixturesRoot, workingRoot, FlowType.CHECK_ALL))
                .doesNotThrowAnyException();
    }

    private static void runFlowForScenarios(Path fixturesRoot, Path workingRoot, FlowType flowType) throws IOException {
        forEachScenario(fixturesRoot, scenario -> runProcessor(workingRoot, resolveConfig(scenario), flowType));
    }

    private static void runProcessor(Path sourcesRoot, Path config, FlowType flowType) {
        UnifiedConfig unifiedConfig = JHarmonizerConfigurationManager.parseUnifiedConfigFromClasspathResource(toUrl(config));
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
        forEachScenario(fixturesRoot, scenario -> assertScenarioOutputMatchesExpected(scenario, workingRoot));
    }

    private static void assertScenarioOutputMatchesExpected(Path scenario, Path workingRoot) {
        Path expectedRoot = resolveExpected(scenario);
        Path actualRoot = resolveScenarioWorkingDirectory(workingRoot, scenario);

        try (Stream<Path> expectedPaths = Files.walk(expectedRoot)) {
            expectedPaths.filter(path -> path.toString().endsWith(".java")).forEach(expectedSource -> {
                Path relativeSource = expectedRoot.relativize(expectedSource);
                Path actualSource = actualRoot.resolve(relativeSource);
                assertThat(actualSource).as("Processed source must exist: %s", relativeSource).exists();
                try {
                    String expectedCode = Files.readString(expectedSource, StandardCharsets.UTF_8);
                    String actualCode = Files.readString(actualSource, StandardCharsets.UTF_8);
                    assertThat(actualCode).isEqualToNormalizingNewlines(expectedCode);
                } catch (IOException ioException) {
                    throw new IllegalStateException("Failed to compare sources for " + relativeSource, ioException);
                }
            });
        } catch (IOException ioException) {
            throw new IllegalStateException("Failed to verify scenario output for " + scenario.getFileName(), ioException);
        }
    }

    private static void copyInputJavaFiles(Path fixturesRoot, Path workingRoot) throws IOException {
        try (Stream<Path> inputSources =
                SourceFilesHandler.findJavaFiles(fixturesRoot, List.of("**/" + INPUT_DIRECTORY + "/*.java"), List.of())) {
            inputSources.forEach(inputSource -> copyInputJavaFile(inputSource, fixturesRoot, workingRoot));
        }
    }

    private static void copyInputJavaFile(Path inputSource, Path fixturesRoot, Path workingRoot) {
        Path relativeSource = fixturesRoot.relativize(inputSource);
        Path scenarioRelative = relativeSource.getParent().getParent();
        Path target = workingRoot.resolve(scenarioRelative).resolve(inputSource.getFileName());

        try {
            Files.createDirectories(target.getParent());
            Files.copy(inputSource, target);
        } catch (IOException ioException) {
            throw new IllegalStateException("Failed to copy scenario input file: " + inputSource, ioException);
        }
    }

    private static void forEachScenario(Path fixturesRoot, Consumer<Path> action) throws IOException {
        try (Stream<Path> scenarios = Files.list(fixturesRoot)) {
            scenarios.filter(Files::isDirectory).forEach(action);
        }
    }

    private static Path resolveConfig(Path scenario) {
        return scenario.resolve(CONFIG_FILE);
    }

    private static Path resolveExpected(Path scenario) {
        return scenario.resolve(EXPECTED_DIRECTORY);
    }

    private static Path resolveScenarioWorkingDirectory(Path workingRoot, Path scenario) {
        return workingRoot.resolve(scenario.getFileName());
    }

    private static URL toUrl(Path path) {
        try {
            return path.toUri().toURL();
        } catch (MalformedURLException exception) {
            throw new IllegalArgumentException("Cannot convert path to URL: " + path, exception);
        }
    }
}
