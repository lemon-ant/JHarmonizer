// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
package io.github.lemon_ant.jharmonizer.core.sorter;

import static io.github.lemon_ant.jharmonizer.core.config.ConfigurationManager.loadDefaultConfig;
import static io.github.lemon_ant.jharmonizer.core.config.ConfigurationManager.overrideDefaultConfig;
import static io.github.lemon_ant.jharmonizer.core.config.input.jharmonizer.JHarmonizerConfigurationManager.parseFlexibleUnifiedConfigFromClasspathResource;
import static io.github.lemon_ant.jharmonizer.core.files_handler.SrcFileCreator.createSrcFile;
import static io.github.lemon_ant.jharmonizer.core.spoon.SpoonTypeUtils.streamDeclaredHierarchy;
import static io.github.lemon_ant.jharmonizer.core.testutils.SpoonTestCaseUtils.parseAstModelFromJavaFixtureResource;
import static io.github.lemon_ant.jharmonizer.core.testutils.TestCaseResourceUtils.requireClasspathResourceUrl;
import static io.github.lemon_ant.jharmonizer.core.translator.spoon.SpoonAstModelTestCreator.copyWithClonedCompilationUnit;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.github.lemon_ant.jharmonizer.core.config.compiled.CompiledConfig;
import io.github.lemon_ant.jharmonizer.core.files_handler.SrcFile;
import io.github.lemon_ant.jharmonizer.core.sorter.spoon.SpoonSorter;
import io.github.lemon_ant.jharmonizer.core.spoon.AnnotationSrcGroup;
import io.github.lemon_ant.jharmonizer.core.translator.SrcAstTranslator;
import io.github.lemon_ant.jharmonizer.core.translator.spoon.SpoonAstModel;
import io.github.lemon_ant.jharmonizer.core.translator.spoon.SpoonParser;
import java.net.URL;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Consumer;
import lombok.NonNull;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import spoon.reflect.cu.SourcePosition;
import spoon.reflect.declaration.CtType;
import spoon.reflect.declaration.CtTypeMember;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SorterTest {
    private static final URL COMPLEX_TYPES_FIXTURE = requireClasspathResourceUrl(
            "/test-cases/core/e2e/printer/scenarios/11-default-complex-types/input/DefaultConfigComplexTypesScenario.java");
    private static final URL DISABLED_ANNOTATIONS_CONFIG =
            requireClasspathResourceUrl("/test-cases/core/e2e/reorder/34-annotations-disabled/config.yml");
    private static final URL DISABLED_ANNOTATIONS_FIXTURE = requireClasspathResourceUrl(
            "/test-cases/core/e2e/reorder/34-annotations-disabled/input/DisabledAnnotationOrdering.java");
    private static final URL FIELD_ALPHA_CONFIG = requireClasspathResourceUrl(
            "/test-cases/core/e2e/reorder/15-field-alpha-ordering-multi-level-dependencies/config.yml");
    private static final URL ORDERED_DEPENDENCIES_FIXTURE = requireClasspathResourceUrl(
            "/test-cases/core/e2e/reorder/15-field-alpha-ordering-multi-level-dependencies/expected/FieldAlphaOrderingMultiLevelDependenciesSample.java");
    private static final URL TOP_LEVEL_TYPES_CONFIG =
            requireClasspathResourceUrl("/test-cases/core/e2e/reorder/17-top-level-types-ordering/config.yml");

    private Sorter annotationSortingDisabledSorter;
    private Sorter defaultSorter;
    private SpoonSorter defaultSpoonSorter;
    private Sorter fieldAlphaSorter;
    private Sorter topLevelTypesSorter;

    @BeforeAll
    void setUp() {
        annotationSortingDisabledSorter = new Sorter(
                overrideDefaultConfig(parseFlexibleUnifiedConfigFromClasspathResource(DISABLED_ANNOTATIONS_CONFIG)));
        CompiledConfig defaultConfig = loadDefaultConfig();
        defaultSpoonSorter = new SpoonSorter(defaultConfig);
        defaultSorter = new Sorter(defaultConfig);
        fieldAlphaSorter =
                new Sorter(overrideDefaultConfig(parseFlexibleUnifiedConfigFromClasspathResource(FIELD_ALPHA_CONFIG)));
        topLevelTypesSorter = new Sorter(
                overrideDefaultConfig(parseFlexibleUnifiedConfigFromClasspathResource(TOP_LEVEL_TYPES_CONFIG)));
    }

    @Test
    void sort_annotationOnlyChanges_returnsNoMemberRelocations() {
        SpoonAstModel model = SpoonParser.parseJavaSrcFile(createSrcFile(
                "@SuppressWarnings(\"all\") @Deprecated class AnnotationOnlyRelocation {}",
                Path.of("AnnotationOnlyRelocation.java")));

        // When
        SortingResult result = defaultSorter.sort(model);

        // Then
        assertThat(result.getMemberRelocations()).isEmpty();
        assertThat(result.isAnnotationsReordered()).isTrue();
        assertThat(result.isMembersReordered()).isFalse();
        assertThat(result.isReordered()).isTrue();
        assertThat(result.getSortedSpoonAstModel()
                        .getAnnotationSrcGroups()
                        .getFirst()
                        .getAnnotationSrcFragments())
                .extracting(fragment -> fragment.getDescriptor().getName())
                .containsExactly("Deprecated", "SuppressWarnings");
    }

    @Test
    void sort_diagnosticsDisabled_reportsReorderingWithoutDiagnostics() {
        SpoonAstModel model = parseAstModelFromJavaFixtureResource(COMPLEX_TYPES_FIXTURE);

        // When
        SortingResult result = defaultSorter.sort(model, false);

        // Then
        assertThat(result.isAnnotationsReordered()).isFalse();
        assertThat(result.isMembersReordered()).isTrue();
        assertThat(result.isReordered()).isTrue();
        assertThat(result.getMemberRelocations()).isEmpty();
    }

    @Test
    void sort_disabledAnnotationSorting_preservesInputGroups() {
        SpoonAstModel model = parseAstModelFromJavaFixtureResource(DISABLED_ANNOTATIONS_FIXTURE);

        // When
        SortingResult result = annotationSortingDisabledSorter.sort(model);

        // Then
        assertThat(result.isAnnotationsReordered()).isFalse();
        assertThat(result.getSortedSpoonAstModel().getAnnotationSrcGroups())
                .containsExactlyElementsOf(model.getAnnotationSrcGroups());
    }

    @ParameterizedTest
    @CsvSource({"false, false", "true, false", "false, true", "true, true"})
    void sort_independentOrderChanges_preservesBothFlags(boolean annotationsOutOfOrder, boolean membersOutOfOrder) {
        // Given
        String annotationSrcCode = annotationsOutOfOrder
                ? "@SuppressWarnings(\"all\") @Deprecated "
                : "@Deprecated @SuppressWarnings(\"all\") ";
        String membersInSrcOrder = membersOutOfOrder ? "void execute() {} int value;" : "int value; void execute() {}";
        SpoonAstModel model = SpoonParser.parseJavaSrcFile(createSrcFile(
                annotationSrcCode + "class IndependentSortingChanges { " + membersInSrcOrder + " }",
                Path.of("IndependentSortingChanges.java")));

        // When
        SortingResult result = defaultSorter.sort(model);

        // Then
        assertThat(result.isAnnotationsReordered()).isEqualTo(annotationsOutOfOrder);
        assertThat(result.isMembersReordered()).isEqualTo(membersOutOfOrder);
        assertThat(result.isReordered()).isEqualTo(annotationsOutOfOrder || membersOutOfOrder);
    }

    @Test
    void sort_invalidTopLevelPositions_reportsReorderingWithoutDiagnostics() {
        // Given
        SpoonAstModel model =
                SpoonParser.parseJavaSrcFile(createSrcFile("class Zeta {} class Alpha {}", Path.of("Zeta.java")));
        model.getCompilationUnit().getDeclaredTypes().forEach(type -> type.setPosition(SourcePosition.NOPOSITION));

        // When
        SortingResult result = topLevelTypesSorter.sort(model);

        // Then
        assertThat(result.isAnnotationsReordered()).isFalse();
        assertThat(result.isMembersReordered()).isTrue();
        assertThat(result.isReordered()).isTrue();
        assertThat(result.getMemberRelocations()).isEmpty();
        assertThat(result.getSortedSpoonAstModel().getCompilationUnit().getDeclaredTypes())
                .extracting(CtType::getSimpleName)
                .containsExactly("Alpha", "Zeta");
    }

    @Test
    void sort_nestedAndTopLevelDeclarations_returnsUnmodifiableDiagnostics() {
        SpoonAstModel model = parseAstModelFromJavaFixtureResource(COMPLEX_TYPES_FIXTURE);

        // When
        SortingResult result = defaultSorter.sort(model);

        // Then
        List<CtTypeMember> relocatedMembers = result.getMemberRelocations().stream()
                .flatMap(relocation -> relocation.getRelocatedMembers().stream())
                .toList();
        assertThat(relocatedMembers).anyMatch(member -> member instanceof CtType<?>);
        assertThat(relocatedMembers).anyMatch(member -> !(member instanceof CtType<?>));
        assertThat(result.isMembersReordered()).isTrue();
        assertThat(result.isReordered()).isTrue();
        assertThat(result.getSortedSpoonAstModel().getCompilationUnit()).isSameAs(model.getCompilationUnit());
        assertThatThrownBy(() -> result.getMemberRelocations().clear())
                .isInstanceOf(UnsupportedOperationException.class);
        result.getMemberRelocations()
                .forEach(relocation -> assertThatThrownBy(
                                () -> relocation.getRelocatedMembers().clear())
                        .isInstanceOf(UnsupportedOperationException.class));
    }

    @Test
    void sort_orderedDeclarations_returnsNoMemberRelocations() {
        SpoonAstModel model = SpoonParser.parseJavaSrcFile(
                createSrcFile("class OrderedDeclarations {}", Path.of("OrderedDeclarations.java")));

        // When
        SortingResult result = defaultSorter.sort(model);

        // Then
        assertThat(result.getMemberRelocations()).isEmpty();
        assertThat(result.isAnnotationsReordered()).isFalse();
        assertThat(result.isMembersReordered()).isFalse();
        assertThat(result.isReordered()).isFalse();
    }

    @Test
    void sort_orderedDependencyChain_reportsNoReorderingAfterRepair() {
        // Given
        SpoonAstModel model = parseAstModelFromJavaFixtureResource(ORDERED_DEPENDENCIES_FIXTURE);
        List<CtTypeMember> membersBeforeSorting = model.getOriginalMemberOrder();

        // When
        SortingResult result = fieldAlphaSorter.sort(model);

        // Then
        assertThat(result.isAnnotationsReordered()).isFalse();
        assertThat(result.isMembersReordered()).isFalse();
        assertThat(result.isReordered()).isFalse();
        assertThat(result.getMemberRelocations()).isEmpty();
        assertThat(streamDeclaredHierarchy(result.getSortedSpoonAstModel().getCompilationUnit())
                        .toList())
                .containsExactlyElementsOf(membersBeforeSorting);
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    class AnnotationReordering {
        private CompiledConfig annotationConfig;

        @BeforeAll
        void setUp() {
            annotationConfig =
                    overrideDefaultConfig(parseFlexibleUnifiedConfigFromClasspathResource(requireClasspathResourceUrl(
                            "/test-cases/core/e2e/printer/scenarios/18-annotation-language-constructs/config.yml")));
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
        void sort_annotationLanguageConstructs_detectsSorting(@NonNull String fileName) {
            // Given
            SpoonAstModel parsedModel = parseAstModelFromJavaFixtureResource(requireClasspathResourceUrl(
                    "/test-cases/core/e2e/printer/scenarios/18-annotation-language-constructs/input/" + fileName));

            // When
            SortingResult result = new Sorter(annotationConfig).sort(parsedModel);

            // Then
            assertThat(result.isAnnotationsReordered()).isTrue();
            assertThat(result.isMembersReordered()).isFalse();
            assertThat(result.isReordered()).isTrue();
            assertThat(result.getMemberRelocations()).isEmpty();
        }

        @Test
        void sort_onlyLaterAnnotationGroupChanges_returnsTrue() {
            // Given
            SrcFile srcFile = createSrcFile(
                    "@Deprecated class LaterAnnotationGroup { @SuppressWarnings(\"all\") @Deprecated void execute() {}"
                            + " }",
                    Path.of("LaterAnnotationGroup.java"));
            SpoonAstModel parsedModel = SrcAstTranslator.parse(srcFile).getSpoonAstModel();

            // When
            SortingResult result = new Sorter(annotationConfig).sort(parsedModel);

            // Then
            SpoonAstModel sortedModel = result.getSortedSpoonAstModel();
            assertThat(sortedModel.getAnnotationSrcGroups().get(0))
                    .isEqualTo(parsedModel.getAnnotationSrcGroups().get(0));
            assertThat(result.isAnnotationsReordered()).isTrue();
            assertThat(result.isMembersReordered()).isFalse();
            assertThat(result.isReordered()).isTrue();
        }

        @Test
        void sort_repeatedAnnotationArgumentsChangeOrder_returnsTrue() {
            // Given
            CompiledConfig argumentsConfig =
                    overrideDefaultConfig(parseFlexibleUnifiedConfigFromClasspathResource(requireClasspathResourceUrl(
                            "/test-cases/core/e2e/reorder/37-annotations-arguments-alpha/config.yml")));
            SpoonAstModel parsedModel = parseAstModelFromJavaFixtureResource(
                    requireClasspathResourceUrl(
                            "/test-cases/core/e2e/reorder/37-annotations-arguments-alpha/input/AlphabeticalAnnotationArgumentsOrdering.java"));
            AnnotationSrcGroup repeatedAnnotations =
                    parsedModel.getAnnotationSrcGroups().get(1);
            SpoonAstModel repeatedAnnotationsModel = parsedModel.withAnnotationSrcGroups(List.of(repeatedAnnotations));

            // When
            SortingResult result = new Sorter(argumentsConfig).sort(repeatedAnnotationsModel);

            // Then
            assertThat(repeatedAnnotations.getAnnotationSrcFragments())
                    .extracting(fragment -> fragment.getDescriptor().getName())
                    .containsExactly("Tag", "Tag");
            assertThat(result.isAnnotationsReordered()).isTrue();
            assertThat(result.isMembersReordered()).isFalse();
            assertThat(result.isReordered()).isTrue();
            assertThat(result.getMemberRelocations()).isEmpty();
        }

        @ParameterizedTest
        @ValueSource(
                strings = {
                    "class UnchangedAnnotations {}",
                    "@Deprecated class UnchangedAnnotations {}",
                    "@Deprecated @SuppressWarnings(\"all\") class UnchangedAnnotations {}"
                })
        void sort_unchangedAnnotations_returnsFalse(@NonNull String srcCode) {
            // Given
            SpoonAstModel parsedModel = SrcAstTranslator.parse(
                            createSrcFile(srcCode, Path.of("UnchangedAnnotations.java")))
                    .getSpoonAstModel();

            // When
            SortingResult result = new Sorter(annotationConfig).sort(parsedModel);

            // Then
            assertThat(result.isAnnotationsReordered()).isFalse();
            assertThat(result.isMembersReordered()).isFalse();
            assertThat(result.isReordered()).isFalse();
        }
    }

    @Nested
    class SortingLifecycle {
        private static final String ALREADY_SORTED_MESSAGE = "already been submitted for sorting";

        @ParameterizedTest
        @ValueSource(booleans = {false, true})
        void sort_changedOrUnchangedSharedAst_rejectsEveryModelView(boolean firstSortChangesOrder) {
            // Given
            String membersInSrcOrder = firstSortChangesOrder ? "void execute() {} int value;" : "";
            SpoonAstModel model = SpoonParser.parseJavaSrcFile(createSrcFile(
                    "@Deprecated class SingleSortingAttempt { " + membersInSrcOrder + " }",
                    Path.of("SingleSortingAttempt.java")));
            SpoonAstModel preexistingModelView = model.withAnnotationSrcGroups(List.of());
            SortingResult firstResult = defaultSorter.sort(model);
            List<SpoonAstModel> sharedModelViews =
                    List.of(model, preexistingModelView, firstResult.getSortedSpoonAstModel());

            // When / Then
            sharedModelViews.forEach(sharedModelView -> assertThatThrownBy(() -> defaultSorter.sort(sharedModelView))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining(ALREADY_SORTED_MESSAGE)
                    .hasMessageContaining(model.getPath().toString()));
            assertThat(firstResult.isMembersReordered()).isEqualTo(firstSortChangesOrder);
        }

        @Test
        void sort_clonedConsumedUnit_rejectsRepeatedInvocation() {
            // Given
            SpoonAstModel model = SpoonParser.parseJavaSrcFile(
                    createSrcFile("class ClonedConsumedModel {}", Path.of("ClonedConsumedModel.java")));
            defaultSorter.sort(model);
            SpoonAstModel clonedModel = copyWithClonedCompilationUnit(model);

            // When
            Throwable repeatedFailure = catchThrowable(() -> defaultSorter.sort(clonedModel));

            // Then
            assertThat(clonedModel.getCompilationUnit()).isNotSameAs(model.getCompilationUnit());
            assertThat(repeatedFailure)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining(ALREADY_SORTED_MESSAGE);
        }

        @Test
        void sort_copiedUnusedUnit_keepsTemplateAvailable() {
            // Given
            SpoonAstModel templateModel = SpoonParser.parseJavaSrcFile(
                    createSrcFile("class UnusedCompilationUnitCopy {}", Path.of("UnusedCompilationUnitCopy.java")));
            SpoonAstModel copiedModel = copyWithClonedCompilationUnit(templateModel);
            defaultSorter.sort(copiedModel);

            // When
            SortingResult result = defaultSorter.sort(templateModel);

            // Then
            assertThat(copiedModel.getCompilationUnit()).isNotSameAs(templateModel.getCompilationUnit());
            assertThat(result.isReordered()).isFalse();
        }

        @Test
        void sort_failedInvocation_blocksRetry() {
            // Given
            SpoonAstModel model = SpoonParser.parseJavaSrcFile(
                    createSrcFile("class FailedSortingAttempt {}", Path.of("FailedSortingAttempt.java")));
            CompiledConfig failingConfig = mock(CompiledConfig.class);
            when(failingConfig.getTopLevelTypesOrdering())
                    .thenThrow(new IllegalStateException("Cannot resolve declaration order"));
            SpoonSorter failingSorter = new SpoonSorter(failingConfig);

            // When
            Throwable firstFailure = catchThrowable(() -> failingSorter.sortCompilationUnitRecursively(model));
            Throwable repeatedFailure = catchThrowable(() -> defaultSorter.sort(model));

            // Then
            assertThat(firstFailure)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("Cannot resolve declaration order");
            assertThat(repeatedFailure)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining(ALREADY_SORTED_MESSAGE);
        }

        @ParameterizedTest
        @ValueSource(booleans = {false, true})
        void sort_mixedSortingEntryPoints_rejectsSecondInvocation(boolean spoonSorterRunsFirst) {
            // Given
            SpoonAstModel model = SpoonParser.parseJavaSrcFile(
                    createSrcFile("class MixedSortingEntryPoints {}", Path.of("MixedSortingEntryPoints.java")));
            Consumer<SpoonAstModel> firstSortInvocation =
                    spoonSorterRunsFirst ? defaultSpoonSorter::sortCompilationUnitRecursively : defaultSorter::sort;
            Consumer<SpoonAstModel> repeatedSortInvocation =
                    spoonSorterRunsFirst ? defaultSorter::sort : defaultSpoonSorter::sortCompilationUnitRecursively;
            firstSortInvocation.accept(model);

            // When / Then
            assertThatThrownBy(() -> repeatedSortInvocation.accept(model))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining(ALREADY_SORTED_MESSAGE);
        }

        @Test
        void sort_reparsedSource_startsIndependentSortingLifecycle() {
            // Given
            SrcFile srcFile = createSrcFile(
                    "class ReparsedSortingModel { void execute() {} int value; }",
                    Path.of("ReparsedSortingModel.java"));
            SpoonAstModel firstModel = SpoonParser.parseJavaSrcFile(srcFile);
            defaultSorter.sort(firstModel);
            SpoonAstModel reparsedModel = SpoonParser.parseJavaSrcFile(srcFile);

            // When
            SortingResult result = defaultSorter.sort(reparsedModel);

            // Then
            assertThat(reparsedModel.getCompilationUnit()).isNotSameAs(firstModel.getCompilationUnit());
            assertThat(result.isMembersReordered()).isTrue();
        }
    }
}
