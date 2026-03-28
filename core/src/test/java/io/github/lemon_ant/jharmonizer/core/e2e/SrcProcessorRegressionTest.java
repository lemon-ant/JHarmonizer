package io.github.lemon_ant.jharmonizer.core.e2e;

import io.github.lemon_ant.jharmonizer.core.testutils.TestCaseResourceUtils;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.util.Optional;
import lombok.NonNull;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SrcProcessorRegressionTest extends AbstractSrcProcessorScenarioE2ETest {

    private static final String FIXTURES_RESOURCE = "/test-cases/core/e2e/regression/";
    private static final URL FIXTURE_RESOURCES_ROOT_DIR =
            TestCaseResourceUtils.requireClasspathDirectoryUrl(FIXTURES_RESOURCE);

    @NonNull
    private static final Path FIXTURES_ROOT = resolveFixturesRoot();

    @TempDir
    Path temporaryDirectory;

    @ParameterizedTest(name = "[{index}] {0}/{1}")
    @MethodSource("fixtureInputFiles")
    @Disabled
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

    @NonNull
    private static Path resolveFixturesRoot() {
        try {
            return Path.of(FIXTURE_RESOURCES_ROOT_DIR.toURI());
        } catch (URISyntaxException exception) {
            throw new IllegalArgumentException(
                    "Cannot convert fixtures URL to URI: " + FIXTURE_RESOURCES_ROOT_DIR, exception);
        }
    }
}
