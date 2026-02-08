package io.github.lemon_ant.jharmonizer.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import io.github.lemon_ant.jharmonizer.core.flow.FlowType;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Integration-like tests for SourceProcessor.processSources.
 * They work against a real temporary file system and exercise
 * the full flow: config → translator → sorter → formatter.
 */
class SourceProcessorTest {

    private static final Collection<String> INCLUDE_ALL_JAVA_FILES = Set.of();
    private static final Collection<String> EXCLUDE_NO_FILES = List.of();

    private static final String SAMPLE_ALL_JAVA21_RESOURCE_PATH =
            "test-cases/core/translator/valid/SampleAllJava21FeaturesList.java";

    @TempDir
    Path temporaryDirectory;

    private static String readClasspathResourceAsString(String classpathResourcePath) throws Exception {
        try (InputStream inputStream = Objects.requireNonNull(
                SourceProcessorTest.class.getClassLoader().getResourceAsStream(classpathResourcePath),
                "Missing test resource: " + classpathResourcePath)) {
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static Path writeJavaFile(Path baseDirectoryPath, String fileName, String fileContent) throws Exception {
        Path javaFilePath = baseDirectoryPath.resolve(fileName);
        return Files.writeString(javaFilePath, fileContent, StandardCharsets.UTF_8);
    }

    @Test
    void processSources_whenSingleJavaFileRestructureFlow_shouldRewriteFile() throws Exception {
        // Given
        String sampleSourceCode = readClasspathResourceAsString(SAMPLE_ALL_JAVA21_RESOURCE_PATH);
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
    void processSources_whenIncludeGlobsUsed_shouldProcessOnlyIncludedFiles() throws Exception {
        // Given
        String includedUnformattedSourceCode = "public class IncludedSample{private int x;}";
        String excludedUnformattedSourceCode = "public class ExcludedSample{private int x;}";
        Path includedJavaFilePath =
                writeJavaFile(temporaryDirectory, "IncludedSample.java", includedUnformattedSourceCode);
        Path excludedJavaFilePath =
                writeJavaFile(temporaryDirectory, "ExcludedSample.java", excludedUnformattedSourceCode);
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
    void processSources_whenAlreadyRestructured_shouldNotThrowInCheckFailFastFlow() throws Exception {
        // Given
        String sampleSourceCode = readClasspathResourceAsString(SAMPLE_ALL_JAVA21_RESOURCE_PATH);
        Path javaFilePath = writeJavaFile(temporaryDirectory, "SampleAllJava21FeaturesList.java", sampleSourceCode);
        SourceProcessor sourceProcessor = new SourceProcessor();
        sourceProcessor.processSources(
                temporaryDirectory, INCLUDE_ALL_JAVA_FILES, EXCLUDE_NO_FILES, FlowType.RESTRUCTURE);

        // When
        ThrowingCallable checkFailFastInvocation = () -> sourceProcessor.processSources(
                temporaryDirectory, INCLUDE_ALL_JAVA_FILES, EXCLUDE_NO_FILES, FlowType.CHECK_FAIL_FAST);

        // Then
        assertThatCode(checkFailFastInvocation).doesNotThrowAnyException();
        String finalSourceCode = Files.readString(javaFilePath, StandardCharsets.UTF_8);
        assertThat(finalSourceCode).isNotBlank();
    }
}
