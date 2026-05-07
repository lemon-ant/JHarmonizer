// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
package io.github.lemon_ant.jharmonizer.core.config.input.jharmonizer;

import static io.github.lemon_ant.jharmonizer.core.testutils.TestCaseResourceUtils.TEST_CASES_DIR;
import static org.assertj.core.api.Assertions.assertThat;

import io.github.lemon_ant.jharmonizer.core.SrcProcessor;
import io.github.lemon_ant.jharmonizer.core.flow.FlowType;
import io.github.lemon_ant.jharmonizer.core.testutils.TestCaseResourceUtils;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import lombok.NonNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class DefaultConfigOrderingIntegrationTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void applyEmbeddedDefaultConfig_sampleAllJava21FeaturesList_matchExpectedOrdering() throws Exception {
        // Given
        String sampleSrcCode =
                TestCaseResourceUtils.readClasspathResourceAsString(Constants.SAMPLE_ALL_JAVA21_RESOURCE_URL);
        Path javaFilePath = writeJavaFile(temporaryDirectory, Constants.SAMPLE_ALL_JAVA21_FILE_NAME, sampleSrcCode);
        SrcProcessor srcProcessor = new SrcProcessor();

        // When
        srcProcessor.processSources(
                temporaryDirectory, Constants.INCLUDE_ALL_JAVA_FILES, Constants.EXCLUDE_NO_FILES, FlowType.REORDER);
        String processedSrcCode = Files.readString(javaFilePath, StandardCharsets.UTF_8);

        // Then
        int publicStaticMainMethodIndex =
                requireSrcFragmentIndex(processedSrcCode, Constants.PUBLIC_STATIC_MAIN_METHOD_FRAGMENT);
        int publicConstructorIndex = requireSrcFragmentIndex(processedSrcCode, Constants.PUBLIC_CONSTRUCTOR_FRAGMENT);
        int publicAssertionTestMethodIndex =
                requireSrcFragmentIndex(processedSrcCode, Constants.PUBLIC_ASSERTION_TEST_METHOD_FRAGMENT);
        int publicEnhancedForLoopMethodIndex =
                requireSrcFragmentIndex(processedSrcCode, Constants.PUBLIC_ENHANCED_FOR_LOOP_METHOD_FRAGMENT);
        int publicRecordPersonIndex =
                requireSrcFragmentIndex(processedSrcCode, Constants.PUBLIC_RECORD_PERSON_FRAGMENT);
        int publicInterfaceDefaultMethodIndex =
                requireSrcFragmentIndex(processedSrcCode, Constants.PUBLIC_INTERFACE_DEFAULT_METHOD_FRAGMENT);
        int packagePrivateInnerClassIndex =
                requireSrcFragmentIndex(processedSrcCode, Constants.PACKAGE_PRIVATE_INNER_CLASS_FRAGMENT);
        int packagePrivateStaticNestedClassIndex =
                requireSrcFragmentIndex(processedSrcCode, Constants.PACKAGE_PRIVATE_STATIC_NESTED_CLASS_FRAGMENT);

        assertThat(publicStaticMainMethodIndex)
                .as("Default config should place public static methods before public constructors")
                .isLessThan(publicConstructorIndex);
        assertThat(publicAssertionTestMethodIndex)
                .as("Default config should sort public instance methods alphabetically")
                .isLessThan(publicEnhancedForLoopMethodIndex);
        assertThat(publicRecordPersonIndex)
                .as("Default config should place public interfaces before public records in nested types")
                .isGreaterThan(publicInterfaceDefaultMethodIndex);
        assertThat(packagePrivateInnerClassIndex)
                .as("Default config should sort package-private nested classes alphabetically")
                .isLessThan(packagePrivateStaticNestedClassIndex);
    }

    private static int requireSrcFragmentIndex(String srcCode, String srcFragment) {
        int srcFragmentIndex = srcCode.indexOf(srcFragment);
        if (srcFragmentIndex < 0) {
            throw new IllegalStateException("Source fragment not found: " + srcFragment);
        }
        return srcFragmentIndex;
    }

    @NonNull
    private static Path writeJavaFile(Path baseDirectoryPath, String fileName, String fileContent) throws Exception {
        Path javaFilePath = baseDirectoryPath.resolve(fileName);
        return Files.writeString(javaFilePath, fileContent, StandardCharsets.UTF_8);
    }

    private static final class Constants {
        private static final Collection<String> EXCLUDE_NO_FILES = List.of();
        private static final Collection<String> INCLUDE_ALL_JAVA_FILES = Set.of();
        private static final String PACKAGE_PRIVATE_INNER_CLASS_FRAGMENT = "class InnerClass";
        private static final String PACKAGE_PRIVATE_STATIC_NESTED_CLASS_FRAGMENT = "static class StaticNestedClass";
        private static final String PUBLIC_ASSERTION_TEST_METHOD_FRAGMENT = "public void assertionTest()";
        private static final String PUBLIC_CONSTRUCTOR_FRAGMENT = "public SampleAllJava21FeaturesList()";
        private static final String PUBLIC_ENHANCED_FOR_LOOP_METHOD_FRAGMENT = "public void enhancedForLoop()";
        private static final String PUBLIC_INTERFACE_DEFAULT_METHOD_FRAGMENT = "public interface DefaultMethod";
        private static final String PUBLIC_RECORD_PERSON_FRAGMENT = "public record Person(String name, int age)";
        private static final String PUBLIC_STATIC_MAIN_METHOD_FRAGMENT = "public static void main(String[] args)";
        private static final String SAMPLE_ALL_JAVA21_FILE_NAME = "SampleAllJava21FeaturesList.java";
        private static final URL SAMPLE_ALL_JAVA21_RESOURCE_URL = TestCaseResourceUtils.requireClasspathResourceUrl(
                "/" + TEST_CASES_DIR + "/core/translator/valid/SampleAllJava21FeaturesList.java");
    }
}
