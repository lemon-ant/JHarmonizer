package io.github.lemon_ant.jharmonizer.core.e2e;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.lemon_ant.jharmonizer.core.SourceProcessor;
import io.github.lemon_ant.jharmonizer.core.config.input.jharmonizer.JHarmonizerConfigurationManager;
import io.github.lemon_ant.jharmonizer.core.config.unified.FlexibleUnifiedConfig;
import io.github.lemon_ant.jharmonizer.core.config.unified.UnifiedConfig;
import io.github.lemon_ant.jharmonizer.core.flow.FlowType;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SourceProcessorE2EFixtureTest {

    private static final Path E2E_FIXTURES_ROOT = Path.of("src/test/resources/test-cases/core/e2e/restructure");

    @TempDir
    Path temporaryDirectoryPath;

    @Test
    void processFixtureScenarios_allScenarios_matchExpectedAndCompileAfter() throws Exception {
        // Given
        assertThat(E2E_FIXTURES_ROOT).exists().isDirectory();
        List<Path> scenarioDirectories = loadScenarioDirectories();
        Path workingInputRootDirectoryPath = temporaryDirectoryPath.resolve("working-input");
        prepareWorkingInputDirectories(scenarioDirectories, workingInputRootDirectoryPath);
        Path beforeCompileOutputDirectoryPath = temporaryDirectoryPath.resolve("before-compile");
        Path afterCompileOutputDirectoryPath = temporaryDirectoryPath.resolve("after-compile");

        JavaCompileTestUtils.assertJavaSourcesCompileWithRelease21(
                workingInputRootDirectoryPath, beforeCompileOutputDirectoryPath);
        assertAllScenarioInputsAreNotStableForCurrentConfig(scenarioDirectories, workingInputRootDirectoryPath);

        // When
        runFlowForAllScenarios(scenarioDirectories, workingInputRootDirectoryPath, FlowType.RESTRUCTURE);

        // Then
        assertAllScenarioInputsAreStableForCurrentConfig(scenarioDirectories, workingInputRootDirectoryPath);
        JavaCompileTestUtils.assertJavaSourcesCompileWithRelease21(
                workingInputRootDirectoryPath, afterCompileOutputDirectoryPath);
        assertAllScenarioOutputsMatchExpectedExactly(scenarioDirectories, workingInputRootDirectoryPath);
    }

    private static void assertAllScenarioInputsAreNotStableForCurrentConfig(
            List<Path> scenarioDirectories, Path workingInputRootDirectoryPath) {
        assertThatThrownBy(() -> runFlowForAllScenarios(
                        scenarioDirectories, workingInputRootDirectoryPath, FlowType.CHECK_FAIL_FAST))
                .isInstanceOf(RuntimeException.class);
    }

    private static void assertAllScenarioInputsAreStableForCurrentConfig(
            List<Path> scenarioDirectories, Path workingInputRootDirectoryPath) {
        assertThatCode(() -> runFlowForAllScenarios(
                        scenarioDirectories, workingInputRootDirectoryPath, FlowType.CHECK_FAIL_FAST))
                .doesNotThrowAnyException();
    }

    private static void runFlowForAllScenarios(
            List<Path> scenarioDirectories, Path workingInputRootDirectoryPath, FlowType flowType) {
        scenarioDirectories.forEach(scenarioDirectoryPath -> runSourceProcessor(
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
            List<Path> scenarioDirectories, Path workingInputRootDirectoryPath) {
        scenarioDirectories.forEach(
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

    private static List<Path> loadScenarioDirectories() throws IOException {
        try (Stream<Path> scenarioPathStream = Files.list(E2E_FIXTURES_ROOT)) {
            List<Path> scenarioDirectoryPaths = scenarioPathStream
                    .filter(Files::isDirectory)
                    .sorted(Comparator.comparing(path -> path.getFileName().toString()))
                    .toList();
            assertThat(scenarioDirectoryPaths).isNotEmpty();
            return scenarioDirectoryPaths;
        }
    }

    private static void prepareWorkingInputDirectories(
            List<Path> scenarioDirectories, Path workingInputRootDirectoryPath) {
        scenarioDirectories.forEach(scenarioDirectoryPath -> {
            try {
                copyDirectory(
                        scenarioInputPath(scenarioDirectoryPath),
                        workingInputPathForScenario(workingInputRootDirectoryPath, scenarioDirectoryPath));
            } catch (IOException ioException) {
                throw new IllegalStateException(
                        "Failed to prepare E2E scenario: " + scenarioDirectoryPath.getFileName(), ioException);
            }
        });
    }

    private static void copyDirectory(Path sourceDirectoryPath, Path targetDirectoryPath) throws IOException {
        Files.walkFileTree(sourceDirectoryPath, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult preVisitDirectory(Path sourcePath, BasicFileAttributes attributes) throws IOException {
                Path relativePath = sourceDirectoryPath.relativize(sourcePath);
                Files.createDirectories(targetDirectoryPath.resolve(relativePath));
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path sourcePath, BasicFileAttributes attributes) throws IOException {
                Path relativePath = sourceDirectoryPath.relativize(sourcePath);
                Files.copy(sourcePath, targetDirectoryPath.resolve(relativePath), StandardCopyOption.REPLACE_EXISTING);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private static Path scenarioConfigPath(Path scenarioDirectoryPath) {
        return scenarioDirectoryPath.resolve("config.yml");
    }

    private static Path scenarioInputPath(Path scenarioDirectoryPath) {
        return scenarioDirectoryPath.resolve("input");
    }

    private static Path scenarioExpectedPath(Path scenarioDirectoryPath) {
        return scenarioDirectoryPath.resolve("expected");
    }

    private static Path workingInputPathForScenario(Path workingInputRootDirectoryPath, Path scenarioDirectoryPath) {
        return workingInputRootDirectoryPath.resolve(scenarioDirectoryPath.getFileName().toString());
    }

    private static URL toUrl(Path path) {
        try {
            return path.toUri().toURL();
        } catch (MalformedURLException exception) {
            throw new IllegalArgumentException("Cannot convert path to URL: " + path, exception);
        }
    }
}
