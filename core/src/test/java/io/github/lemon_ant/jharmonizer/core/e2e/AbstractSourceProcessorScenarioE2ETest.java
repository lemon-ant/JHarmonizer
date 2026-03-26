package io.github.lemon_ant.jharmonizer.core.e2e;

import static io.github.lemon_ant.jharmonizer.core.e2e.JavaCompileTestUtils.compileJavaSourceWithRelease21;
import static io.github.lemon_ant.jharmonizer.core.e2e.JavaRunMainTestUtils.runJavaMainMethod;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.lemon_ant.jharmonizer.core.SourceProcessor;
import io.github.lemon_ant.jharmonizer.core.config.input.jharmonizer.JHarmonizerConfigurationManager;
import io.github.lemon_ant.jharmonizer.core.config.unified.FlexibleUnifiedConfig;
import io.github.lemon_ant.jharmonizer.core.files_handler.SourceFilesHandler;
import io.github.lemon_ant.jharmonizer.core.flow.FlowType;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import lombok.NonNull;
import org.junit.jupiter.params.provider.Arguments;

abstract class AbstractSourceProcessorScenarioE2ETest {

    private static final String INPUT_DIRECTORY = "input";
    private static final String EXPECTED_DIRECTORY = "expected";
    private static final Pattern SCENARIO_PREFIX_PATTERN = Pattern.compile("^(\\d+)-.+$");

    protected final void processFixtureInputFileMatchesExpectedAndCompileAfter(
            Path temporaryDirectory, Path scenarioDir, Path sourceFile) throws Exception {
        // Given
        Path fixtureScenario = getFixturesRoot().resolve(scenarioDir);
        Path fixtureInputFile = resolveInput(fixtureScenario).resolve(sourceFile);
        Path expectedSourceFile = resolveExpected(fixtureScenario).resolve(sourceFile);
        String scenarioName = scenarioDir.toString();
        String inputSourceCode = Files.readString(fixtureInputFile, StandardCharsets.UTF_8);
        String expectedSourceCode = Files.readString(expectedSourceFile, StandardCharsets.UTF_8);

        Path workingScenarioRoot =
                temporaryDirectory.resolve(resolveWorkspaceDirectoryName()).resolve(scenarioName);
        Path workingInputFile = copyInputJavaFile(fixtureInputFile, workingScenarioRoot);
        boolean unchangedFixture = inputSourceCode.equals(expectedSourceCode);

        Path compileBeforeOutput =
                temporaryDirectory.resolve(resolveCompileBeforeDirectoryName()).resolve(scenarioName);
        Path compileAfterOutput =
                temporaryDirectory.resolve(resolveCompileAfterDirectoryName()).resolve(scenarioName);

        JavaCompileTestUtils.CompileResult compileBeforeResult =
                compileJavaSourceWithRelease21(workingInputFile, compileBeforeOutput);
        assertThat(compileBeforeResult.getExitCode())
                .as(
                        "Expected javac --release 21 to compile file %s. Diagnostics:%n%s",
                        workingInputFile, compileBeforeResult.getOutput())
                .isZero();

        assertMainMethodExecutionSucceedsWhenPresent(workingInputFile, compileBeforeOutput);

        assertFileIsNotProcessedYet(fixtureScenario, workingInputFile, unchangedFixture);

        // When
        runProcessorForSingleFile(workingInputFile, findScenarioConfigPath(fixtureScenario), FlowType.RESTRUCTURE);

        // Then
        assertFileProcessingIsDeterministic(fixtureScenario, workingInputFile);

        JavaCompileTestUtils.CompileResult compileAfterResult =
                compileJavaSourceWithRelease21(workingInputFile, compileAfterOutput);
        assertThat(compileAfterResult.getExitCode())
                .as(
                        "Expected javac --release 21 to compile file %s. Diagnostics:%n%s",
                        workingInputFile, compileAfterResult.getOutput())
                .isZero();

        assertMainMethodExecutionSucceedsWhenPresent(workingInputFile, compileAfterOutput);

        String workingInputFileSrc = Files.readString(workingInputFile, StandardCharsets.UTF_8);
        assertThat(workingInputFileSrc).isEqualTo(expectedSourceCode);
    }

    protected final void fixtureScenarioDirectoriesNumberingValidatedHaveUniqueSequentialNumbersWithoutGaps()
            throws Exception {
        // Given
        List<String> scenarioNames;
        try (Stream<Path> children = Files.list(getFixturesRoot())) {
            scenarioNames = children.filter(Files::isDirectory)
                    .map(path -> path.getFileName().toString())
                    .sorted(Comparator.naturalOrder())
                    .toList();
        }

        // When / Then
        assertThat(scenarioNames)
                .as("Expected at least one fixture scenario directory")
                .isNotEmpty();

        int[] scenarioNumbers = scenarioNames.stream()
                .mapToInt(scenarioName -> {
                    Matcher matcher = SCENARIO_PREFIX_PATTERN.matcher(scenarioName);
                    assertThat(matcher.matches())
                            .as(
                                    "Scenario directory should start with a numeric prefix followed by '-': %s",
                                    scenarioName)
                            .isTrue();
                    return Integer.parseInt(matcher.group(1));
                })
                .toArray();

        assertThat(scenarioNumbers)
                .as("Scenario numbering must be consecutive starting from 1")
                .containsExactly(
                        IntStream.rangeClosed(1, scenarioNumbers.length).toArray());
    }

    @NonNull
    protected final Stream<Arguments> fixtureInputFiles() throws IOException {
        Comparator<Path> fixtureExecutionOrder = Comparator.comparing(
                        this::resolveScenarioDirectoryName, Comparator.reverseOrder())
                .thenComparing(Path::getFileName, Comparator.naturalOrder());
        return SourceFilesHandler.findJavaFiles(
                        getFixturesRoot(), List.of("**/" + INPUT_DIRECTORY + "/*.java"), List.of())
                .sorted(fixtureExecutionOrder)
                .map(fixtureInputFile -> {
                    Path scenarioDir = fixtureInputFile.getParent().getParent().getFileName();
                    Path sourceFile = fixtureInputFile.getFileName();
                    return Arguments.of(scenarioDir, sourceFile);
                });
    }

    @NonNull
    protected abstract Path getFixturesRoot();

    @NonNull
    protected abstract Optional<Path> findScenarioConfigPath(Path fixtureScenario);

    @NonNull
    protected abstract String resolveWorkspaceDirectoryName();

    @NonNull
    protected abstract String resolveCompileBeforeDirectoryName();

    @NonNull
    protected abstract String resolveCompileAfterDirectoryName();

    @NonNull
    private static Path resolveExpected(Path scenario) {
        return scenario.resolve(EXPECTED_DIRECTORY);
    }

    @NonNull
    private static Path resolveInput(Path scenario) {
        return scenario.resolve(INPUT_DIRECTORY);
    }

    @NonNull
    private String resolveScenarioDirectoryName(Path fixtureInputFile) {
        return fixtureInputFile.getParent().getParent().getFileName().toString();
    }

    private static void assertMainMethodExecutionSucceedsWhenPresent(Path srcFile, Path compiledOutputDirectory)
            throws IOException, InterruptedException {
        if (!containsMainMethodDeclaration(srcFile)) {
            return;
        }

        JavaRunMainTestUtils.RunResult runResult = runJavaMainMethod(srcFile, compiledOutputDirectory);
        assertThat(runResult.getExitCode())
                .as(
                        "Expected main method execution to succeed for %s. Output:%n%s",
                        runResult.getClassName(), runResult.getOutput())
                .isZero();
    }

    private static boolean containsMainMethodDeclaration(Path srcFile) {
        try (Stream<String> lines = Files.lines(srcFile, StandardCharsets.UTF_8)) {
            return lines.map(String::trim).anyMatch(line -> line.contains("public static void main("));
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to inspect source file for main method: " + srcFile, exception);
        }
    }

    private void assertFileIsNotProcessedYet(Path fixtureScenario, Path workingInputFile, boolean unchangedFixture) {
        Optional<Path> scenarioConfigPath = findScenarioConfigPath(fixtureScenario);
        if (unchangedFixture) {
            assertThatCode(() ->
                            runProcessorForSingleFile(workingInputFile, scenarioConfigPath, FlowType.CHECK_FAIL_FAST))
                    .doesNotThrowAnyException();
            return;
        }

        assertThatThrownBy(
                        () -> runProcessorForSingleFile(workingInputFile, scenarioConfigPath, FlowType.CHECK_FAIL_FAST))
                .isInstanceOf(RuntimeException.class);
    }

    private void assertFileProcessingIsDeterministic(Path fixtureScenario, Path workingInputFile) {
        assertThatCode(() -> runProcessorForSingleFile(
                        workingInputFile, findScenarioConfigPath(fixtureScenario), FlowType.CHECK_ALL))
                .doesNotThrowAnyException();
    }

    private void runProcessorForSingleFile(Path sourceFilePath, Optional<Path> scenarioConfigPath, FlowType flowType) {
        SourceProcessor sourceProcessor = buildSourceProcessor(scenarioConfigPath);
        sourceProcessor.processSources(
                sourceFilePath.getParent(), List.of(sourceFilePath.getFileName().toString()), List.of(), flowType);
    }

    @NonNull
    private static SourceProcessor buildSourceProcessor(Optional<Path> scenarioConfigPath) {
        if (scenarioConfigPath.isEmpty()) {
            return new SourceProcessor();
        }

        FlexibleUnifiedConfig flexibleConfig =
                JHarmonizerConfigurationManager.parseFlexibleUnifiedConfigFromClasspathResource(
                        E2EFileUtils.toUrl(scenarioConfigPath.orElseThrow()));
        return new SourceProcessor(flexibleConfig);
    }

    @NonNull
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
}
