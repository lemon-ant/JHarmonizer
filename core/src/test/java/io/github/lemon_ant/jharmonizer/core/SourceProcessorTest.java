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
    private static final URL SAMPLE_ALL_JAVA21_RESOURCE_URL = TestCaseResourceUtils.requireClasspathResourceUrl(
            "/test-cases/core/translator/valid/SampleAllJava21FeaturesList.java");
    private static final String PUBLIC_STATIC_MAIN_METHOD_FRAGMENT = "public static void main(String[] args)";
    private static final String PUBLIC_CONSTRUCTOR_FRAGMENT = "public SampleAllJava21FeaturesList()";
    private static final String PUBLIC_ASSERTION_TEST_METHOD_FRAGMENT = "public void assertionTest()";
    private static final String PUBLIC_ENHANCED_FOR_LOOP_METHOD_FRAGMENT = "public void enhancedForLoop()";
    private static final String PUBLIC_RECORD_PERSON_FRAGMENT = "public record Person(String name, int age)";
    private static final String PUBLIC_INTERFACE_DEFAULT_METHOD_FRAGMENT = "public interface DefaultMethod";
    private static final String PACKAGE_PRIVATE_INNER_CLASS_FRAGMENT = "class InnerClass";
    private static final String PACKAGE_PRIVATE_STATIC_NESTED_CLASS_FRAGMENT = "static class StaticNestedClass";

    @TempDir
    Path temporaryDirectory;

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
    void processSources_defaultConfigSampleAllJava21FeaturesList_matchesExpectedOrdering() throws Exception {
        // Given
        String sampleSourceCode = TestCaseResourceUtils.readClasspathResourceAsString(SAMPLE_ALL_JAVA21_RESOURCE_URL);
        Path javaFilePath = writeJavaFile(temporaryDirectory, "SampleAllJava21FeaturesList.java", sampleSourceCode);
        SourceProcessor sourceProcessor = new SourceProcessor();

        // When
        sourceProcessor.processSources(
                temporaryDirectory, INCLUDE_ALL_JAVA_FILES, EXCLUDE_NO_FILES, FlowType.RESTRUCTURE);
        String processedSourceCode = Files.readString(javaFilePath, StandardCharsets.UTF_8);

        // Then
        int publicStaticMainMethodIndex =
                requireSourceFragmentIndex(processedSourceCode, PUBLIC_STATIC_MAIN_METHOD_FRAGMENT);
        int publicConstructorIndex = requireSourceFragmentIndex(processedSourceCode, PUBLIC_CONSTRUCTOR_FRAGMENT);
        int publicAssertionTestMethodIndex =
                requireSourceFragmentIndex(processedSourceCode, PUBLIC_ASSERTION_TEST_METHOD_FRAGMENT);
        int publicEnhancedForLoopMethodIndex =
                requireSourceFragmentIndex(processedSourceCode, PUBLIC_ENHANCED_FOR_LOOP_METHOD_FRAGMENT);
        int publicRecordPersonIndex = requireSourceFragmentIndex(processedSourceCode, PUBLIC_RECORD_PERSON_FRAGMENT);
        int publicInterfaceDefaultMethodIndex =
                requireSourceFragmentIndex(processedSourceCode, PUBLIC_INTERFACE_DEFAULT_METHOD_FRAGMENT);
        int packagePrivateInnerClassIndex =
                requireSourceFragmentIndex(processedSourceCode, PACKAGE_PRIVATE_INNER_CLASS_FRAGMENT);
        int packagePrivateStaticNestedClassIndex =
                requireSourceFragmentIndex(processedSourceCode, PACKAGE_PRIVATE_STATIC_NESTED_CLASS_FRAGMENT);

        assertThat(publicStaticMainMethodIndex)
                .as("Default config should place public static methods before public constructors")
                .isLessThan(publicConstructorIndex);
        assertThat(publicAssertionTestMethodIndex)
                .as("Default config should sort public instance methods alphabetically")
                .isLessThan(publicEnhancedForLoopMethodIndex);
        assertThat(publicRecordPersonIndex)
                .as("Default config should place public records before public interfaces in nested types")
                .isLessThan(publicInterfaceDefaultMethodIndex);
        assertThat(packagePrivateInnerClassIndex)
                .as("Default config should sort package-private nested classes alphabetically")
                .isLessThan(packagePrivateStaticNestedClassIndex);
    }

    private static Path writeJavaFile(Path baseDirectoryPath, String fileName, String fileContent) throws Exception {
        Path javaFilePath = baseDirectoryPath.resolve(fileName);
        return Files.writeString(javaFilePath, fileContent, StandardCharsets.UTF_8);
    }

    private static int requireSourceFragmentIndex(String sourceCode, String sourceFragment) {
        int sourceFragmentIndex = sourceCode.indexOf(sourceFragment);
        if (sourceFragmentIndex < 0) {
            throw new IllegalStateException("Source fragment not found: " + sourceFragment);
        }
        return sourceFragmentIndex;
    }
}
