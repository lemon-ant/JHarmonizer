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
import java.util.stream.Collectors;
import java.util.stream.Stream;
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

    @TempDir
    Path temporaryDirectory;

    @ParameterizedTest(name = "[{index}] {0}/{1}")
    @MethodSource("fixtureInputFiles")
    void processFixtureInputFile_matchesExpectedAndCompileAfter(String scenarioName, String inputFileName)
            throws Exception {
        // Given
        Path fixturesRoot = resolveFixturesRoot();
        Path fixtureScenario = fixturesRoot.resolve(scenarioName);
        Path fixtureInputFile = resolveInput(fixtureScenario).resolve(inputFileName);
        Path workingRoot = temporaryDirectory.resolve("SourceProcessorE2E-working-dir").resolve(scenarioName);
        Path workingInputFile = copyInputJavaFile(fixtureInputFile, workingRoot);
        Path compileBeforeOutput = temporaryDirectory.resolve("SourceProcessorE2E-compile-before").resolve(scenarioName);
        Path compileAfterOutput = temporaryDirectory.resolve("SourceProcessorE2E-compile-after").resolve(scenarioName);
        assertJavaSourcesCompileWithRelease21(workingRoot, compileBeforeOutput);
        assertJavaMainMethodsRunSuccessfully(workingRoot, compileBeforeOutput);
        assertFixtureIsNotStable(fixtureScenario, workingRoot);

        // When
        runProcessor(workingRoot, resolveConfig(fixtureScenario), FlowType.RESTRUCTURE);

        // Then
        assertFixtureIsStable(fixtureScenario, workingRoot);
        assertJavaSourcesCompileWithRelease21(workingRoot, compileAfterOutput);
        assertOutputMatchesExpected(fixtureScenario, inputFileName, workingInputFile);
        assertJavaMainMethodsRunSuccessfully(workingRoot, compileAfterOutput);
    }

    private static Stream<Arguments> fixtureInputFiles() throws IOException {
        Path fixturesRoot = resolveFixturesRoot();
        try (Stream<Path> scenarios = Files.list(fixturesRoot)) {
            List<Path> orderedScenarios =
                    scenarios.filter(Files::isDirectory).sorted().collect(Collectors.toList());
            return orderedScenarios.stream().flatMap(SourceProcessorE2EFixtureTest::scenarioInputFiles);
        }
    }

    private static Stream<Arguments> scenarioInputFiles(Path fixtureScenario) {
        Path inputRoot = resolveInput(fixtureScenario);
        try (Stream<Path> inputSources = Files.list(inputRoot)) {
            List<Path> orderedSources = inputSources
                    .filter(path -> path.toString().endsWith(".java"))
                    .sorted()
                    .collect(Collectors.toList());
            return orderedSources.stream().map(inputSource -> Arguments.of(
                    fixtureScenario.getFileName().toString(),
                    inputSource.getFileName().toString()));
        } catch (IOException ioException) {
            throw new IllegalStateException("Failed to collect fixture input files for " + fixtureScenario, ioException);
        }
    }

    private static Path resolveFixturesRoot() {
        try {
            return Path.of(FIXTURE_RESOURCES_ROOT_DIR.toURI());
        } catch (URISyntaxException exception) {
            throw new IllegalArgumentException(
                    "Cannot convert fixtures URL to URI: " + FIXTURE_RESOURCES_ROOT_DIR, exception);
        }
    }

    private static void assertFixtureIsNotStable(Path fixtureScenario, Path workingRoot) {
        assertThatThrownBy(() -> runProcessor(workingRoot, resolveConfig(fixtureScenario), FlowType.CHECK_FAIL_FAST))
                .isInstanceOf(RuntimeException.class);
    }

    private static void assertFixtureIsStable(Path fixtureScenario, Path workingRoot) {
        assertThatCode(() -> runProcessor(workingRoot, resolveConfig(fixtureScenario), FlowType.CHECK_ALL))
                .doesNotThrowAnyException();
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

    private static void assertOutputMatchesExpected(Path fixtureScenario, String inputFileName, Path workingInputFile) {
        Path expectedRoot = resolveExpected(fixtureScenario);
        Path expectedSource = expectedRoot.resolve(inputFileName);

        assertThat(expectedSource)
                .as("Expected source must exist: %s", inputFileName)
                .exists();
        assertThat(workingInputFile)
                .as("Processed source must exist: %s", inputFileName)
                .exists();
        try {
            String expectedCode = Files.readString(expectedSource, StandardCharsets.UTF_8);
            String actualCode = Files.readString(workingInputFile, StandardCharsets.UTF_8);
            assertThat(actualCode).isEqualToNormalizingNewlines(expectedCode);
        } catch (IOException ioException) {
            throw new IllegalStateException("Failed to compare sources for " + inputFileName, ioException);
        }
    }

    private static Path copyInputJavaFile(Path inputSource, Path workingRoot) {
        Path target = workingRoot.resolve(inputSource.getFileName());

        try {
            Files.createDirectories(target.getParent());
            Files.copy(inputSource, target);
            return target;
        } catch (IOException ioException) {
            throw new IllegalStateException("Failed to copy scenario input file: " + inputSource, ioException);
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

    private static URL toUrl(Path path) {
        try {
            return path.toUri().toURL();
        } catch (MalformedURLException exception) {
            throw new IllegalArgumentException("Cannot convert path to URL: " + path, exception);
        }
    }
}
