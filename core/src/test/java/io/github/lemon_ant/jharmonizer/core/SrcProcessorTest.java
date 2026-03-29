package io.github.lemon_ant.jharmonizer.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import io.github.lemon_ant.jharmonizer.core.config.input.jharmonizer.JHarmonizerConfigurationManager;
import io.github.lemon_ant.jharmonizer.core.config.unified.FlexibleUnifiedConfig;
import io.github.lemon_ant.jharmonizer.core.config.unified.UnifiedConfigMerger;
import io.github.lemon_ant.jharmonizer.core.files_handler.SrcFilesHandler;
import io.github.lemon_ant.jharmonizer.core.flow.FlowType;
import io.github.lemon_ant.jharmonizer.core.processing_stat.FileProcessingStatistic;
import io.github.lemon_ant.jharmonizer.core.processing_stat.SrcProcessingStats.AggregatedProcessingStatistic;
import io.github.lemon_ant.jharmonizer.core.testutils.TestCaseResourceUtils;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.NonNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.LoggerFactory;

/**
 * Integration-like tests for SrcProcessor.processSources.
 * They work against a real temporary file system and exercise
 * the full flow: config → parser → sorter → formatter.
 */
class SrcProcessorTest {

    private static final Collection<String> INCLUDE_ALL_JAVA_FILES = Set.of();
    private static final Collection<String> EXCLUDE_NO_FILES = List.of();
    private static final URL SAMPLE_ALL_JAVA21_RESOURCE_URL = TestCaseResourceUtils.requireClasspathResourceUrl(
            "/test-cases/core/translator/valid/SampleAllJava21FeaturesList.java");
    private static final String FLOW_LEVEL_FIXTURES_RESOURCE = "/test-cases/core/source-processor/flow-level/";
    private static final URL FLOW_LEVEL_FIXTURES_ROOT_URL =
            TestCaseResourceUtils.requireClasspathDirectoryUrl(FLOW_LEVEL_FIXTURES_RESOURCE);
    private static final Path FLOW_LEVEL_FIXTURES_ROOT = resolveFlowLevelFixturesRoot();
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

    @Test
    void processSources_singleJavaFile_restructureFlowRewritesFile() throws Exception {
        // Given
        String sampleSrcCode = TestCaseResourceUtils.readClasspathResourceAsString(SAMPLE_ALL_JAVA21_RESOURCE_URL);
        Path javaFilePath = writeJavaFile(temporaryDirectory, "SampleAllJava21FeaturesList.java", sampleSrcCode);
        String originalSrcCode = Files.readString(javaFilePath, StandardCharsets.UTF_8);
        SrcProcessor srcProcessor = new SrcProcessor();

        // When
        srcProcessor.processSources(temporaryDirectory, INCLUDE_ALL_JAVA_FILES, EXCLUDE_NO_FILES, FlowType.RESTRUCTURE);
        String processedSrcCode = Files.readString(javaFilePath, StandardCharsets.UTF_8);

        // Then
        assertThat(processedSrcCode).isNotBlank().isNotEqualTo(originalSrcCode);
    }

    @Test
    void processSources_multipleJavaFiles_onlyIncludedFilesAreProcessed() throws Exception {
        // Given
        String unformattedSrcCode = "package demo; public class Included {private int x;}";
        Path includedJavaFilePath = writeJavaFile(temporaryDirectory, "IncludedSample.java", unformattedSrcCode);
        Path excludedJavaFilePath = writeJavaFile(temporaryDirectory, "ExcludedSample.java", unformattedSrcCode);
        String includedOriginalSrcCode = Files.readString(includedJavaFilePath, StandardCharsets.UTF_8);
        String excludedOriginalSrcCode = Files.readString(excludedJavaFilePath, StandardCharsets.UTF_8);
        Collection<String> includeGlobs = Set.of("Included*.java");
        SrcProcessor srcProcessor = new SrcProcessor();

        // When
        srcProcessor.processSources(temporaryDirectory, includeGlobs, EXCLUDE_NO_FILES, FlowType.RESTRUCTURE);
        String includedProcessedSrcCode = Files.readString(includedJavaFilePath, StandardCharsets.UTF_8);
        String excludedProcessedSrcCode = Files.readString(excludedJavaFilePath, StandardCharsets.UTF_8);

        // Then
        assertThat(includedProcessedSrcCode)
                .as("Included file must be processed")
                .isNotEqualTo(includedOriginalSrcCode);
        assertThat(excludedProcessedSrcCode)
                .as("Excluded file must remain unchanged")
                .isEqualTo(excludedOriginalSrcCode);
    }

    @Test
    void processSources_alreadyRestructuredFile_checkFailFastFlowCompletesWithoutExceptions() throws Exception {
        // Given
        String sampleSrcCode = TestCaseResourceUtils.readClasspathResourceAsString(SAMPLE_ALL_JAVA21_RESOURCE_URL);
        Path javaFilePath = writeJavaFile(temporaryDirectory, "SampleAllJava21FeaturesList.java", sampleSrcCode);
        SrcProcessor srcProcessor = new SrcProcessor();
        srcProcessor.processSources(temporaryDirectory, INCLUDE_ALL_JAVA_FILES, EXCLUDE_NO_FILES, FlowType.RESTRUCTURE);

        // When / Then
        assertThatCode(() -> srcProcessor.processSources(
                        temporaryDirectory, INCLUDE_ALL_JAVA_FILES, EXCLUDE_NO_FILES, FlowType.CHECK_FAIL_FAST))
                .doesNotThrowAnyException();
        String finalSrcCode = Files.readString(javaFilePath, StandardCharsets.UTF_8);
        assertThat(finalSrcCode).isNotBlank();
    }

    @Test
    void processSources_checkAllWithViolations_aggregatesStatsAndContinuesOtherFiles() throws Exception {
        // Given
        Path scenarioRoot = copyScenarioInputToWorkingDirectory(temporaryDirectory, "check-all");
        Map<String, String> expectedSources = readScenarioExpectedSources("check-all");
        List<String> orderedInputFiles = List.of("A_Checked.java", "B_Reordered.java", "C_Formatted.java");
        SrcProcessor srcProcessor = new SrcProcessor();
        ListAppender<ILoggingEvent> listAppender = attachListAppender();

        // When
        AggregatedProcessingStatistic aggregatedProcessingStatistic;
        try {
            aggregatedProcessingStatistic =
                    srcProcessor.processSources(scenarioRoot, orderedInputFiles, EXCLUDE_NO_FILES, FlowType.CHECK_ALL);
        } finally {
            detachListAppender(listAppender);
        }
        String logs = collectLogMessages(listAppender);

        // Then
        assertThat(logs).contains("A_Checked.java");
        assertThat(logs).contains("JHarmonizer FORMATTED");
        assertThat(logs).contains("B_Reordered.java");
        assertThat(logs).contains("JHarmonizer REORDERED");
        assertThat(logs).contains("C_Formatted.java");
        assertThat(logs).contains("JHarmonizer FORMATTED");
        assertThat(aggregatedProcessingStatistic.getFileCount()).isEqualTo(3);
        assertThat(aggregatedProcessingStatistic.getTotalSize())
                .isEqualTo(expectedSources.values().stream()
                        .mapToLong(String::length)
                        .sum());
        assertThat(aggregatedProcessingStatistic.getTotalProcessingTimeNanos()).isPositive();
        assertThat(aggregatedProcessingStatistic.getSmallestFile())
                .extracting(FileProcessingStatistic::getPath)
                .isEqualTo(scenarioRoot.resolve("A_Checked.java"));
        assertThat(aggregatedProcessingStatistic.getLargestFile())
                .extracting(FileProcessingStatistic::getPath)
                .isEqualTo(scenarioRoot.resolve("B_Reordered.java"));
        assertScenarioMatchesExpectedSources(scenarioRoot, "check-all");
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
        SrcProcessor srcProcessor = new SrcProcessor();
        ListAppender<ILoggingEvent> listAppender = attachListAppender();

        // When
        try {
            srcProcessor.processSources(
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
    void processSources_fullyOffFile_logsSkippedStatus() throws Exception {
        // Given
        String fullyOffSrcCode = "// @jharmonizer:fully-off\npackage demo; public class FullyOffSample {}";
        Path javaFilePath = writeJavaFile(temporaryDirectory, "FullyOffSample.java", fullyOffSrcCode);
        String originalSrcCode = Files.readString(javaFilePath, StandardCharsets.UTF_8);
        SrcProcessor srcProcessor = new SrcProcessor();
        ListAppender<ILoggingEvent> listAppender = attachListAppender();

        // When
        try {
            srcProcessor.processSources(
                    temporaryDirectory, INCLUDE_ALL_JAVA_FILES, EXCLUDE_NO_FILES, FlowType.RESTRUCTURE);
        } finally {
            detachListAppender(listAppender);
        }
        String logs = collectLogMessages(listAppender);

        // Then
        assertThat(logs).contains("FullyOffSample.java");
        assertThat(logs).contains("JHarmonizer SKIPPED");
        assertThat(Files.readString(javaFilePath, StandardCharsets.UTF_8)).isEqualTo(originalSrcCode);
    }

    @Test
    void processSources_partialConfigFile_reordersUsingMergedDefaults() throws Exception {
        // Given
        Path javaFilePath = writeJavaFile(temporaryDirectory, "Sample.java", SOURCE_WITH_MULTIPLE_TOP_LEVEL_TYPES);
        Path customConfigFilePath = writeConfigFile(temporaryDirectory.resolve("custom-config.yml"));
        FlexibleUnifiedConfig externalConfig =
                JHarmonizerConfigurationManager.parseFlexibleUnifiedConfigFromFile(customConfigFilePath);
        SrcProcessor srcProcessor = new SrcProcessor(externalConfig);

        // When
        srcProcessor.processSources(temporaryDirectory, INCLUDE_ALL_JAVA_FILES, EXCLUDE_NO_FILES, FlowType.RESTRUCTURE);

        // Then
        String processedSrcCode = Files.readString(javaFilePath, StandardCharsets.UTF_8);
        assertThat(processedSrcCode.indexOf("interface Alpha")).isLessThan(processedSrcCode.indexOf("class Sample"));
    }

    @Test
    void processSources_restructureWithBackupsEnabled_createsBackupNextToSrc() throws Exception {
        // Given
        String unformattedSrcCode = "package demo; public class BackupEnabled {private int x;}";
        Path javaFilePath = writeJavaFile(temporaryDirectory, "BackupEnabled.java", unformattedSrcCode);
        String originalSrcCode = Files.readString(javaFilePath, StandardCharsets.UTF_8);
        SrcProcessor srcProcessor = new SrcProcessor(new FlexibleUnifiedConfig(null, null, true, null, null));

        // When
        srcProcessor.processSources(temporaryDirectory, INCLUDE_ALL_JAVA_FILES, EXCLUDE_NO_FILES, FlowType.RESTRUCTURE);

        // Then
        Path backupFilePath =
                javaFilePath.resolveSibling(javaFilePath.getFileName().toString() + ".bak");
        assertThat(backupFilePath).exists();
        assertThat(Files.readString(backupFilePath, StandardCharsets.UTF_8)).isEqualTo(originalSrcCode);
        assertThat(Files.readString(javaFilePath, StandardCharsets.UTF_8)).isNotEqualTo(originalSrcCode);
    }

    @Test
    void processSources_secondRunWithBackupsEnabled_replacesExistingBackup() throws Exception {
        // Given
        String firstSrcCode = "package demo; public class BackupTwice {private int x;}";
        String secondSrcCode = "package demo; public class BackupTwice {private int y;}";
        Path javaFilePath = writeJavaFile(temporaryDirectory, "BackupTwice.java", firstSrcCode);
        SrcProcessor srcProcessor = new SrcProcessor(new FlexibleUnifiedConfig(null, null, true, null, null));

        // When
        srcProcessor.processSources(temporaryDirectory, INCLUDE_ALL_JAVA_FILES, EXCLUDE_NO_FILES, FlowType.RESTRUCTURE);
        SrcFilesHandler.overwrite(javaFilePath, secondSrcCode);
        srcProcessor.processSources(temporaryDirectory, INCLUDE_ALL_JAVA_FILES, EXCLUDE_NO_FILES, FlowType.RESTRUCTURE);

        // Then
        Path backupFilePath =
                javaFilePath.resolveSibling(javaFilePath.getFileName().toString() + ".bak");
        assertThat(backupFilePath).exists();
        assertThat(Files.readString(backupFilePath, StandardCharsets.UTF_8)).isEqualTo(secondSrcCode);
    }

    @Test
    void processSources_restructureWithBackupsDisabled_doesNotCreateBackup() throws Exception {
        // Given
        String unformattedSrcCode = "package demo; public class BackupDisabled {private int x;}";
        Path javaFilePath = writeJavaFile(temporaryDirectory, "BackupDisabled.java", unformattedSrcCode);
        String originalSrcCode = Files.readString(javaFilePath, StandardCharsets.UTF_8);
        SrcProcessor srcProcessor = new SrcProcessor(new FlexibleUnifiedConfig(null, null, false, null, null));

        // When
        srcProcessor.processSources(temporaryDirectory, INCLUDE_ALL_JAVA_FILES, EXCLUDE_NO_FILES, FlowType.RESTRUCTURE);

        // Then
        Path backupFilePath =
                javaFilePath.resolveSibling(javaFilePath.getFileName().toString() + ".bak");
        assertThat(backupFilePath).doesNotExist();
        assertThat(Files.readString(javaFilePath, StandardCharsets.UTF_8)).isNotEqualTo(originalSrcCode);
    }

    @Test
    void processSources_restructureWithNoBackupOverride_doesNotCreateBackup() throws Exception {
        // Given
        String unformattedSrcCode = "package demo; public class BackupOverrideDisabled {private int x;}";
        Path javaFilePath = writeJavaFile(temporaryDirectory, "BackupOverrideDisabled.java", unformattedSrcCode);
        String originalSrcCode = Files.readString(javaFilePath, StandardCharsets.UTF_8);
        FlexibleUnifiedConfig effectiveConfig = UnifiedConfigMerger.merge(
                new FlexibleUnifiedConfig(null, null, true, null, null),
                new FlexibleUnifiedConfig(null, null, false, null, null));
        SrcProcessor srcProcessor = new SrcProcessor(effectiveConfig);

        // When
        srcProcessor.processSources(temporaryDirectory, INCLUDE_ALL_JAVA_FILES, EXCLUDE_NO_FILES, FlowType.RESTRUCTURE);

        // Then
        Path backupFilePath =
                javaFilePath.resolveSibling(javaFilePath.getFileName().toString() + ".bak");
        assertThat(backupFilePath).doesNotExist();
        assertThat(Files.readString(javaFilePath, StandardCharsets.UTF_8)).isNotEqualTo(originalSrcCode);
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
        Logger logger = (Logger) LoggerFactory.getLogger(SrcProcessor.class);
        ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
        listAppender.start();
        logger.addAppender(listAppender);
        return listAppender;
    }

    private static void detachListAppender(ListAppender<ILoggingEvent> listAppender) {
        Logger logger = (Logger) LoggerFactory.getLogger(SrcProcessor.class);
        logger.detachAppender(listAppender);
        listAppender.stop();
    }

    @NonNull
    private static String collectLogMessages(ListAppender<ILoggingEvent> listAppender) {
        return listAppender.list.stream()
                .map(ILoggingEvent::getFormattedMessage)
                .collect(java.util.stream.Collectors.joining("\n"));
    }

    @NonNull
    private static Path copyScenarioInputToWorkingDirectory(Path temporaryRoot, String scenarioName) {
        Path srcInputDirectory = FLOW_LEVEL_FIXTURES_ROOT.resolve(scenarioName).resolve("input");
        Path workingDirectory = temporaryRoot.resolve("flow-level").resolve(scenarioName);
        try (Stream<Path> srcFiles = Files.list(srcInputDirectory)) {
            Files.createDirectories(workingDirectory);
            srcFiles.filter(Files::isRegularFile).forEach(srcFile -> {
                Path targetFile = workingDirectory.resolve(srcFile.getFileName());
                try {
                    Files.copy(srcFile, targetFile, StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException exception) {
                    throw new IllegalStateException("Failed to copy fixture file: " + srcFile, exception);
                }
            });
            return workingDirectory;
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to prepare scenario fixtures: " + scenarioName, exception);
        }
    }

    private static void assertScenarioMatchesExpectedSources(Path workingDirectory, String scenarioName) {
        Map<String, String> expectedSources = readScenarioExpectedSources(scenarioName);
        Map<String, String> actualSources = readDirectoryJavaSources(workingDirectory);
        assertThat(actualSources).isEqualTo(expectedSources);
    }

    @NonNull
    private static Map<String, String> readScenarioExpectedSources(String scenarioName) {
        Path expectedDirectory = FLOW_LEVEL_FIXTURES_ROOT.resolve(scenarioName).resolve("expected");
        return readDirectoryJavaSources(expectedDirectory);
    }

    @NonNull
    private static Map<String, String> readDirectoryJavaSources(Path directory) {
        try (Stream<Path> files = Files.list(directory)) {
            return files.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith(".java"))
                    .sorted(Comparator.comparing(path -> path.getFileName().toString()))
                    .collect(Collectors.toMap(
                            path -> path.getFileName().toString(),
                            SrcProcessorTest::readSrcFile,
                            (left, right) -> left,
                            java.util.LinkedHashMap::new));
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to read fixture directory: " + directory, exception);
        }
    }

    @NonNull
    private static String readSrcFile(Path srcFile) {
        try {
            return Files.readString(srcFile, StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to read fixture source file: " + srcFile, exception);
        }
    }

    @NonNull
    private static Path resolveFlowLevelFixturesRoot() {
        try {
            return Path.of(FLOW_LEVEL_FIXTURES_ROOT_URL.toURI());
        } catch (URISyntaxException exception) {
            throw new IllegalStateException(
                    "Failed to resolve flow-level fixture root URL: " + FLOW_LEVEL_FIXTURES_ROOT_URL, exception);
        }
    }
}
