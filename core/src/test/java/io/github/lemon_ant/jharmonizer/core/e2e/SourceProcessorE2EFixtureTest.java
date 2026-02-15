package io.github.lemon_ant.jharmonizer.core.e2e;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.lemon_ant.jharmonizer.core.SourceProcessor;
import io.github.lemon_ant.jharmonizer.core.config.input.jharmonizer.JHarmonizerConfigurationManager;
import io.github.lemon_ant.jharmonizer.core.config.unified.FlexibleUnifiedConfig;
import io.github.lemon_ant.jharmonizer.core.config.unified.UnifiedConfig;
import io.github.lemon_ant.jharmonizer.core.config.unified.UnifiedFormatterStyle;
import io.github.lemon_ant.jharmonizer.core.files_handler.SourceFilesHandler;
import io.github.lemon_ant.jharmonizer.core.flow.FlowType;
import io.github.lemon_ant.jharmonizer.core.formatter.Formatter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;
import lombok.NonNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SourceProcessorE2EFixtureTest {

    private static final Path E2E_FIXTURES_ROOT = Path.of("src/test/resources/test-cases/core/e2e/restructure");
    private static final List<String> REQUIRED_SCENARIO_NAMES = List.of(
            "01-baseline-ordering",
            "02-static-vs-instance-initializers",
            "03-field-initializer-chain",
            "04-cycle-scc-bundling",
            "05-keep-accessors-together",
            "06-enum-preserve-and-method-order",
            "07-record-method-ordering",
            "08-separator-variants");

    @TempDir
    Path temporaryDirectoryPath;

    @Test
    void processFixtureScenarios_allScenarios_matchExpectedAndCompileAfter() throws Exception {
        // Given
        assertThat(E2E_FIXTURES_ROOT).exists().isDirectory();
        List<Path> scenarioDirectoryPaths = loadScenarioDirectories();
        Path workingInputRootDirectoryPath = temporaryDirectoryPath.resolve("working-input");
        List<ScenarioContext> scenarioContexts = scenarioDirectoryPaths.stream()
                .map(scenarioDirectoryPath -> prepareScenarioContext(scenarioDirectoryPath, workingInputRootDirectoryPath))
                .toList();
        Path beforeCompileOutputDirectoryPath = temporaryDirectoryPath.resolve("before-compile");
        Path afterCompileOutputDirectoryPath = temporaryDirectoryPath.resolve("after-compile");

        // When
        JavaCompileTestUtils.assertJavaSourcesCompileWithRelease21(workingInputRootDirectoryPath, beforeCompileOutputDirectoryPath);
        assertAllScenarioInputsAreNotStableForCurrentConfig(scenarioContexts);
        runRestructureForAllScenarios(scenarioContexts);
        runPostPalantirFormattingPhase(workingInputRootDirectoryPath);
        assertAllScenarioInputsAreStableForCurrentConfig(scenarioContexts);
        JavaCompileTestUtils.assertJavaSourcesCompileWithRelease21(workingInputRootDirectoryPath, afterCompileOutputDirectoryPath);

        // Then
        scenarioContexts.forEach(SourceProcessorE2EFixtureTest::assertScenarioOutputMatchesExpectedExactly);
    }

    private void assertAllScenarioInputsAreNotStableForCurrentConfig(List<ScenarioContext> scenarioContexts) {
        assertThatThrownBy(() -> scenarioContexts.forEach(scenarioContext -> runSourceProcessor(
                        scenarioContext.workingInputDirectoryPath(), scenarioContext.configPath(), FlowType.CHECK_FAIL_FAST)))
                .isInstanceOf(RuntimeException.class);
    }

    private static void runRestructureForAllScenarios(List<ScenarioContext> scenarioContexts) {
        scenarioContexts.forEach(
                scenarioContext -> runSourceProcessor(scenarioContext.workingInputDirectoryPath(), scenarioContext.configPath(), FlowType.RESTRUCTURE));
    }

    private void assertAllScenarioInputsAreStableForCurrentConfig(List<ScenarioContext> scenarioContexts) {
        assertThatCode(() -> scenarioContexts.forEach(scenarioContext -> runSourceProcessor(
                        scenarioContext.workingInputDirectoryPath(), scenarioContext.configPath(), FlowType.CHECK_FAIL_FAST)))
                .doesNotThrowAnyException();
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

    private static void runPostPalantirFormattingPhase(Path sourceDirectoryPath) throws IOException {
        Formatter palantirFormatter = new Formatter(UnifiedFormatterStyle.PALANTIR, false);

        try (Stream<Path> sourcePathStream = Files.walk(sourceDirectoryPath)) {
            sourcePathStream.filter(path -> path.toString().endsWith(".java")).sorted().forEach(sourcePath -> {
                SourceFilesHandler.SrcFile sourceFile = SourceFilesHandler.readFile(sourcePath);
                String formattedSourceCode =
                        palantirFormatter.formatSource(sourceFile.getSrcCode(), sourcePath).getFormatedSrcCode();
                SourceFilesHandler.overwrite(sourcePath, formattedSourceCode);
            });
        }
    }

    private static void assertScenarioOutputMatchesExpectedExactly(ScenarioContext scenarioContext) {
        try (Stream<Path> expectedPathStream = Files.walk(scenarioContext.expectedDirectoryPath())) {
            expectedPathStream
                    .filter(path -> path.toString().endsWith(".java"))
                    .sorted()
                    .forEach(expectedSourcePath -> {
                        Path relativeSourcePath = scenarioContext.expectedDirectoryPath().relativize(expectedSourcePath);
                        Path actualSourcePath = scenarioContext.workingInputDirectoryPath().resolve(relativeSourcePath);
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
                    "Failed to verify scenario output for " + scenarioContext.name(), ioException);
        }
    }

    @NonNull
    private List<Path> loadScenarioDirectories() throws IOException {
        try (Stream<Path> scenarioPathStream = Files.list(E2E_FIXTURES_ROOT)) {
            List<Path> scenarioDirectoryPaths = scenarioPathStream
                    .filter(Files::isDirectory)
                    .sorted(Comparator.comparing(path -> path.getFileName().toString()))
                    .toList();
            List<String> actualScenarioNames = scenarioDirectoryPaths.stream()
                    .map(path -> path.getFileName().toString())
                    .toList();
            assertThat(actualScenarioNames).containsExactlyElementsOf(REQUIRED_SCENARIO_NAMES);
            return scenarioDirectoryPaths;
        }
    }

    @NonNull
    private ScenarioContext prepareScenarioContext(Path scenarioDirectoryPath, Path workingInputRootDirectoryPath) {
        try {
            Path fixtureInputDirectoryPath = scenarioDirectoryPath.resolve("input");
            Path fixtureExpectedDirectoryPath = scenarioDirectoryPath.resolve("expected");
            Path fixtureConfigPath = scenarioDirectoryPath.resolve("config.yml");
            Path scenarioWorkingInputDirectoryPath =
                    workingInputRootDirectoryPath.resolve(scenarioDirectoryPath.getFileName().toString());
            copyDirectory(fixtureInputDirectoryPath, scenarioWorkingInputDirectoryPath);
            return new ScenarioContext(
                    scenarioDirectoryPath.getFileName().toString(),
                    fixtureExpectedDirectoryPath,
                    fixtureConfigPath,
                    scenarioWorkingInputDirectoryPath);
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to prepare E2E scenario: " + scenarioDirectoryPath.getFileName(), exception);
        }
    }

    private static void copyDirectory(Path sourceDirectoryPath, Path targetDirectoryPath) throws IOException {
        try (Stream<Path> sourcePathStream = Files.walk(sourceDirectoryPath)) {
            for (Path sourcePath : sourcePathStream.sorted().toList()) {
                Path relativePath = sourceDirectoryPath.relativize(sourcePath);
                Path targetPath = targetDirectoryPath.resolve(relativePath);
                if (Files.isDirectory(sourcePath)) {
                    Files.createDirectories(targetPath);
                } else {
                    Files.createDirectories(targetPath.getParent());
                    Files.copy(sourcePath, targetPath);
                }
            }
        }
    }

    private static URL toUrl(Path path) {
        try {
            return path.toUri().toURL();
        } catch (MalformedURLException exception) {
            throw new IllegalArgumentException("Cannot convert path to URL: " + path, exception);
        }
    }

    private record ScenarioContext(
            String name, Path expectedDirectoryPath, Path configPath, Path workingInputDirectoryPath) {}
}
