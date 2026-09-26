// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
package io.github.lemon_ant.jharmonizer.core.translator.spoon;

import static io.github.lemon_ant.jharmonizer.core.config.ConfigurationManager.overrideDefaultConfig;
import static io.github.lemon_ant.jharmonizer.core.config.input.jharmonizer.JHarmonizerConfigurationManager.parseFlexibleUnifiedConfigFromClasspathResource;
import static io.github.lemon_ant.jharmonizer.core.files_handler.SrcFileCreator.createSrcFile;
import static io.github.lemon_ant.jharmonizer.core.testutils.SpoonTestCaseUtils.parseAstModelFromJavaFixtureResource;
import static io.github.lemon_ant.jharmonizer.core.testutils.TestCaseResourceUtils.requireClasspathResourceUrl;
import static io.github.lemon_ant.jharmonizer.core.translator.spoon.RelocationDetector.findRelocations;
import static io.github.lemon_ant.jharmonizer.core.translator.spoon.RelocationDetector.hasRelocations;
import static io.github.lemon_ant.jharmonizer.core.translator.spoon.RelocationDetector.hasReorderedAnnotations;
import static io.github.lemon_ant.jharmonizer.core.translator.spoon.RelocationDetector.snapshotOriginalMemberOrder;
import static org.assertj.core.api.Assertions.assertThat;

import io.github.lemon_ant.jharmonizer.core.config.ConfigurationManager;
import io.github.lemon_ant.jharmonizer.core.config.compiled.CompiledConfig;
import io.github.lemon_ant.jharmonizer.core.files_handler.SrcFile;
import io.github.lemon_ant.jharmonizer.core.sorter.Sorter;
import io.github.lemon_ant.jharmonizer.core.spoon.AnnotationSourceScanner.AnnotationSrcFragment;
import io.github.lemon_ant.jharmonizer.core.translator.ParsingResult;
import io.github.lemon_ant.jharmonizer.core.translator.SrcAstTranslator;
import java.nio.file.Path;
import java.util.List;
import lombok.NonNull;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.declaration.CtType;
import spoon.reflect.declaration.CtTypeMember;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RelocationDetectorTest {
    private CompiledConfig defaultConfig;
    private PrinterConfig defaultPrinterConfig;

    @BeforeAll
    void setUp() {
        defaultConfig = ConfigurationManager.loadDefaultConfig();
        defaultPrinterConfig = new PrinterConfig(
                defaultConfig.getFormatting().isBlankLineAfterTypeHeader(),
                defaultConfig.getFormatting().isBlankLineBeforeComment(),
                defaultConfig.getFormatting().isBlankLineBetweenFields());
    }

    @Test
    void findRelocations_contiguousChunkMoved_reportsSingleRelocationWithMinimalMovedChunk() {
        // Given — sorted order is [a, b, c, d] but original was [c, d, a, b]
        // (chunk [a, b] moved to the front; equivalently [c, d] moved to the back)
        SrcFile srcFile = createSrcFile(
                "public class Sample {\n"
                        + "    public void a() {}\n\n"
                        + "    public void b() {}\n\n"
                        + "    public void c() {}\n\n"
                        + "    public void d() {}\n"
                        + "}\n",
                Path.of("Sample.java"));
        ParsingResult parsingResult = SrcAstTranslator.parse(srcFile, defaultPrinterConfig);
        SpoonAstModel spoonAstModel = parsingResult.getSpoonAstModel();
        CtType<?> sampleType =
                spoonAstModel.getCompilationUnit().getDeclaredTypes().get(0);
        CtMethod<?> methodA = requireMethodByName(sampleType, "a");
        CtMethod<?> methodB = requireMethodByName(sampleType, "b");
        CtMethod<?> methodC = requireMethodByName(sampleType, "c");
        CtMethod<?> methodD = requireMethodByName(sampleType, "d");
        // Simulate original order: [c, d, a, b]
        List<CtTypeMember> simulatedOriginalOrder = List.of(methodC, methodD, methodA, methodB);

        // When
        List<MemberRelocation> relocations =
                findRelocations(simulatedOriginalOrder, spoonAstModel.getCompilationUnit());

        // Then — patience-sort LIS keeps the latest-finishing increasing run [c, d] stable;
        // [a, b] is reported as the single moved chunk inserted before c
        assertThat(relocations).hasSize(1);
        assertThat(relocations.get(0).getRelocatedMembers()).containsExactly(methodA, methodB);
        assertThat(relocations.get(0).getSortedPredecessor()).isNull();
        assertThat(relocations.get(0).getSortedSuccessor()).isEqualTo(methodC);
    }

    @Test
    void findRelocations_memberBelongingToRelocatedType_notFlaggedAsMemberViolation() {
        // Given
        SrcFile srcFile =
                createSrcFile("class Alpha { static String label() {} }\nclass Beta {}\n", Path.of("Sample.java"));
        ParsingResult parsingResult = SrcAstTranslator.parse(srcFile, defaultPrinterConfig);
        SpoonAstModel spoonAstModel = parsingResult.getSpoonAstModel();
        CtType<?> alpha = spoonAstModel.getCompilationUnit().getDeclaredTypes().get(0);
        CtType<?> beta = spoonAstModel.getCompilationUnit().getDeclaredTypes().get(1);
        CtTypeMember labelMethod = alpha.getMethods().iterator().next();
        // Simulate original order: Beta first, Alpha second; label stays as Alpha's only member.
        List<CtTypeMember> simulatedOriginalOrder = List.of(beta, alpha, labelMethod);

        // When
        List<MemberRelocation> relocations =
                findRelocations(simulatedOriginalOrder, spoonAstModel.getCompilationUnit());

        // Then
        assertThat(relocations)
                .noneMatch(relocation -> relocation.getRelocatedMembers().contains(labelMethod));
    }

    @Test
    void findRelocations_noChanges_returnsEmptyList() {
        // Given
        SrcFile srcFile = createSrcFile(
                "public class Sample {\n    public void a() {}\n\n    public void b() {}\n}\n", Path.of("Sample.java"));
        ParsingResult parsingResult = SrcAstTranslator.parse(srcFile, defaultPrinterConfig);
        SpoonAstModel spoonAstModel = parsingResult.getSpoonAstModel();
        List<CtTypeMember> originalMemberOrder = snapshotOriginalMemberOrder(spoonAstModel.getCompilationUnit());

        // When
        List<MemberRelocation> relocations = findRelocations(originalMemberOrder, spoonAstModel.getCompilationUnit());

        // Then
        assertThat(relocations).isEmpty();
    }

    @Test
    void findRelocations_oneMemberMovedFromLastToFirst_reportsSingleRelocationForMovedMember() {
        // Given — compilation unit has the "sorted" order [d, a, b, c] (d was last, now first)
        SrcFile srcFile = createSrcFile(
                "public class Sample {\n"
                        + "    public void d() {}\n\n"
                        + "    public void a() {}\n\n"
                        + "    public void b() {}\n\n"
                        + "    public void c() {}\n"
                        + "}\n",
                Path.of("Sample.java"));
        ParsingResult parsingResult = SrcAstTranslator.parse(srcFile, defaultPrinterConfig);
        SpoonAstModel spoonAstModel = parsingResult.getSpoonAstModel();
        CtType<?> sampleType =
                spoonAstModel.getCompilationUnit().getDeclaredTypes().get(0);
        CtMethod<?> methodD = requireMethodByName(sampleType, "d");
        CtMethod<?> methodA = requireMethodByName(sampleType, "a");
        CtMethod<?> methodB = requireMethodByName(sampleType, "b");
        CtMethod<?> methodC = requireMethodByName(sampleType, "c");
        // Simulate original order: [a, b, c, d] — d was last
        List<CtTypeMember> simulatedOriginalOrder = List.of(methodA, methodB, methodC, methodD);

        // When
        List<MemberRelocation> relocations =
                findRelocations(simulatedOriginalOrder, spoonAstModel.getCompilationUnit());

        // Then — minimal moved set is just {d}; [a, b, c] remain stable as the longest increasing subsequence
        assertThat(relocations).hasSize(1);
        assertThat(relocations.get(0).getRelocatedMembers()).containsExactly(methodD);
        assertThat(relocations.get(0).getSortedPredecessor()).isNull();
        assertThat(relocations.get(0).getSortedSuccessor()).isEqualTo(methodA);
    }

    @Test
    void findRelocations_withEmptyOriginalMemberOrder_returnsEmptyList() {
        // Given
        SrcFile srcFile = createSrcFile("public class Sample {\n    public void a() {}\n}\n", Path.of("Sample.java"));
        ParsingResult parsingResult = SrcAstTranslator.parse(srcFile, defaultPrinterConfig);
        SpoonAstModel spoonAstModel = parsingResult.getSpoonAstModel();

        // When
        List<MemberRelocation> relocations = findRelocations(List.of(), spoonAstModel.getCompilationUnit());

        // Then
        assertThat(relocations).isEmpty();
    }

    @Test
    void isRelocated_annotationsOnly_returnsTrue() {
        // Given
        SrcFile srcFile = createSrcFile(
                "@SuppressWarnings(\"all\") @Deprecated class AnnotationOnlyRelocation {}",
                Path.of("AnnotationOnlyRelocation.java"));
        SpoonAstModel parsedModel =
                SrcAstTranslator.parse(srcFile, defaultPrinterConfig).getSpoonAstModel();
        SpoonAstModel sortedModel = new Sorter(defaultConfig).sort(parsedModel).getSortedSpoonAstModel();

        // When
        boolean relocated = hasRelocations(sortedModel);

        // Then
        assertThat(sortedModel.getAnnotationGroups().get(0))
                .extracting(fragment -> fragment.getDescriptor().getName())
                .containsExactly("Deprecated", "SuppressWarnings");
        assertThat(relocated).isTrue();
    }

    @Test
    void isRelocated_noChanges_returnsFalse() {
        // Given
        SrcFile srcFile = createSrcFile(
                "public class Sample {\n    public void a() {}\n\n    public void b() {}\n}\n", Path.of("Sample.java"));
        ParsingResult parsingResult = SrcAstTranslator.parse(srcFile, defaultPrinterConfig);
        SpoonAstModel spoonAstModel = parsingResult.getSpoonAstModel();

        // When
        boolean relocated = hasRelocations(spoonAstModel);

        // Then
        assertThat(relocated).isFalse();
    }

    @Test
    void snapshotOriginalMemberOrder_multiRootTypeFile_collectsMembersInSourceOrder() {
        // Given
        SrcFile srcFile =
                createSrcFile("class Alpha { static String label() {} }\nclass Beta {}\n", Path.of("Sample.java"));
        ParsingResult parsingResult = SrcAstTranslator.parse(srcFile, defaultPrinterConfig);
        SpoonAstModel spoonAstModel = parsingResult.getSpoonAstModel();
        CtType<?> alpha = spoonAstModel.getCompilationUnit().getDeclaredTypes().get(0);
        CtType<?> beta = spoonAstModel.getCompilationUnit().getDeclaredTypes().get(1);
        CtTypeMember labelMethod = alpha.getMethods().iterator().next();

        // When
        List<CtTypeMember> memberOrder = snapshotOriginalMemberOrder(spoonAstModel.getCompilationUnit());

        // Then — DFS source order: Alpha, Alpha.label, Beta
        assertThat(memberOrder).containsExactly(alpha, labelMethod, beta);
    }

    @NonNull
    private static CtMethod<?> requireMethodByName(CtType<?> type, String name) {
        return type.getMethods().stream()
                .filter(method -> method.getSimpleName().equals(name))
                .findFirst()
                .orElseThrow(
                        () -> new IllegalStateException("No method named '" + name + "' in " + type.getSimpleName()));
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    class AnnotationRelocations {
        private CompiledConfig annotationConfig;
        private SpoonAstModel permutationModel;

        @BeforeAll
        void setUp() {
            annotationConfig =
                    overrideDefaultConfig(parseFlexibleUnifiedConfigFromClasspathResource(requireClasspathResourceUrl(
                            "/test-cases/core/e2e/printer/scenarios/18-annotation-language-constructs/config.yml")));
            permutationModel = SrcAstTranslator.parse(
                            createSrcFile(
                                    "@Deprecated @SuppressWarnings(\"all\")"
                                            + " @javax.annotation.processing.Generated(\"test\") class ThreeAnnotations"
                                            + " {}",
                                    Path.of("ThreeAnnotations.java")),
                            defaultPrinterConfig)
                    .getSpoonAstModel();
        }

        @ParameterizedTest
        @ValueSource(
                strings = {
                    "AnnotationLiteralAndCommentPreservation.java",
                    "AnnotationOrderingOptOut.java",
                    "EnumAndInterfaceAnnotationOrdering.java",
                    "RecordComponentAnnotationOrdering.java",
                    "TypeUseAnnotationOrdering.java",
                    "module-info.java",
                    "package-info.java"
                })
        void isRelocated_annotationLanguageConstructs_detectsSorting(@NonNull String fileName) {
            // Given
            SpoonAstModel parsedModel = parseAstModelFromJavaFixtureResource(requireClasspathResourceUrl(
                    "/test-cases/core/e2e/printer/scenarios/18-annotation-language-constructs/input/" + fileName));
            SpoonAstModel sortedModel =
                    new Sorter(annotationConfig).sort(parsedModel).getSortedSpoonAstModel();

            // When
            boolean relocated = hasRelocations(sortedModel);

            // Then
            assertThat(relocated).isTrue();
            assertThat(hasReorderedAnnotations(sortedModel)).isTrue();
            assertThat(hasReorderedAnnotations(parsedModel)).isFalse();
            assertThat(findRelocations(sortedModel.getOriginalMemberOrder(), sortedModel.getCompilationUnit()))
                    .isEmpty();
        }

        @ParameterizedTest
        @CsvSource({
            "0, 1, 2, false",
            "0, 2, 1, true",
            "1, 0, 2, true",
            "1, 2, 0, true",
            "2, 0, 1, true",
            "2, 1, 0, true"
        })
        void isRelocated_annotationPermutations_detectsEveryChangedOrder(
                int first, int second, int third, boolean expectedRelocation) {
            // Given
            List<AnnotationSrcFragment> originalGroup =
                    permutationModel.getAnnotationGroups().get(0);
            SpoonAstModel reorderedModel = permutationModel.withAnnotationGroups(
                    List.of(List.of(originalGroup.get(first), originalGroup.get(second), originalGroup.get(third))));

            // When
            boolean relocated = hasRelocations(reorderedModel);

            // Then
            assertThat(relocated).isEqualTo(expectedRelocation);
            assertThat(hasReorderedAnnotations(reorderedModel)).isEqualTo(expectedRelocation);
            assertThat(hasRelocations(permutationModel)).isFalse();
        }

        @Test
        void isRelocated_disabledAnnotationSorting_returnsFalse() {
            // Given
            CompiledConfig disabledConfig = overrideDefaultConfig(parseFlexibleUnifiedConfigFromClasspathResource(
                    requireClasspathResourceUrl("/test-cases/core/e2e/reorder/34-annotations-disabled/config.yml")));
            SpoonAstModel parsedModel = parseAstModelFromJavaFixtureResource(requireClasspathResourceUrl(
                    "/test-cases/core/e2e/reorder/34-annotations-disabled/input/DisabledAnnotationOrdering.java"));
            SpoonAstModel sortedModel =
                    new Sorter(disabledConfig).sort(parsedModel).getSortedSpoonAstModel();

            // When
            boolean relocated = hasRelocations(sortedModel);

            // Then
            assertThat(sortedModel.getAnnotationGroups()).containsExactlyElementsOf(parsedModel.getAnnotationGroups());
            assertThat(relocated).isFalse();
        }

        @Test
        void isRelocated_onlyLaterAnnotationGroupChanges_returnsTrue() {
            // Given
            SrcFile srcFile = createSrcFile(
                    "@Deprecated class LaterAnnotationGroup { @SuppressWarnings(\"all\") @Deprecated void execute() {}"
                            + " }",
                    Path.of("LaterAnnotationGroup.java"));
            SpoonAstModel parsedModel =
                    SrcAstTranslator.parse(srcFile, defaultPrinterConfig).getSpoonAstModel();
            SpoonAstModel sortedModel =
                    new Sorter(annotationConfig).sort(parsedModel).getSortedSpoonAstModel();

            // When
            boolean relocated = hasRelocations(sortedModel);

            // Then
            assertThat(sortedModel.getAnnotationGroups().get(0))
                    .containsExactlyElementsOf(parsedModel.getAnnotationGroups().get(0));
            assertThat(relocated).isTrue();
        }

        @Test
        void isRelocated_repeatedAnnotationArgumentsChangeOrder_returnsTrue() {
            // Given
            CompiledConfig argumentsConfig =
                    overrideDefaultConfig(parseFlexibleUnifiedConfigFromClasspathResource(requireClasspathResourceUrl(
                            "/test-cases/core/e2e/reorder/37-annotations-arguments-alpha/config.yml")));
            SpoonAstModel parsedModel = parseAstModelFromJavaFixtureResource(
                    requireClasspathResourceUrl(
                            "/test-cases/core/e2e/reorder/37-annotations-arguments-alpha/input/AlphabeticalAnnotationArgumentsOrdering.java"));
            List<AnnotationSrcFragment> repeatedAnnotations =
                    parsedModel.getAnnotationGroups().get(1);
            SpoonAstModel repeatedAnnotationsModel = parsedModel.withAnnotationGroups(List.of(repeatedAnnotations));
            SpoonAstModel sortedModel =
                    new Sorter(argumentsConfig).sort(repeatedAnnotationsModel).getSortedSpoonAstModel();

            // When
            boolean relocated = hasRelocations(sortedModel);

            // Then
            assertThat(repeatedAnnotations)
                    .extracting(fragment -> fragment.getDescriptor().getName())
                    .containsExactly("Tag", "Tag");
            assertThat(relocated).isTrue();
            assertThat(hasReorderedAnnotations(sortedModel)).isTrue();
            assertThat(findRelocations(sortedModel.getOriginalMemberOrder(), sortedModel.getCompilationUnit()))
                    .isEmpty();
        }

        @ParameterizedTest
        @ValueSource(
                strings = {
                    "class UnchangedAnnotations {}",
                    "@Deprecated class UnchangedAnnotations {}",
                    "@Deprecated @SuppressWarnings(\"all\") class UnchangedAnnotations {}"
                })
        void isRelocated_unchangedAnnotations_returnsFalse(@NonNull String srcCode) {
            // Given
            SpoonAstModel parsedModel = SrcAstTranslator.parse(
                            createSrcFile(srcCode, Path.of("UnchangedAnnotations.java")), defaultPrinterConfig)
                    .getSpoonAstModel();
            SpoonAstModel sortedModel =
                    new Sorter(annotationConfig).sort(parsedModel).getSortedSpoonAstModel();

            // When
            boolean relocated = hasRelocations(sortedModel);

            // Then
            assertThat(relocated).isFalse();
        }
    }
}
