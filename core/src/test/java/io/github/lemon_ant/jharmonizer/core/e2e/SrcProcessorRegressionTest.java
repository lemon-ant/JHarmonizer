package io.github.lemon_ant.jharmonizer.core.e2e;

import static io.github.lemon_ant.jharmonizer.core.e2e.JavaCompileTestUtils.compileJavaSrcWithRelease21;
import static io.github.lemon_ant.jharmonizer.core.e2e.JavaRunMainTestUtils.runJavaMainMethod;
import static org.assertj.core.api.Assertions.assertThat;

import io.github.lemon_ant.jharmonizer.core.testutils.TestCaseResourceUtils;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.util.Optional;
import lombok.NonNull;
import lombok.Value;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SrcProcessorRegressionTest
        extends AbstractSrcProcessorScenarioE2ETest<SrcProcessorRegressionTest.CompileAndRunSnapshot> {

    private static final String FIXTURES_RESOURCE = "/test-cases/core/e2e/regression/";
    private static final URL FIXTURE_RESOURCES_ROOT_DIR =
            TestCaseResourceUtils.requireClasspathDirectoryUrl(FIXTURES_RESOURCE);

    @NonNull
    private static final Path FIXTURES_ROOT = resolveFixturesRoot();

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
        Path scenarioConfigPath = fixtureScenario.resolve("config.yml");
        return java.nio.file.Files.exists(scenarioConfigPath) ? Optional.of(scenarioConfigPath) : Optional.empty();
    }

    @Override
    @NonNull
    protected String resolveWorkspaceDirectoryName() {
        return "SrcProcessorRegressionE2E-working-dir";
    }

    @Override
    @NonNull
    protected String resolveCompileBeforeDirectoryName() {
        return "SrcProcessorRegressionE2E-compile-before";
    }

    @Override
    @NonNull
    protected String resolveCompileAfterDirectoryName() {
        return "SrcProcessorRegressionE2E-compile-after";
    }

    @Override
    @NonNull
    protected CompileAndRunSnapshot validateBeforeProcessing(Path workingInputFile, Path compileBeforeOutput) {
        return captureCompileAndRunSnapshot(workingInputFile, compileBeforeOutput);
    }

    @Override
    protected void validateAfterProcessing(
            Path workingInputFile, Path compileAfterOutput, CompileAndRunSnapshot beforeSnapshot) {
        CompileAndRunSnapshot afterSnapshot = captureCompileAndRunSnapshot(workingInputFile, compileAfterOutput);
        assertRelaxedCompileAndRunConsistency(beforeSnapshot, afterSnapshot, workingInputFile);
    }

    @Override
    protected boolean shouldCheckFailFastThrowForChangedFixture(CompileAndRunSnapshot validationState) {
        return validationState.isCompiled();
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

    @NonNull
    private static CompileAndRunSnapshot captureCompileAndRunSnapshot(Path srcFile, Path compileOutputDirectory) {
        JavaCompileTestUtils.CompileResult compileResult;
        try {
            compileResult = compileJavaSrcWithRelease21(srcFile, compileOutputDirectory);
        } catch (IOException | InterruptedException exception) {
            return CompileAndRunSnapshot.compileFailed();
        }
        if (compileResult.getExitCode() != 0) {
            return CompileAndRunSnapshot.compileFailed();
        }

        if (!containsMainMethodDeclaration(srcFile)) {
            return CompileAndRunSnapshot.compiledWithoutMain();
        }

        try {
            JavaRunMainTestUtils.RunResult runResult = runJavaMainMethod(srcFile, compileOutputDirectory);
            return CompileAndRunSnapshot.compiledWithMain(runResult.getExitCode());
        } catch (IOException | InterruptedException exception) {
            return CompileAndRunSnapshot.compiledWithMainExecutionFailed();
        }
    }

    private static void assertRelaxedCompileAndRunConsistency(
            CompileAndRunSnapshot beforeSnapshot, CompileAndRunSnapshot afterSnapshot, Path srcFile) {
        if (!beforeSnapshot.isCompiled()) {
            return;
        }

        assertThat(afterSnapshot.isCompiled())
                .as("Expected processed source to remain compilable because original source compiled: %s", srcFile)
                .isTrue();

        if (!beforeSnapshot.isMainExecuted()) {
            return;
        }

        assertThat(afterSnapshot.isMainExecuted())
                .as(
                        "Expected processed source main method to execute because original source main executed: %s",
                        srcFile)
                .isTrue();
        assertThat(afterSnapshot.getMainExitCode())
                .as("Expected processed source main method exit code to match original source: %s", srcFile)
                .isEqualTo(beforeSnapshot.getMainExitCode());
    }

    @Value
    static class CompileAndRunSnapshot {
        boolean compiled;
        boolean mainExecuted;
        int mainExitCode;

        @NonNull
        private static CompileAndRunSnapshot compileFailed() {
            return new CompileAndRunSnapshot(false, false, Integer.MIN_VALUE);
        }

        @NonNull
        private static CompileAndRunSnapshot compiledWithoutMain() {
            return new CompileAndRunSnapshot(true, false, Integer.MIN_VALUE);
        }

        @NonNull
        private static CompileAndRunSnapshot compiledWithMain(int mainExitCode) {
            return new CompileAndRunSnapshot(true, true, mainExitCode);
        }

        @NonNull
        private static CompileAndRunSnapshot compiledWithMainExecutionFailed() {
            return new CompileAndRunSnapshot(true, false, Integer.MIN_VALUE);
        }
    }
}
