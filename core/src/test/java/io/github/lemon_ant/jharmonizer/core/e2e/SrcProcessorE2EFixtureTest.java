package io.github.lemon_ant.jharmonizer.core.e2e;

import static io.github.lemon_ant.jharmonizer.core.e2e.JavaCompileTestUtils.compileJavaSrcWithRelease21;
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

    private static final String FIXTURES_RESOURCE = "/test-cases/core/e2e/restructure/";
    private static final URL FIXTURE_RESOURCES_ROOT_DIR =
            TestCaseResourceUtils.requireClasspathDirectoryUrl(FIXTURES_RESOURCE);

    @NonNull
    private static final Path FIXTURES_ROOT = resolveFixturesRoot();

    private static final String CONFIG_FILE = "config.yml";

    @TempDir
    Path temporaryDirectory;

    @ParameterizedTest(name = "[{index}] {0}/{1}")
    @MethodSource("fixtureInputFiles")
    void processFixtureInputFile_matchesExpectedAndCompileAfter(Path scenarioDir, Path srcFile) throws Exception {
        processFixtureInputFileMatchesExpectedAndCompileAfter(temporaryDirectory, scenarioDir, srcFile);
    }

    @Test
    void fixtureScenarioDirectories_numberingValidated_haveUniqueSequentialNumbersWithoutGaps() throws Exception {
        fixtureScenarioDirectoriesNumberingValidatedHaveUniqueSequentialNumbersWithoutGaps();
    }

    @Override
    @NonNull
    protected Path getFixturesRoot() {
        return FIXTURES_ROOT;
    }

    @Override
    @NonNull
    protected Optional<Path> findScenarioConfigPath(Path fixtureScenario) {
        return Optional.of(fixtureScenario.resolve(CONFIG_FILE));
    }

    @Override
    @NonNull
    protected String resolveWorkspaceDirectoryName() {
        return "SrcProcessorE2E-working-dir";
    }

    @Override
    @NonNull
    protected String resolveCompileBeforeDirectoryName() {
        return "SrcProcessorE2E-compile-before";
    }

    @Override
    @NonNull
    protected String resolveCompileAfterDirectoryName() {
        return "SrcProcessorE2E-compile-after";
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
    protected boolean shouldCheckFailFastThrowForChangedFixture(StrictValidationState validationState) {
        return true;
    }

    @NonNull
    private static Path resolveFixturesRoot() {
        try {
            return Path.of(FIXTURE_RESOURCES_ROOT_DIR.toURI());
        } catch (URISyntaxException exception) {
            throw new IllegalArgumentException(
                    "Cannot convert fixtures URL to URI: " + FIXTURE_RESOURCES_ROOT_DIR, exception);
        }
    }

    enum StrictValidationState {
        INSTANCE
    }
}
