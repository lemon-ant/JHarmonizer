package io.github.lemon_ant.jharmonizer.core.e2e;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.lemon_ant.jharmonizer.core.config.compiled.CompiledConfig;
import io.github.lemon_ant.jharmonizer.core.config.compiled.Unified2CompiledModelCompiler;
import io.github.lemon_ant.jharmonizer.core.config.input.jharmonizer.JHarmonizerConfigurationManager;
import io.github.lemon_ant.jharmonizer.core.files_handler.SourceFilesHandler;
import io.github.lemon_ant.jharmonizer.core.flow.RestructureFlow;
import io.github.lemon_ant.jharmonizer.core.formatter.Formatter;
import io.github.lemon_ant.jharmonizer.core.sorter.Sorter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Comparator;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SourceProcessorE2EFixtureTest {

    private static final Path E2E_FIXTURES_ROOT =
            Path.of("src/test/resources/test-cases/core/e2e/restructure");
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

        // When
        try (Stream<Path> scenarioPathStream = Files.list(E2E_FIXTURES_ROOT)) {
            List<Path> scenarioDirectoryPaths = scenarioPathStream
                    .filter(Files::isDirectory)
                    .sorted(Comparator.comparing(path -> path.getFileName().toString()))
                    .toList();
            List<String> actualScenarioNames = scenarioDirectoryPaths.stream()
                    .map(path -> path.getFileName().toString())
                    .toList();
            assertThat(actualScenarioNames).containsExactlyElementsOf(REQUIRED_SCENARIO_NAMES);
            scenarioDirectoryPaths.forEach(this::verifyScenario);
        }

        // Then
        assertThat(temporaryDirectoryPath).exists();
    }

    private void verifyScenario(Path scenarioDirectoryPath) {
        try {
            // Given
            Path fixtureInputDirectoryPath = scenarioDirectoryPath.resolve("input");
            Path fixtureExpectedDirectoryPath = scenarioDirectoryPath.resolve("expected");
            Path fixtureConfigPath = scenarioDirectoryPath.resolve("config.yml");
            Path scenarioWorkingInputDirectoryPath = temporaryDirectoryPath.resolve(
                    scenarioDirectoryPath.getFileName().toString() + "-input");
            copyDirectory(fixtureInputDirectoryPath, scenarioWorkingInputDirectoryPath);
            Path beforeCompileOutputDirectoryPath = temporaryDirectoryPath.resolve(
                    scenarioDirectoryPath.getFileName().toString() + "-before-compile");
            Path afterCompileOutputDirectoryPath = temporaryDirectoryPath.resolve(
                    scenarioDirectoryPath.getFileName().toString() + "-after-compile");

            // When
            JavaCompileTestUtils.assertJavaSourcesCompileWithRelease21(
                    scenarioWorkingInputDirectoryPath, beforeCompileOutputDirectoryPath);
            runRestructureFlow(scenarioWorkingInputDirectoryPath, fixtureConfigPath);
            JavaCompileTestUtils.assertJavaSourcesCompileWithRelease21(
                    scenarioWorkingInputDirectoryPath, afterCompileOutputDirectoryPath);

            // Then
            assertDirectoriesEqualWithNormalization(fixtureExpectedDirectoryPath, scenarioWorkingInputDirectoryPath);
        } catch (Exception exception) {
            throw new IllegalStateException("Failed E2E scenario: " + scenarioDirectoryPath.getFileName(), exception);
        }
    }

    private static void runRestructureFlow(Path sourceDirectoryPath, Path configPath) throws Exception {
        CompiledConfig compiledConfig = Unified2CompiledModelCompiler.compile(
                JHarmonizerConfigurationManager.parseUnifiedConfigFromClasspathResource(toUrl(configPath)));
        Formatter formatter = new Formatter(
                compiledConfig.getFormatting().getFormatterStyle(),
                compiledConfig.getFormatting().isFixImports());
        RestructureFlow restructureFlow = new RestructureFlow(
                formatter, compiledConfig.isBackupsEnabled(), new Sorter(compiledConfig));

        try (Stream<Path> sourcePathStream = Files.walk(sourceDirectoryPath)) {
            sourcePathStream
                    .filter(path -> path.toString().endsWith(".java"))
                    .sorted()
                    .map(SourceFilesHandler::readFile)
                    .forEach(restructureFlow::processSource);
        }
    }

    private static void assertDirectoriesEqualWithNormalization(Path expectedDirectoryPath, Path actualDirectoryPath)
            throws IOException {
        try (Stream<Path> expectedPathStream = Files.walk(expectedDirectoryPath)) {
            expectedPathStream
                    .filter(path -> path.toString().endsWith(".java"))
                    .sorted()
                    .forEach(expectedSourcePath -> {
                        Path relativeSourcePath = expectedDirectoryPath.relativize(expectedSourcePath);
                        Path actualSourcePath = actualDirectoryPath.resolve(relativeSourcePath);
                        assertThat(actualSourcePath)
                                .as("Processed source must exist: %s", relativeSourcePath)
                                .exists();
                        try {
                            String expectedSourceCode = Files.readString(expectedSourcePath, StandardCharsets.UTF_8);
                            String actualSourceCode = Files.readString(actualSourcePath, StandardCharsets.UTF_8);
                            assertThat(JavaCompileTestUtils.normalizeSourceForFixtureComparison(actualSourceCode))
                                    .as("Normalized output mismatch for %s", relativeSourcePath)
                                    .isEqualTo(JavaCompileTestUtils.normalizeSourceForFixtureComparison(expectedSourceCode));
                        } catch (IOException ioException) {
                            throw new IllegalStateException(
                                    "Failed to compare sources for " + relativeSourcePath, ioException);
                        }
                    });
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
}
