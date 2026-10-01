// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
package io.github.lemon_ant.jharmonizer.core.sorter.spoon;

import static io.github.lemon_ant.jharmonizer.core.sorter.spoon.SpoonAnnotationSorterTestUtils.sortAnnotationGroups;
import static io.github.lemon_ant.jharmonizer.core.testutils.SpoonTestCaseUtils.parseAstModelFromJavaFixtureResource;
import static io.github.lemon_ant.jharmonizer.core.testutils.TestCaseResourceUtils.readClasspathResourceAsString;
import static io.github.lemon_ant.jharmonizer.core.testutils.TestCaseResourceUtils.requireClasspathResourceUrl;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.groups.Tuple.tuple;

import io.github.lemon_ant.jharmonizer.core.config.compiled.CompiledConfig;
import io.github.lemon_ant.jharmonizer.core.config.compiled.Unified2CompiledModelCompiler;
import io.github.lemon_ant.jharmonizer.core.config.input.jharmonizer.JHarmonizerConfigurationManager;
import io.github.lemon_ant.jharmonizer.core.config.unified.AnnotationDescriptor;
import io.github.lemon_ant.jharmonizer.core.sorter.Sorter;
import io.github.lemon_ant.jharmonizer.core.sorter.SortingResult;
import io.github.lemon_ant.jharmonizer.core.sorter.spoon.OrderChangeCollector.ElementOrdering;
import io.github.lemon_ant.jharmonizer.core.spoon.AnnotationSourceScanner;
import io.github.lemon_ant.jharmonizer.core.spoon.AnnotationSrcGroup;
import io.github.lemon_ant.jharmonizer.core.spoon.AnnotationSrcGroup.AnnotationSrcFragment;
import io.github.lemon_ant.jharmonizer.core.spoon.AnnotationSrcGroup.AnnotationSrcGap;
import io.github.lemon_ant.jharmonizer.core.translator.spoon.SpoonAstModel;
import java.net.URL;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import spoon.reflect.cu.SourcePosition;
import spoon.reflect.declaration.CtAnnotation;
import spoon.reflect.declaration.CtType;

class SpoonAnnotationSorterTest {
    private static final URL FIXTURE = requireClasspathResourceUrl(
            "/test-cases/core/e2e/printer/scenarios/17-annotation-fragments/input/AnnotationFragmentPreservation.java");
    private static final URL GROUP_EXPECTED = requireClasspathResourceUrl(
            "/test-cases/core/e2e/printer/scenarios/21-annotation-group-gaps/expected/AnnotationGroupGapPreservation.java");
    private static final URL GROUP_INPUT = requireClasspathResourceUrl(
            "/test-cases/core/e2e/printer/scenarios/21-annotation-group-gaps/input/AnnotationGroupGapPreservation.java");

    private static CompiledConfig compiledConfig;
    private static List<String> expectedReplacementCodes;
    private static List<AnnotationSrcGroup> sourceGroups;

    @BeforeAll
    static void setUp() {
        compiledConfig =
                Unified2CompiledModelCompiler.compile(JHarmonizerConfigurationManager.parseUnifiedDefaultConfig());
        sourceGroups = AnnotationSourceScanner.scan(readClasspathResourceAsString(GROUP_INPUT));
        expectedReplacementCodes = AnnotationSourceScanner.scan(readClasspathResourceAsString(GROUP_EXPECTED)).stream()
                .map(AnnotationSrcGroup::getReplacementCode)
                .toList();
    }

    @ParameterizedTest
    @CsvSource({"0, 1, 2, false", "0, 2, 1, true", "1, 0, 2, true", "1, 2, 0, true", "2, 0, 1, true", "2, 1, 0, true"})
    void sort_annotationPermutations_reportsEveryChangedOrder(
            int firstSlotIndex, int secondSlotIndex, int thirdSlotIndex, boolean expectedReordering) {
        // Given
        AnnotationSrcGroup originalGroup = sourceGroups.getFirst();
        List<AnnotationSrcFragment> originalAnnotations = originalGroup.getAnnotationSrcFragments();
        List<String> namesInRequestedOrder = List.of(firstSlotIndex, secondSlotIndex, thirdSlotIndex).stream()
                .map(slotIndex ->
                        originalAnnotations.get(slotIndex).getDescriptor().getName())
                .toList();
        Comparator<AnnotationDescriptor> comparator =
                Comparator.comparingInt(descriptor -> namesInRequestedOrder.indexOf(descriptor.getName()));

        // When
        ElementOrdering<AnnotationSrcGroup> result =
                SpoonAnnotationSorter.sort(List.of(originalGroup), Set.of(), comparator);

        // Then
        assertThat(result.isReordered()).isEqualTo(expectedReordering);
        assertThat(result.getElementsInSortedOrder().getFirst().getAnnotationSrcFragments())
                .extracting(fragment -> fragment.getDescriptor().getName())
                .containsExactlyElementsOf(namesInRequestedOrder);
        if (!expectedReordering) {
            assertThat(result.getElementsInSortedOrder().getFirst()).isSameAs(originalGroup);
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void sort_annotationSrcGroups_preservesAstAnnotationsAndReturnsImmutableOrder(boolean sourcePositionAvailable) {
        // Given
        SpoonAstModel model = parseAstModelFromJavaFixtureResource(FIXTURE);
        CtType<?> type = model.getMainType().orElseThrow();
        if (!sourcePositionAvailable) {
            type.getAnnotations().get(0).setPosition(SourcePosition.NOPOSITION);
        }
        List<CtAnnotation<?>> originalAnnotations = List.copyOf(type.getAnnotations());
        List<SourcePosition> originalPositions =
                originalAnnotations.stream().map(CtAnnotation::getPosition).toList();
        List<AnnotationSrcGroup> originalGroups = model.getAnnotationSrcGroups();
        List<AnnotationSrcFragment> originalTypeAnnotations =
                originalGroups.get(0).getAnnotationSrcFragments();
        Sorter sorter = new Sorter(compiledConfig);

        // When
        SortingResult result = sorter.sort(model);

        // Then
        SpoonAstModel sortedModel = result.getSortedSpoonAstModel();
        assertThat(result.isAnnotationsReordered()).isTrue();
        assertThat(model.getAnnotationSrcGroups()).isSameAs(originalGroups);
        assertThat(sortedModel.getCompilationUnit()).isSameAs(model.getCompilationUnit());
        assertThat(type.getAnnotations()).containsExactlyElementsOf(originalAnnotations);
        assertThat(type.getAnnotations())
                .allSatisfy(annotation -> assertThat(annotation.getParent()).isSameAs(type));
        assertThat(originalAnnotations.stream().map(CtAnnotation::getPosition).toList())
                .containsExactlyElementsOf(originalPositions);
        assertThat(sortedModel.getAnnotationSrcGroups()).hasSameSizeAs(originalGroups);
        assertThat(sortedModel.getAnnotationSrcGroups().get(0).getAnnotationSrcFragments())
                .containsExactly(originalTypeAnnotations.get(1), originalTypeAnnotations.get(0));
        assertThat(sortedModel.getAnnotationSrcGroups().get(0).getStart())
                .isEqualTo(originalGroups.get(0).getStart());
        assertThat(sortedModel.getAnnotationSrcGroups().get(0).getEndExclusive())
                .isEqualTo(originalGroups.get(0).getEndExclusive());
        assertThat(sortedModel.getAnnotationSrcGroups().get(0).getAnnotationSrcGapsInSrcOrder())
                .extracting(AnnotationSrcGap::getStart, AnnotationSrcGap::getEndExclusive)
                .containsExactlyElementsOf(originalGroups.get(0).getAnnotationSrcGapsInSrcOrder().stream()
                        .map(fragment -> tuple(fragment.getStart(), fragment.getEndExclusive()))
                        .toList());
        assertThat(originalTypeAnnotations)
                .extracting(fragment -> fragment.getDescriptor().getName())
                .containsExactly("Zed", "Able");
        assertThat(sortedModel.getAnnotationSrcGroups().stream()
                        .flatMap(group -> group.getAnnotationSrcFragments().stream())
                        .toList())
                .containsExactlyInAnyOrderElementsOf(originalGroups.stream()
                        .flatMap(group -> group.getAnnotationSrcFragments().stream())
                        .toList());
        assertThatThrownBy(() -> sortedModel.getAnnotationSrcGroups().clear())
                .isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> sortedModel
                        .getAnnotationSrcGroups()
                        .get(0)
                        .getAnnotationSrcFragments()
                        .clear())
                .isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> sortedModel
                        .getAnnotationSrcGroups()
                        .get(0)
                        .getAnnotationSrcGapsInSrcOrder()
                        .clear())
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void sort_previouslySortedGroups_retainsOriginalGapOwnership() {
        // Given
        Comparator<AnnotationDescriptor> comparator = Comparator.comparing(AnnotationDescriptor::getName);
        List<AnnotationSrcGroup> ascendingGroups = sortAnnotationGroups(sourceGroups, comparator);
        List<String> ascendingReplacementCodes = ascendingGroups.stream()
                .map(AnnotationSrcGroup::getReplacementCode)
                .toList();
        List<AnnotationSrcGroup> descendingGroups = sortAnnotationGroups(ascendingGroups, comparator.reversed());
        List<String> descendingReplacementCodes = descendingGroups.stream()
                .map(AnnotationSrcGroup::getReplacementCode)
                .toList();

        // When
        List<AnnotationSrcGroup> sortedGroups = sortAnnotationGroups(descendingGroups, comparator);

        // Then
        assertThat(descendingReplacementCodes).isNotEqualTo(ascendingReplacementCodes);
        assertThat(sortedGroups).containsExactlyElementsOf(ascendingGroups);
        assertThat(sortedGroups)
                .extracting(AnnotationSrcGroup::getReplacementCode)
                .containsExactlyElementsOf(expectedReplacementCodes);
        assertThat(ascendingGroups)
                .extracting(AnnotationSrcGroup::getReplacementCode)
                .containsExactlyElementsOf(ascendingReplacementCodes);
    }

    @Test
    void sort_scannedGroups_providesSortedReplacementBeforePrinting() {
        // When
        ElementOrdering<AnnotationSrcGroup> result =
                SpoonAnnotationSorter.sort(sourceGroups, Set.of(), Comparator.comparing(AnnotationDescriptor::getName));

        // Then
        List<AnnotationSrcGroup> sortedGroups = result.getElementsInSortedOrder();
        assertThat(result.isReordered()).isTrue();
        assertThat(sortedGroups)
                .extracting(AnnotationSrcGroup::getReplacementCode)
                .containsExactlyElementsOf(expectedReplacementCodes);
        assertThat(sortedGroups)
                .extracting(AnnotationSrcGroup::getStart, AnnotationSrcGroup::getEndExclusive)
                .containsExactlyElementsOf(sourceGroups.stream()
                        .map(group -> tuple(group.getStart(), group.getEndExclusive()))
                        .toList());
    }
}
