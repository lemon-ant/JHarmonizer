package io.github.lemon_ant.jharmonizer.core.e2e;

import static io.github.lemon_ant.jharmonizer.core.e2e.JavaRunMainTestUtils.runJavaMainMethod;
import static org.assertj.core.api.Assertions.assertThat;

import io.github.lemon_ant.jharmonizer.core.SrcProcessingResult;
import io.github.lemon_ant.jharmonizer.core.SrcProcessor;
import io.github.lemon_ant.jharmonizer.core.config.input.jharmonizer.JHarmonizerConfigurationManager;
import io.github.lemon_ant.jharmonizer.core.config.unified.FlexibleUnifiedConfig;
import io.github.lemon_ant.jharmonizer.core.files_handler.SrcFilesHandler;
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

abstract class AbstractSrcProcessorScenarioE2ETest<ValidationStateT> {

    private static final String INPUT_DIRECTORY = "input";
    private static final String EXPECTED_DIRECTORY = "expected";
    private static final Pattern SCENARIO_PREFIX_PATTERN = Pattern.compile("^(\\d+)-.+$");

    protected final void processFixtureInputFileMatchesExpectedAndCompileAfter(
            Path temporaryDirectory, Path scenarioDir, Path srcFile) throws Exception {
        // Given
        Path fixtureScenario = getFixturesRoot().resolve(scenarioDir);
        Path fixtureInputFile = resolveInput(fixtureScenario).resolve(srcFile);
        Path expectedSrcFile = resolveExpected(fixtureScenario).resolve(srcFile);
        String scenarioName = scenarioDir.toString();
        String inputSrcCode = Files.readString(fixtureInputFile, StandardCharsets.UTF_8);
        String expectedSrcCode = Files.readString(expectedSrcFile, StandardCharsets.UTF_8);

        Path workingScenarioRoot = temporaryDirectory
                .resolve(resolveDirectoryNamePrefix() + "-working-dir")
                .resolve(scenarioName);
        Path workingInputFile = copyInputJavaFile(fixtureInputFile, workingScenarioRoot);
        boolean unchangedFixture = inputSrcCode.equals(expectedSrcCode);

        Path compileBeforeOutput = temporaryDirectory
                .resolve(resolveDirectoryNamePrefix() + "-compile-before")
                .resolve(scenarioName);
        Path compileAfterOutput = temporaryDirectory
                .resolve(resolveDirectoryNamePrefix() + "-compile-after")
                .resolve(scenarioName);
        ValidationStateT beforeValidationState = validateBeforeProcessing(workingInputFile, compileBeforeOutput);

        assertFileIsNotProcessedYet(fixtureScenario, workingInputFile, unchangedFixture);

        // When
        runProcessorForSingleFile(
                workingInputFile, findScenarioConfigPath(fixtureScenario).orElse(null), FlowType.REORDER);

        // Then
        assertFileProcessingIsDeterministic(fixtureScenario, workingInputFile);

        validateAfterProcessing(workingInputFile, compileAfterOutput, beforeValidationState);

        String workingInputFileSrc = Files.readString(workingInputFile, StandardCharsets.UTF_8);
        assertThat(workingInputFileSrc).isEqualTo(expectedSrcCode);
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
    protected final Stream<Arguments> fixtureInputFiles() {
        Comparator<Path> fixtureExecutionOrder = Comparator.comparing(
                        this::resolveScenarioDirectoryName, Comparator.reverseOrder())
                .thenComparing(Path::getFileName, Comparator.naturalOrder());
        return SrcFilesHandler.findJavaFiles(getFixturesRoot(), List.of("**/" + INPUT_DIRECTORY + "/*.java"), List.of())
                .sorted(fixtureExecutionOrder)
                .map(fixtureInputFile -> {
                    Path scenarioDir = fixtureInputFile.getParent().getParent().getFileName();
                    Path srcFile = fixtureInputFile.getFileName();
                    return Arguments.of(scenarioDir, srcFile);
                });
    }

    @NonNull
    protected abstract Path getFixturesRoot();

    @NonNull
    protected abstract Optional<Path> findScenarioConfigPath(Path fixtureScenario);

    @NonNull
    protected abstract String resolveDirectoryNamePrefix();

    @NonNull
    protected abstract ValidationStateT validateBeforeProcessing(Path workingInputFile, Path compileBeforeOutput)
            throws Exception;

    protected abstract void validateAfterProcessing(
            Path workingInputFile, Path compileAfterOutput, ValidationStateT validationState) throws Exception;

    protected static void assertMainMethodExecutionSucceedsWhenPresent(Path srcFile, Path compiledOutputDirectory)
            throws IOException, InterruptedException {
        if (doesntContainMainMethodDeclaration(srcFile)) {
            return;
        }

        JavaRunMainTestUtils.RunResult runResult = runJavaMainMethod(srcFile, compiledOutputDirectory);
        assertThat(runResult.getExitCode())
                .as(
                        "Expected main method execution to succeed for %s. Output:%n%s",
                        runResult.getClassName(), runResult.getOutput())
                .isZero();
    }

    protected static boolean doesntContainMainMethodDeclaration(Path srcFile) {
        try (Stream<String> lines = Files.lines(srcFile, StandardCharsets.UTF_8)) {
            return lines.map(String::trim).noneMatch(line -> line.contains("public static void main("));
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to inspect source file for main method: " + srcFile, exception);
        }
    }

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

    private void assertFileIsNotProcessedYet(Path fixtureScenario, Path workingInputFile, boolean unchangedFixture) {
        Path scenarioConfigPath = findScenarioConfigPath(fixtureScenario).orElse(null);
        SrcProcessingResult result =
                runProcessorForSingleFile(workingInputFile, scenarioConfigPath, FlowType.CHECK_FAIL_FAST);
        if (unchangedFixture) {
            assertThat(result.isSuccess()).isTrue();
            return;
        }
        assertThat(result.isSuccess()).isFalse();
    }

    private void assertFileProcessingIsDeterministic(Path fixtureScenario, Path workingInputFile) {
        SrcProcessingResult result = runProcessorForSingleFile(
                workingInputFile, findScenarioConfigPath(fixtureScenario).orElse(null), FlowType.CHECK_FAIL_FAST);
        assertThat(result.isSuccess()).isTrue();
    }

    @NonNull
    private SrcProcessingResult runProcessorForSingleFile(
            Path srcFilePath, Path scenarioConfigPath, FlowType flowType) {
        SrcProcessor srcProcessor = buildSrcProcessor(scenarioConfigPath);
        return srcProcessor.processSources(
                srcFilePath.getParent(), List.of(srcFilePath.getFileName().toString()), List.of(), flowType);
    }

    @NonNull
    private static SrcProcessor buildSrcProcessor(Path scenarioConfigPath) {
        if (scenarioConfigPath == null) {
            return new SrcProcessor(disableProcessingStatisticsOutput(
                    FlexibleUnifiedConfig.builder().build()));
        }

        FlexibleUnifiedConfig flexibleConfig =
                JHarmonizerConfigurationManager.parseFlexibleUnifiedConfigFromClasspathResource(
                        E2EFileUtils.toUrl(scenarioConfigPath));
        return new SrcProcessor(disableProcessingStatisticsOutput(flexibleConfig));
    }

    @NonNull
    private static FlexibleUnifiedConfig disableProcessingStatisticsOutput(FlexibleUnifiedConfig flexibleConfig) {
        return FlexibleUnifiedConfig.builder()
                .topLevelTypesOrdering(flexibleConfig.getTopLevelTypesOrdering().orElse(null))
                .formatting(flexibleConfig.getFormatting().orElse(null))
                .backupsEnabled(flexibleConfig.getBackupsEnabled().orElse(null))
                .printProcessingStatistics(false)
                .headerLine(flexibleConfig.getHeaderLine().orElse(null))
                .rootMemberGroups(flexibleConfig.getRootMemberGroups().orElse(null))
                .build();
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
