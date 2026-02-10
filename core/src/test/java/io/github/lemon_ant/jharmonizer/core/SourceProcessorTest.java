package io.github.lemon_ant.jharmonizer.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import io.github.lemon_ant.jharmonizer.core.flow.FlowType;
import io.github.lemon_ant.jharmonizer.core.testutils.TestCaseResourceUtils;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Integration-like tests for SourceProcessor.processSources.
 * They work against a real temporary file system and exercise
 * the full flow: config → parser → sorter → formatter.
 */
class SourceProcessorTest {

    private static final Collection<String> INCLUDE_ALL_JAVA_FILES = Set.of();
    private static final Collection<String> EXCLUDE_NO_FILES = List.of();

    private static final URL SAMPLE_ALL_JAVA21_RESOURCE_URL = SourceProcessorTest.class.getResource(
            "/test-cases/core/translator/valid/SampleAllJava21FeaturesList.java");

    @TempDir
    Path temporaryDirectory;

    private static String loadSampleAllJava21FeaturesSource() throws Exception {
        return TestCaseResourceUtils.readClasspathResourceAsString(SAMPLE_ALL_JAVA21_RESOURCE_URL);
    }

    private static Path writeJavaFile(Path baseDirectoryPath, String fileName, String fileContent) throws Exception {
        Path javaFilePath = baseDirectoryPath.resolve(fileName);
        return Files.writeString(javaFilePath, fileContent, StandardCharsets.UTF_8);
    }

    @Test
    void processSources_singleJavaFile_restructureFlowRewritesFile() throws Exception {
        // Given
        String sampleSourceCode = loadSampleAllJava21FeaturesSource();
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
        String sampleSourceCode = loadSampleAllJava21FeaturesSource();
        Path javaFilePath = writeJavaFile(temporaryDirectory, "SampleAllJava21FeaturesList.java", sampleSourceCode);
        SourceProcessor sourceProcessor = new SourceProcessor();
        // First bring the file into a fully restructured and formatted state
        sourceProcessor.processSources(
                temporaryDirectory, INCLUDE_ALL_JAVA_FILES, EXCLUDE_NO_FILES, FlowType.RESTRUCTURE);

        // When / Then
        assertThatCode(() -> sourceProcessor.processSources(
                        temporaryDirectory, INCLUDE_ALL_JAVA_FILES, EXCLUDE_NO_FILES, FlowType.CHECK_FAIL_FAST))
                .doesNotThrowAnyException();
        // sanity check that the file is still readable and not empty
        String finalSourceCode = Files.readString(javaFilePath, StandardCharsets.UTF_8);
        assertThat(finalSourceCode).isNotBlank();
    }
}
