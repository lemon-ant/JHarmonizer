// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
package io.github.lemon_ant.jharmonizer.core.e2e;

import static io.github.lemon_ant.jharmonizer.core.e2e.JavaCompileTestUtils.compileJavaSrcWithRelease21;
import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import lombok.NonNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
abstract class AbstractCompilableSrcProcessorE2ETest
        extends AbstractSrcProcessorScenarioE2ETest<AbstractCompilableSrcProcessorE2ETest.ValidationState> {

    @TempDir
    private Path temporaryDirectory;

    @Test
    void fixtureScenarioDirectories_numberingValidated_haveUniqueSequentialNumbersWithoutGaps() throws Exception {
        fixtureScenarioDirectoriesNumberingValidatedHaveUniqueSequentialNumbersWithoutGaps();
    }

    @ParameterizedTest(name = "[{index}] {0}/{1}")
    @MethodSource("fixtureInputFiles")
    void processFixtureInputFile_configuredScenario_matchesExpectedAndCompiles(
            @NonNull Path scenarioDir, @NonNull Path srcFile) throws Exception {
        processFixtureInputFileMatchesExpectedAndCompileAfter(temporaryDirectory, scenarioDir, srcFile);
    }

    private static void assertCompilesAndRuns(Path srcFile, Path outputDirectory) throws Exception {
        JavaCompileTestUtils.CompileResult result = compileJavaSrcWithRelease21(srcFile, outputDirectory);
        assertThat(result.getExitCode())
                .as("Expected javac --release 21 to compile %s. Diagnostics:%n%s", srcFile, result.getOutput())
                .isZero();
        assertMainMethodExecutionSucceedsWhenPresent(srcFile, outputDirectory);
    }

    @NonNull
    @Override
    protected final Optional<Path> findScenarioConfigPath(@NonNull Path fixtureScenario) {
        return Optional.of(fixtureScenario.resolve("config.yml"));
    }

    protected final void processVariant(
            @NonNull String scenario,
            @NonNull String fileName,
            @NonNull String inputSrcCode,
            @NonNull String expectedSrcCode)
            throws Exception {
        processSrcCodeMatchesExpectedAndCompileAfter(
                Files.createTempDirectory(temporaryDirectory, "variant-"),
                getFixturesRoot().resolve(scenario),
                Path.of(fileName),
                inputSrcCode,
                expectedSrcCode);
    }

    @NonNull
    @Override
    protected final String resolveDirectoryNamePrefix() {
        return getClass().getSimpleName();
    }

    @Override
    protected final void validateAfterProcessing(
            @NonNull Path workingInputFile, @NonNull Path compileAfterOutput, @NonNull ValidationState validationState)
            throws Exception {
        assertCompilesAndRuns(workingInputFile, compileAfterOutput);
    }

    @NonNull
    @Override
    protected final ValidationState validateBeforeProcessing(
            @NonNull Path workingInputFile, @NonNull Path compileBeforeOutput) throws Exception {
        assertCompilesAndRuns(workingInputFile, compileBeforeOutput);
        return ValidationState.INSTANCE;
    }

    enum ValidationState {
        INSTANCE
    }
}
