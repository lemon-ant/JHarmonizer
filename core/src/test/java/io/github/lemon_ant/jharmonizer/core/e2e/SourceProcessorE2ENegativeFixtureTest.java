package io.github.lemon_ant.jharmonizer.core.e2e;

import static io.github.lemon_ant.jharmonizer.core.e2e.JavaCompileTestUtils.assertJavaSourcesCompileWithRelease21;
import static io.github.lemon_ant.jharmonizer.core.e2e.JavaRunMainTestUtils.assertJavaMainMethodsRunSuccessfully;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.lemon_ant.jharmonizer.core.SourceProcessor;
import io.github.lemon_ant.jharmonizer.core.config.input.jharmonizer.JHarmonizerConfigurationManager;
import io.github.lemon_ant.jharmonizer.core.config.unified.FlexibleUnifiedConfig;
import io.github.lemon_ant.jharmonizer.core.config.unified.UnifiedConfig;
import io.github.lemon_ant.jharmonizer.core.flow.FlowType;
import io.github.lemon_ant.jharmonizer.core.testutils.TestCaseResourceUtils;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SourceProcessorE2ENegativeFixtureTest {

    private static final String FIXTURES_RESOURCE = "/test-cases/core/e2e/restructure-negative/";
    private static final URL FIXTURE_RESOURCES_ROOT_DIR =
            TestCaseResourceUtils.requireClasspathDirectoryUrl(FIXTURES_RESOURCE);
    private static final String CONFIG_FILE = "config.yml";

    @TempDir
    Path temporaryDirectory;

    @Test
    void processExplicitTypeInstanceReferrerScenario_restructureBreaksRuntimeSemantics() throws Exception {
        Path fixturesRoot = resolveFixturesRoot();
        Path scenario = fixturesRoot.resolve("01-explicit-type-instance-referrer-missing-dependency");
        Path workingRoot = temporaryDirectory.resolve("working");
        Path compileBeforeOutput = temporaryDirectory.resolve("compile-before");
        Path compileAfterOutput = temporaryDirectory.resolve("compile-after");

        Path sourceInput = scenario.resolve("input/ExplicitTypeInstanceReferrerMissingDependencySample.java");
        Path workingSource =
                workingRoot.resolve("ExplicitTypeInstanceReferrerMissingDependencySample.java");
        Files.createDirectories(workingRoot);
        Files.copy(sourceInput, workingSource);

        assertJavaSourcesCompileWithRelease21(workingRoot, compileBeforeOutput);
        assertJavaMainMethodsRunSuccessfully(workingRoot, compileBeforeOutput);

        runProcessor(workingRoot, scenario.resolve(CONFIG_FILE), FlowType.RESTRUCTURE);

        assertJavaSourcesCompileWithRelease21(workingRoot, compileAfterOutput);
        assertThatThrownBy(() -> assertJavaMainMethodsRunSuccessfully(workingRoot, compileAfterOutput))
                .isInstanceOf(AssertionError.class);
    }

    private static Path resolveFixturesRoot() {
        try {
            return Path.of(FIXTURE_RESOURCES_ROOT_DIR.toURI());
        } catch (URISyntaxException exception) {
            throw new IllegalArgumentException(
                    "Cannot convert fixtures URL to URI: " + FIXTURE_RESOURCES_ROOT_DIR, exception);
        }
    }

    private static void runProcessor(Path sourcesRoot, Path config, FlowType flowType) {
        UnifiedConfig unifiedConfig =
                JHarmonizerConfigurationManager.parseUnifiedConfigFromClasspathResource(toUrl(config));
        FlexibleUnifiedConfig flexibleConfig = new FlexibleUnifiedConfig(
                unifiedConfig.getTopLevelTypesOrdering(),
                unifiedConfig.getFormatting(),
                unifiedConfig.isBackupsEnabled(),
                unifiedConfig.getHeaderLine(),
                unifiedConfig.getRootMemberGroups());
        SourceProcessor sourceProcessor = new SourceProcessor(flexibleConfig);
        sourceProcessor.processSources(sourcesRoot, List.of(), List.of(), flowType);
    }

    private static URL toUrl(Path path) {
        try {
            return path.toUri().toURL();
        } catch (MalformedURLException exception) {
            throw new IllegalArgumentException("Cannot convert path to URL: " + path, exception);
        }
    }
}
