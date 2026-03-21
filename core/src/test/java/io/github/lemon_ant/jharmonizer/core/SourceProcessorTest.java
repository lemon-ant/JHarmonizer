package io.github.lemon_ant.jharmonizer.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import io.github.lemon_ant.jharmonizer.core.config.input.jharmonizer.JHarmonizerConfigurationManager;
import io.github.lemon_ant.jharmonizer.core.config.unified.FlexibleUnifiedConfig;
import io.github.lemon_ant.jharmonizer.core.flow.FlowType;
import io.github.lemon_ant.jharmonizer.core.testutils.TestCaseResourceUtils;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import lombok.NonNull;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.slf4j.LoggerFactory;

/**
 * Integration-like tests for SourceProcessor.processSources.
 * They work against a real temporary file system and exercise
 * the full flow: config → parser → sorter → formatter.
 */
class SourceProcessorTest {

    private static final Collection<String> INCLUDE_ALL_JAVA_FILES = Set.of();
    private static final Collection<String> EXCLUDE_NO_FILES = List.of();
    private static final URL SAMPLE_ALL_JAVA21_RESOURCE_URL = TestCaseResourceUtils.requireClasspathResourceUrl(
            "/test-cases/core/translator/valid/SampleAllJava21FeaturesList.java");
    private static final URL SPECIAL_JAVA_FIXTURES_ROOT_URL =
            TestCaseResourceUtils.requireClasspathDirectoryUrl("/test-cases/core/source-processor/special-java-files/");
    private static final String SOURCE_WITH_MULTIPLE_TOP_LEVEL_TYPES = """
            package demo;
            public class Sample {}
            interface Alpha {}
            """;
    private static final String PARTIAL_TOP_LEVEL_TYPES_CONFIG = """
            top-level-types-ordering:
              main-type-first: false
              type-groups:
                - [ interface ]
                - [ class ]
              ordering-rules: [ alpha ]
            """;

    @TempDir
    Path temporaryDirectory;

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("specialJavaFixtureDirectories")
    void processSources_specialJavaFixture_propagatesParserFailure(String fixtureDirectoryName) throws Exception {
        // Given
        Path projectDirectory = copyFixtureProject(fixtureDirectoryName);
        SourceProcessor sourceProcessor = new SourceProcessor();

        // When / Then
        assertThatThrownBy(() -> sourceProcessor.processSources(
                        projectDirectory, INCLUDE_ALL_JAVA_FILES, EXCLUDE_NO_FILES, FlowType.RESTRUCTURE))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("AllTypes cannot be empty");
    }

    @Test
    void processSources_singleJavaFile_restructureFlowRewritesFile() throws Exception {
        // Given
        String sampleSourceCode = TestCaseResourceUtils.readClasspathResourceAsString(SAMPLE_ALL_JAVA21_RESOURCE_URL);
        Path javaFilePath = writeJavaFile(temporaryDirectory, "SampleAllJava21FeaturesList.java", sampleSourceCode);
        String originalSourceCode = Files.readString(javaFilePath, StandardCharsets.UTF_8);
        SourceProcessor sourceProcessor = new SourceProcessor();

        // When
        sourceProcessor.processSources(
                temporaryDirectory, INCLUDE_ALL_JAVA_FILES, EXCLUDE_NO_FILES, FlowType.RESTRUCTURE);
        String processedSourceCode = Files.readString(javaFilePath, StandardCharsets.UTF_8);

        // Then
        assertThat(processedSourceCode).isNotBlank().isNotEqualTo(originalSourceCode);
    }

    @Test
    void processSources_multipleJavaFiles_onlyIncludedFilesAreProcessed() throws Exception {
        // Given
        String unformattedSourceCode = "package demo; public class Included {private int x;}";
        Path includedJavaFilePath = writeJavaFile(temporaryDirectory, "IncludedSample.java", unformattedSourceCode);
        Path excludedJavaFilePath = writeJavaFile(temporaryDirectory, "ExcludedSample.java", unformattedSourceCode);
        String includedOriginalSourceCode = Files.readString(includedJavaFilePath, StandardCharsets.UTF_8);
        String excludedOriginalSourceCode = Files.readString(excludedJavaFilePath, StandardCharsets.UTF_8);
        Collection<String> includeGlobs = Set.of("Included*.java");
        SourceProcessor sourceProcessor = new SourceProcessor();

        // When
        sourceProcessor.processSources(temporaryDirectory, includeGlobs, EXCLUDE_NO_FILES, FlowType.RESTRUCTURE);
        String includedProcessedSourceCode = Files.readString(includedJavaFilePath, StandardCharsets.UTF_8);
        String excludedProcessedSourceCode = Files.readString(excludedJavaFilePath, StandardCharsets.UTF_8);

        // Then
        assertThat(includedProcessedSourceCode)
                .as("Included file must be processed")
                .isNotEqualTo(includedOriginalSourceCode);
        assertThat(excludedProcessedSourceCode)
                .as("Excluded file must remain unchanged")
                .isEqualTo(excludedOriginalSourceCode);
    }

    @Test
    void processSources_alreadyRestructuredFile_checkFailFastFlowCompletesWithoutExceptions() throws Exception {
        // Given
        String sampleSourceCode = TestCaseResourceUtils.readClasspathResourceAsString(SAMPLE_ALL_JAVA21_RESOURCE_URL);
        Path javaFilePath = writeJavaFile(temporaryDirectory, "SampleAllJava21FeaturesList.java", sampleSourceCode);
        SourceProcessor sourceProcessor = new SourceProcessor();
        sourceProcessor.processSources(
                temporaryDirectory, INCLUDE_ALL_JAVA_FILES, EXCLUDE_NO_FILES, FlowType.RESTRUCTURE);

        // When / Then
        assertThatCode(() -> sourceProcessor.processSources(
                        temporaryDirectory, INCLUDE_ALL_JAVA_FILES, EXCLUDE_NO_FILES, FlowType.CHECK_FAIL_FAST))
                .doesNotThrowAnyException();
        String finalSourceCode = Files.readString(javaFilePath, StandardCharsets.UTF_8);
        assertThat(finalSourceCode).isNotBlank();
    }

    @Test
    void processSources_deepJavaFile_logsAbbreviatedJHarmonizerMessage() throws Exception {
        // Given
        Path nestedDirectoryPath = Files.createDirectories(
                temporaryDirectory.resolve(Path.of("feature", "deeplyNestedPackage", "nested", "internal", "tooling")));
        Path javaFilePath = writeJavaFile(
                nestedDirectoryPath,
                "InternalToolForLoggingVerification.java",
                "package demo; public class InternalToolForLoggingVerification {}");
        SourceProcessor sourceProcessor = new SourceProcessor();
        ListAppender<ILoggingEvent> listAppender = attachListAppender();

        // When
        try {
            sourceProcessor.processSources(
                    temporaryDirectory, INCLUDE_ALL_JAVA_FILES, EXCLUDE_NO_FILES, FlowType.RESTRUCTURE);
        } finally {
            detachListAppender(listAppender);
        }

        // Then
        String harmonizationLogMessage = listAppender.list.stream()
                .map(ILoggingEvent::getFormattedMessage)
                .filter(message -> message.startsWith("JHarmonizer "))
                .findFirst()
                .orElseThrow();
        assertThat(harmonizationLogMessage)
                .startsWith("JHarmonizer FORMATTED ")
                .contains("...")
                .contains("InternalToolForLoggingVerification.java")
                .doesNotContain("Harmonization finished for")
                .hasSizeLessThanOrEqualTo(120);
        assertThat(Files.readString(javaFilePath, StandardCharsets.UTF_8))
                .contains("public class InternalToolForLoggingVerification");
    }

    @Test
    void processSources_partialConfigFile_reordersUsingMergedDefaults() throws Exception {
        // Given
        Path javaFilePath = writeJavaFile(temporaryDirectory, "Sample.java", SOURCE_WITH_MULTIPLE_TOP_LEVEL_TYPES);
        Path customConfigFilePath = writeConfigFile(temporaryDirectory.resolve("custom-config.yml"));
        FlexibleUnifiedConfig externalConfig =
                JHarmonizerConfigurationManager.parseFlexibleUnifiedConfigFromFile(customConfigFilePath);
        SourceProcessor sourceProcessor = new SourceProcessor(externalConfig);

        // When
        sourceProcessor.processSources(
                temporaryDirectory, INCLUDE_ALL_JAVA_FILES, EXCLUDE_NO_FILES, FlowType.RESTRUCTURE);

        // Then
        String processedSourceCode = Files.readString(javaFilePath, StandardCharsets.UTF_8);
        assertThat(processedSourceCode.indexOf("interface Alpha"))
                .isLessThan(processedSourceCode.indexOf("class Sample"));
    }

    @NonNull
    private static Path writeJavaFile(Path baseDirectoryPath, String fileName, String fileContent) throws Exception {
        Path javaFilePath = baseDirectoryPath.resolve(fileName);
        return Files.writeString(javaFilePath, fileContent, StandardCharsets.UTF_8);
    }

    @NonNull
    private static Path writeConfigFile(Path configFilePath) throws Exception {
        return Files.writeString(configFilePath, PARTIAL_TOP_LEVEL_TYPES_CONFIG, StandardCharsets.UTF_8);
    }

    @NonNull
    private static ListAppender<ILoggingEvent> attachListAppender() {
        Logger logger = (Logger) LoggerFactory.getLogger(SourceProcessor.class);
        ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
        listAppender.start();
        logger.addAppender(listAppender);
        return listAppender;
    }

    private static void detachListAppender(ListAppender<ILoggingEvent> listAppender) {
        Logger logger = (Logger) LoggerFactory.getLogger(SourceProcessor.class);
        logger.detachAppender(listAppender);
        listAppender.stop();
    }

    @NonNull
    private static Stream<Arguments> specialJavaFixtureDirectories() {
        return Stream.of(
                Arguments.of("package-info-only"),
                Arguments.of("module-info-only"),
                Arguments.of("package-info-with-type"),
                Arguments.of("module-info-with-type"),
                Arguments.of("module-and-package-info-with-type"));
    }

    @NonNull
    private Path copyFixtureProject(String fixtureDirectoryName) throws IOException {
        Path srcPath = toPath(
                TestCaseResourceUtils.resolveRelativeUrl(SPECIAL_JAVA_FIXTURES_ROOT_URL, fixtureDirectoryName + "/"));
        Path targetProjectDirectory = temporaryDirectory.resolve(fixtureDirectoryName);
        Files.createDirectories(targetProjectDirectory);
        FileUtils.copyDirectory(srcPath.toFile(), targetProjectDirectory.toFile());
        return targetProjectDirectory;
    }

    @NonNull
    private static Path toPath(URL resourceUrl) {
        try {
            return Path.of(resourceUrl.toURI());
        } catch (URISyntaxException exception) {
            throw new IllegalArgumentException("Failed to convert test resource URL to URI: " + resourceUrl, exception);
        }
    }
}
