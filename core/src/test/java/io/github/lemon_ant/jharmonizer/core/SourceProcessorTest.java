package io.github.lemon_ant.jharmonizer.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import io.github.lemon_ant.jharmonizer.core.flow.FlowType;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Integration-like tests for SourceProcessor.processSources.
 * They work against a real temporary file system and exercise
 * the full flow: config → parser → sorter → formatter.
 */
class SourceProcessorTest {

    private static final Collection<String> ALL_JAVA_GLOBS = Set.of();
    private static final Collection<String> NO_EXCLUDES = List.of();

    private static final String SAMPLE_ALL_JAVA21_RESOURCE_PATH =
            "test-cases/core/parser/SampleAllJava21FeaturesList.java";

    @TempDir
    Path temporaryDirectory;

    @Test
    void processSources_singleJavaFile_restructureFlowRewritesFile() throws Exception {
        // given
        String sampleSourceCode = loadSampleAllJava21FeaturesSource();
        Path javaFilePath = writeJavaFile(temporaryDirectory, "SampleAllJava21FeaturesList.java", sampleSourceCode);

        String originalSourceCode = Files.readString(javaFilePath, StandardCharsets.UTF_8);
        SourceProcessor sourceProcessor = new SourceProcessor();

        // when
        sourceProcessor.processSources(temporaryDirectory, ALL_JAVA_GLOBS, NO_EXCLUDES, FlowType.RESTRUCTURE);

        String processedSourceCode = Files.readString(javaFilePath, StandardCharsets.UTF_8);

        // then
        assertThat(processedSourceCode).isNotBlank().isNotEqualTo(originalSourceCode);
    }

    @Test
    void processSources_multipleJavaFiles_onlyIncludedFilesAreProcessed() throws Exception {
        // given
        String unformattedSourceCode = "package demo; public class Included {private int x;}";
        Path includedJavaFilePath = writeJavaFile(temporaryDirectory, "IncludedSample.java", unformattedSourceCode);
        Path excludedJavaFilePath = writeJavaFile(temporaryDirectory, "ExcludedSample.java", unformattedSourceCode);

        String includedOriginalSourceCode = Files.readString(includedJavaFilePath, StandardCharsets.UTF_8);
        String excludedOriginalSourceCode = Files.readString(excludedJavaFilePath, StandardCharsets.UTF_8);

        Collection<String> includeGlobs = Set.of("Included*.java");
        Collection<String> excludeGlobs = NO_EXCLUDES;

        SourceProcessor sourceProcessor = new SourceProcessor();

        // when
        sourceProcessor.processSources(temporaryDirectory, includeGlobs, excludeGlobs, FlowType.RESTRUCTURE);

        String includedProcessedSourceCode = Files.readString(includedJavaFilePath, StandardCharsets.UTF_8);
        String excludedProcessedSourceCode = Files.readString(excludedJavaFilePath, StandardCharsets.UTF_8);

        // then
        assertThat(includedProcessedSourceCode)
                .as("Included file must be processed")
                .isNotEqualTo(includedOriginalSourceCode);

        assertThat(excludedProcessedSourceCode)
                .as("Excluded file must remain unchanged")
                .isEqualTo(excludedOriginalSourceCode);
    }

    @Test
    @Disabled("TODO Fix it, it should work")
    void processSources_alreadyRestructuredFile_checkFailFastFlowCompletesWithoutExceptions() throws Exception {
        // given
        String sampleSourceCode = loadSampleAllJava21FeaturesSource();
        Path javaFilePath = writeJavaFile(temporaryDirectory, "SampleAllJava21FeaturesList.java", sampleSourceCode);

        SourceProcessor sourceProcessor = new SourceProcessor();

        // First bring the file into a fully restructured and formatted state
        sourceProcessor.processSources(temporaryDirectory, ALL_JAVA_GLOBS, NO_EXCLUDES, FlowType.RESTRUCTURE);

        // when / then
        assertThatCode(() -> sourceProcessor.processSources(
                        temporaryDirectory, ALL_JAVA_GLOBS, NO_EXCLUDES, FlowType.CHECK_FAIL_FAST))
                .doesNotThrowAnyException();

        // sanity check that the file is still readable and not empty
        String finalSourceCode = Files.readString(javaFilePath, StandardCharsets.UTF_8);
        assertThat(finalSourceCode).isNotBlank();
    }

    // ----------------------------------------------------------------------
    // Helpers
    // ----------------------------------------------------------------------

    private static String loadSampleAllJava21FeaturesSource() throws Exception {
        URL resourceUrl = Objects.requireNonNull(
                SourceProcessorTest.class.getClassLoader().getResource(SAMPLE_ALL_JAVA21_RESOURCE_PATH),
                "SampleAllJava21FeaturesList.java resource must exist");

        Path resourcePath = Path.of(resourceUrl.toURI());
        return Files.readString(resourcePath, StandardCharsets.UTF_8);
    }

    private static Path writeJavaFile(Path baseDirectoryPath, String fileName, String fileContent) throws Exception {
        Path javaFilePath = baseDirectoryPath.resolve(fileName);
        return Files.writeString(javaFilePath, fileContent, StandardCharsets.UTF_8);
    }
}
