// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
package io.github.lemon_ant.jharmonizer.core.sorter;

import static io.github.lemon_ant.jharmonizer.core.config.ConfigurationManager.loadDefaultConfig;
import static io.github.lemon_ant.jharmonizer.core.config.ConfigurationManager.overrideDefaultConfig;
import static io.github.lemon_ant.jharmonizer.core.config.input.jharmonizer.JHarmonizerConfigurationManager.parseFlexibleUnifiedConfigFromClasspathResource;
import static io.github.lemon_ant.jharmonizer.core.files_handler.SrcFileCreator.createSrcFile;
import static io.github.lemon_ant.jharmonizer.core.testutils.SpoonTestCaseUtils.parseAstModelFromJavaFixtureResource;
import static io.github.lemon_ant.jharmonizer.core.testutils.TestCaseResourceUtils.requireClasspathResourceUrl;
import static io.github.lemon_ant.jharmonizer.core.translator.spoon.MemberRelocationPrinter.printRelocations;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.lemon_ant.jharmonizer.core.translator.spoon.SpoonAstModel;
import io.github.lemon_ant.jharmonizer.core.translator.spoon.SpoonParser;
import java.net.URL;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
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
    private static final URL TOP_LEVEL_TYPES_CONFIG =
            requireClasspathResourceUrl("/test-cases/core/e2e/reorder/17-top-level-types-ordering/config.yml");
    private static final URL TOP_LEVEL_TYPES_FIXTURE = requireClasspathResourceUrl(
            "/test-cases/core/e2e/reorder/17-top-level-types-ordering/input/TopLevelTypesOrderingFixture.java");

    private Sorter annotationSortingDisabledSorter;
    private Sorter defaultSorter;
    private Sorter topLevelTypesSorter;

    @BeforeAll
    void setUp() {
        annotationSortingDisabledSorter = new Sorter(
                overrideDefaultConfig(parseFlexibleUnifiedConfigFromClasspathResource(DISABLED_ANNOTATIONS_CONFIG)));
        defaultSorter = new Sorter(loadDefaultConfig());
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
        assertThat(result.isMembersReordered()).isTrue();
        assertThat(result.getSortedSpoonAstModel()
                        .getAnnotationSrcGroups()
                        .getFirst()
                        .getAnnotationSrcFragments())
                .extracting(fragment -> fragment.getDescriptor().getName())
                .containsExactly("Deprecated", "SuppressWarnings");
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
    }

    @Test
    void sort_repeatedSortRestoresOriginalOrder_retainsEarlierReorderingFlag() {
        // Given
        SpoonAstModel model =
                SpoonParser.parseJavaSrcFile(createSrcFile("class Zeta {} class Alpha {}", Path.of("Zeta.java")));
        SortingResult firstResult = topLevelTypesSorter.sort(model);

        // When
        SortingResult secondResult = defaultSorter.sort(model);

        // Then
        assertThat(firstResult.isAnnotationsReordered()).isFalse();
        assertThat(firstResult.isMembersReordered()).isTrue();
        assertThat(secondResult.isAnnotationsReordered()).isFalse();
        assertThat(secondResult.isMembersReordered()).isFalse();
    }

    @Test
    void sort_repeatedSortWithAnnotationSortingDisabled_preservesEarlierAnnotationFlag() {
        // Given
        SpoonAstModel model = parseAstModelFromJavaFixtureResource(DISABLED_ANNOTATIONS_FIXTURE);
        SortingResult firstResult = defaultSorter.sort(model);

        // When
        SortingResult secondResult = annotationSortingDisabledSorter.sort(model);

        // Then
        assertThat(firstResult.isAnnotationsReordered()).isTrue();
        assertThat(firstResult.isMembersReordered()).isTrue();
        assertThat(secondResult.isAnnotationsReordered()).isFalse();
        assertThat(secondResult.getSortedSpoonAstModel().getAnnotationSrcGroups())
                .containsExactlyElementsOf(model.getAnnotationSrcGroups());
    }

    @Test
    void sort_repeatedSortWithDifferentConfiguration_preservesEarlierDiagnostics() {
        // Given
        SpoonAstModel baselineModel = parseAstModelFromJavaFixtureResource(TOP_LEVEL_TYPES_FIXTURE);
        String expectedReport = printRelocations(
                baselineModel.getPath(), topLevelTypesSorter.sort(baselineModel).getMemberRelocations());
        SpoonAstModel model = parseAstModelFromJavaFixtureResource(TOP_LEVEL_TYPES_FIXTURE);
        SortingResult firstResult = topLevelTypesSorter.sort(model);

        // When
        SortingResult secondResult = defaultSorter.sort(model);

        // Then
        assertThat(printRelocations(model.getPath(), firstResult.getMemberRelocations()))
                .isEqualTo(expectedReport);
        assertThat(printRelocations(model.getPath(), secondResult.getMemberRelocations()))
                .isNotEqualTo(expectedReport);
    }
}
