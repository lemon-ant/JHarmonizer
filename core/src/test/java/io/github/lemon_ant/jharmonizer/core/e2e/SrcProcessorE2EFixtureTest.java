// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
package io.github.lemon_ant.jharmonizer.core.e2e;

import static io.github.lemon_ant.jharmonizer.core.e2e.JavaCompileTestUtils.compileJavaSrcWithRelease21;
import static io.github.lemon_ant.jharmonizer.core.testutils.TestCaseResourceUtils.TEST_CASES_DIR;
import static org.assertj.core.api.Assertions.assertThat;

import io.github.lemon_ant.jharmonizer.core.testutils.TestCaseResourceUtils;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.util.Optional;
import lombok.NonNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

// PER_CLASS allows non-static @MethodSource from the shared base class.
// This test keeps only immutable constants plus @TempDir, and each scenario is processed in its own
// subdirectory, so no mutable scenario state leaks between parameterized invocations.
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SrcProcessorE2EFixtureTest
        extends AbstractSrcProcessorScenarioE2ETest<SrcProcessorE2EFixtureTest.StrictValidationState> {
    private static final String CONFIG_FILE = "config.yml";
    private static final String FIXTURES_RESOURCE = "/" + TEST_CASES_DIR + "/core/e2e/reorder/";

    @NonNull
    private static final Path FIXTURES_ROOT =
            resolveFixturesRoot(TestCaseResourceUtils.requireClasspathDirectoryUrl(FIXTURES_RESOURCE));

    private static final URL FIXTURE_RESOURCES_ROOT_DIR =
            TestCaseResourceUtils.requireClasspathDirectoryUrl(FIXTURES_RESOURCE);

    @TempDir
    Path temporaryDirectory;

    @Test
    void fixtureScenarioDirectories_numberingValidated_haveUniqueSequentialNumbersWithoutGaps() throws Exception {
        fixtureScenarioDirectoriesNumberingValidatedHaveUniqueSequentialNumbersWithoutGaps();
    }

    @ParameterizedTest(name = "[{index}] {0}/{1}")
    @MethodSource("fixtureInputFiles")
    void processFixtureInputFile_matchesExpectedAndCompileAfter(Path scenarioDir, Path srcFile) throws Exception {
        processFixtureInputFileMatchesExpectedAndCompileAfter(temporaryDirectory, scenarioDir, srcFile);
    }

    @NonNull
    private static Path resolveFixturesRoot(URL fixturesRootUrl) {
        try {
            return Path.of(fixturesRootUrl.toURI());
        } catch (URISyntaxException exception) {
            throw new IllegalArgumentException("Cannot convert fixtures URL to URI: " + fixturesRootUrl, exception);
        }
    }

    @Override
    @NonNull
    protected Optional<Path> findScenarioConfigPath(Path fixtureScenario) {
        return Optional.of(fixtureScenario.resolve(CONFIG_FILE));
    }

    @Override
    @NonNull
    protected Path getFixturesRoot() {
        return FIXTURES_ROOT;
    }

    @Override
    @NonNull
    protected String resolveDirectoryNamePrefix() {
        return "SrcProcessorE2E";
    }

    @Override
    protected void validateAfterProcessing(
            Path workingInputFile, Path compileAfterOutput, StrictValidationState validationState) throws Exception {
        JavaCompileTestUtils.CompileResult compileAfterResult =
                compileJavaSrcWithRelease21(workingInputFile, compileAfterOutput);
        assertThat(compileAfterResult.getExitCode())
                .as(
                        "Expected javac --release 21 to compile file %s. Diagnostics:%n%s",
                        workingInputFile, compileAfterResult.getOutput())
                .isZero();
        assertMainMethodExecutionSucceedsWhenPresent(workingInputFile, compileAfterOutput);
    }

    @Override
    @NonNull
    protected StrictValidationState validateBeforeProcessing(Path workingInputFile, Path compileBeforeOutput)
            throws Exception {
        JavaCompileTestUtils.CompileResult compileBeforeResult =
                compileJavaSrcWithRelease21(workingInputFile, compileBeforeOutput);
        assertThat(compileBeforeResult.getExitCode())
                .as(
                        "Expected javac --release 21 to compile file %s. Diagnostics:%n%s",
                        workingInputFile, compileBeforeResult.getOutput())
                .isZero();
        assertMainMethodExecutionSucceedsWhenPresent(workingInputFile, compileBeforeOutput);
        return StrictValidationState.INSTANCE;
    }

    enum StrictValidationState {
        INSTANCE
    }
}
