// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
package io.github.lemon_ant.jharmonizer.core.sorter.spoon;

import static io.github.lemon_ant.jharmonizer.core.sorter.spoon.SpoonAnnotationSorterTestUtils.findAnnotationOrder;
import static io.github.lemon_ant.jharmonizer.core.sorter.spoon.SpoonAnnotationSorterTestUtils.renderAnnotationGroup;
import static io.github.lemon_ant.jharmonizer.core.sorter.spoon.SpoonAnnotationSorterTestUtils.sortAnnotationGroups;
import static io.github.lemon_ant.jharmonizer.core.testutils.SpoonTestCaseUtils.parseAstModelFromJavaFixtureResource;
import static io.github.lemon_ant.jharmonizer.core.testutils.SpoonTestCaseUtils.scanAnnotationSrcGroups;
import static io.github.lemon_ant.jharmonizer.core.testutils.TestCaseResourceUtils.readClasspathResourceAsString;
import static io.github.lemon_ant.jharmonizer.core.testutils.TestCaseResourceUtils.requireClasspathResourceUrl;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.lemon_ant.jharmonizer.core.config.compiled.CompiledConfig;
import io.github.lemon_ant.jharmonizer.core.config.compiled.Unified2CompiledModelCompiler;
import io.github.lemon_ant.jharmonizer.core.config.input.jharmonizer.JHarmonizerConfigurationManager;
import io.github.lemon_ant.jharmonizer.core.config.unified.AnnotationDescriptor;
import io.github.lemon_ant.jharmonizer.core.sorter.Sorter;
import io.github.lemon_ant.jharmonizer.core.sorter.SortingResult;
import io.github.lemon_ant.jharmonizer.core.translator.spoon.SpoonAstModel;
import io.github.lemon_ant.jharmonizer.core.translator.spoon.annotation.AnnotationSourceScanner;
import io.github.lemon_ant.jharmonizer.core.translator.spoon.annotation.AnnotationSrcGroup;
import io.github.lemon_ant.jharmonizer.core.translator.spoon.annotation.AnnotationSrcGroup.AnnotationSrcFragment;
import java.net.URL;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.NonNull;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import spoon.reflect.cu.SourcePosition;
import spoon.reflect.declaration.CtAnnotation;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.declaration.CtField;
import spoon.reflect.declaration.CtType;
import spoon.reflect.visitor.CtScanner;
import spoon.reflect.visitor.filter.TypeFilter;

class SpoonAnnotationSorterTest {
    private static final URL EXTERNAL_INHERITED_ALIASES = requireClasspathResourceUrl(
            "/test-cases/core/sorter/spoon/invalid/ExternalInheritedAnnotationAliases.java");
    private static final URL FIXTURE = requireClasspathResourceUrl(
            "/test-cases/core/e2e/printer/scenarios/17-annotation-fragments/input/AnnotationFragmentPreservation.java");
    private static final URL GROUP_EXPECTED = requireClasspathResourceUrl(
            "/test-cases/core/e2e/printer/scenarios/21-annotation-group-gaps/expected/AnnotationGroupGapPreservation.java");
    private static final URL GROUP_INPUT = requireClasspathResourceUrl(
            "/test-cases/core/e2e/printer/scenarios/21-annotation-group-gaps/input/AnnotationGroupGapPreservation.java");
    private static final URL PARTIALLY_RESOLVED_ALIASES = requireClasspathResourceUrl(
            "/test-cases/core/sorter/spoon/invalid/PartiallyResolvedAnnotationAliases.java");
    private static final URL UNAVAILABLE_BINDINGS =
            requireClasspathResourceUrl("/test-cases/core/sorter/spoon/invalid/UnavailableAnnotationBindings.java");
    private static final URL UNRESOLVED_ANNOTATIONS =
            requireClasspathResourceUrl("/test-cases/core/sorter/spoon/invalid/UnresolvedAnnotationOrder.java");
    private static final URL VISIBLE_EXTERNAL_ANNOTATIONS =
            requireClasspathResourceUrl("/test-cases/core/sorter/spoon/invalid/VisibleExternalAnnotationOrder.java");

    private static CompiledConfig compiledConfig;
    private static List<String> expectedReplacementCodes;
    private static SpoonAstModel externalInheritedAliasesModel;
    private static SpoonAstModel partiallyResolvedAliasesModel;
    private static List<AnnotationSrcGroup> sourceGroups;
    private static List<AnnotationSrcGroup> unavailableBindingGroups;
    private static SpoonAstModel unresolvedAnnotationsModel;
    private static SpoonAstModel visibleExternalAnnotationsModel;

    @BeforeAll
    static void setUp() {
        compiledConfig =
                Unified2CompiledModelCompiler.compile(JHarmonizerConfigurationManager.parseUnifiedDefaultConfig());
        externalInheritedAliasesModel = parseAstModelFromJavaFixtureResource(EXTERNAL_INHERITED_ALIASES);
        sourceGroups = parseAstModelFromJavaFixtureResource(GROUP_INPUT).getOriginalAnnotationSrcGroups();
        expectedReplacementCodes = scanAnnotationSrcGroups(readClasspathResourceAsString(GROUP_EXPECTED)).stream()
                .map(AnnotationSrcGroup::getReplacementCode)
                .toList();
        partiallyResolvedAliasesModel = parseAstModelFromJavaFixtureResource(PARTIALLY_RESOLVED_ALIASES);
        visibleExternalAnnotationsModel = parseAstModelFromJavaFixtureResource(VISIBLE_EXTERNAL_ANNOTATIONS);
        unresolvedAnnotationsModel = parseAstModelFromJavaFixtureResource(UNRESOLVED_ANNOTATIONS);
        SpoonAstModel unavailableBindingsModel = parseAstModelFromJavaFixtureResource(UNAVAILABLE_BINDINGS);
        new CtScanner() {
            @Override
            protected void enter(@NonNull CtElement element) {
                if (element instanceof CtAnnotation<?> annotation) {
                    annotation.setPosition(SourcePosition.NOPOSITION);
                }
            }
        }.scan(unavailableBindingsModel.getCompilationUnit().getDeclaredTypes());
        unavailableBindingGroups = AnnotationSourceScanner.scan(
                readClasspathResourceAsString(UNAVAILABLE_BINDINGS), unavailableBindingsModel.getCompilationUnit());
    }

    @ParameterizedTest
    @CsvSource({"0, 1, 2, false", "0, 2, 1, true", "1, 0, 2, true", "1, 2, 0, true", "2, 0, 1, true", "2, 1, 0, true"})
    void sort_annotationPermutations_reportsEveryChangedOrder(
            int firstSlotIndex, int secondSlotIndex, int thirdSlotIndex, boolean expectedReordering) {
        // Given
        AnnotationSrcGroup originalGroup = sourceGroups.getFirst();
        List<AnnotationSrcFragment> originalAnnotations = originalGroup.getAnnotationSrcFragmentsInSrcOrder();
        List<String> namesInRequestedOrder = List.of(firstSlotIndex, secondSlotIndex, thirdSlotIndex).stream()
                .map(slotIndex ->
                        originalAnnotations.get(slotIndex).getDescriptor().getName())
                .toList();
        Comparator<AnnotationDescriptor> comparator =
                Comparator.comparingInt(descriptor -> namesInRequestedOrder.indexOf(descriptor.getName()));

        // When
        Map<Integer, List<AnnotationSrcFragment>> annotationOrderByGroupSrcStart =
                SpoonAnnotationSorter.sort(List.of(originalGroup), Set.of(), comparator);

        // Then
        assertThat(!annotationOrderByGroupSrcStart.isEmpty()).isEqualTo(expectedReordering);
        assertThat(findAnnotationOrder(originalGroup, annotationOrderByGroupSrcStart))
                .extracting(fragment -> fragment.getDescriptor().getName())
                .containsExactlyElementsOf(namesInRequestedOrder);
        if (!expectedReordering) {
            assertThat(findAnnotationOrder(originalGroup, annotationOrderByGroupSrcStart))
                    .isSameAs(originalAnnotations);
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
        List<AnnotationSrcGroup> originalGroups = model.getOriginalAnnotationSrcGroups();
        int originalGroupStart = originalGroups.get(0).getStart();
        int originalGroupEndExclusive = originalGroups.get(0).getEndExclusive();
        List<AnnotationSrcFragment> originalTypeAnnotations =
                originalGroups.get(0).getAnnotationSrcFragmentsInSrcOrder();
        Sorter sorter = new Sorter(compiledConfig);

        // When
        SortingResult result = sorter.sort(model);

        // Then
        Map<Integer, List<AnnotationSrcFragment>> sortedAnnotationSrcFragmentsByGroupSrcStart =
                result.getSortedAnnotationSrcFragmentsByGroupSrcStart();
        assertThat(result.isAnnotationsReordered()).isTrue();
        assertThat(model.getOriginalAnnotationSrcGroups()).isSameAs(originalGroups);
        assertThat(type.getAnnotations()).containsExactlyElementsOf(originalAnnotations);
        assertThat(type.getAnnotations())
                .allSatisfy(annotation -> assertThat(annotation.getParent()).isSameAs(type));
        assertThat(originalAnnotations.stream().map(CtAnnotation::getPosition).toList())
                .containsExactlyElementsOf(originalPositions);
        assertThat(findAnnotationOrder(originalGroups.get(0), sortedAnnotationSrcFragmentsByGroupSrcStart))
                .containsExactly(originalTypeAnnotations.get(1), originalTypeAnnotations.get(0));
        assertThat(model.getOriginalAnnotationSrcGroups().get(0).getStart()).isEqualTo(originalGroupStart);
        assertThat(model.getOriginalAnnotationSrcGroups().get(0).getEndExclusive())
                .isEqualTo(originalGroupEndExclusive);
        assertThat(originalTypeAnnotations)
                .extracting(fragment -> fragment.getDescriptor().getName())
                .containsExactly("Zed", "Able");
        assertThat(originalGroups.stream()
                        .flatMap(group ->
                                findAnnotationOrder(group, sortedAnnotationSrcFragmentsByGroupSrcStart).stream())
                        .toList())
                .containsExactlyInAnyOrderElementsOf(originalGroups.stream()
                        .flatMap(group -> group.getAnnotationSrcFragmentsInSrcOrder().stream())
                        .toList());
        assertThatThrownBy(() -> model.getOriginalAnnotationSrcGroups().clear())
                .isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> model.getOriginalAnnotationSrcGroups()
                        .get(0)
                        .getAnnotationSrcFragmentsInSrcOrder()
                        .clear())
                .isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> sortedAnnotationSrcFragmentsByGroupSrcStart.clear())
                .isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> sortedAnnotationSrcFragmentsByGroupSrcStart
                        .get(originalGroups.get(0).getStart())
                        .clear())
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"independentTypes", "omittedContainerValue", "bareContainer"})
    void sort_distinctExternalTypes_allowsIndependentOrdering(@NonNull String fieldName) {
        // Given
        AnnotationSrcGroup originalGroup = requireAnnotationGroupForField(visibleExternalAnnotationsModel, fieldName);
        List<AnnotationSrcFragment> originalAnnotations = originalGroup.getAnnotationSrcFragmentsInSrcOrder();
        Comparator<AnnotationDescriptor> comparator = Comparator.comparing(AnnotationDescriptor::getName)
                .thenComparing(AnnotationDescriptor::getArguments, Comparator.nullsFirst(Comparator.naturalOrder()));

        // When
        Map<Integer, List<AnnotationSrcFragment>> annotationOrderByGroupSrcStart =
                SpoonAnnotationSorter.sort(List.of(originalGroup), Set.of(), comparator);

        // Then
        assertThat(annotationOrderByGroupSrcStart).isNotEmpty();
        assertThat(findAnnotationOrder(originalGroup, annotationOrderByGroupSrcStart))
                .containsExactly(originalAnnotations.get(1), originalAnnotations.get(0));
    }

    @Test
    void sort_independentNestedTypes_reordersTypesWithinTheirContainerBounds() {
        // Given
        AnnotationSrcGroup originalGroup =
                requireAnnotationGroupForField(visibleExternalAnnotationsModel, "independentNestedTypes");
        List<AnnotationSrcFragment> originalAnnotations = originalGroup.getAnnotationSrcFragmentsInSrcOrder();

        // When
        Map<Integer, List<AnnotationSrcFragment>> annotationOrderByGroupSrcStart = SpoonAnnotationSorter.sort(
                List.of(originalGroup), Set.of(), Comparator.comparing(AnnotationDescriptor::getName));

        // Then
        assertThat(annotationOrderByGroupSrcStart).isNotEmpty();
        assertThat(findAnnotationOrder(originalGroup, annotationOrderByGroupSrcStart))
                .containsExactly(
                        originalAnnotations.get(4),
                        originalAnnotations.get(2),
                        originalAnnotations.get(1),
                        originalAnnotations.get(3),
                        originalAnnotations.get(0));
    }

    @Test
    void sort_independentPermutations_retainsOriginalGapOwnership() {
        // Given
        Comparator<AnnotationDescriptor> comparator = Comparator.comparing(AnnotationDescriptor::getName);
        List<String> originalReplacementCodes = sourceGroups.stream()
                .map(AnnotationSrcGroup::getReplacementCode)
                .toList();
        Map<Integer, List<AnnotationSrcFragment>> ascendingOrders = sortAnnotationGroups(sourceGroups, comparator);
        List<String> ascendingReplacementCodes = sourceGroups.stream()
                .map(group -> renderAnnotationGroup(group, ascendingOrders))
                .toList();
        Map<Integer, List<AnnotationSrcFragment>> descendingOrders =
                sortAnnotationGroups(sourceGroups, comparator.reversed());
        List<String> descendingReplacementCodes = sourceGroups.stream()
                .map(group -> renderAnnotationGroup(group, descendingOrders))
                .toList();

        // When
        Map<Integer, List<AnnotationSrcFragment>> repeatedAscendingOrders =
                sortAnnotationGroups(sourceGroups, comparator);

        // Then
        assertThat(descendingReplacementCodes).isNotEqualTo(ascendingReplacementCodes);
        assertThat(repeatedAscendingOrders).isEqualTo(ascendingOrders);
        assertThat(sourceGroups.stream()
                        .map(group -> renderAnnotationGroup(group, repeatedAscendingOrders))
                        .toList())
                .containsExactlyElementsOf(expectedReplacementCodes);
        assertThat(sourceGroups.stream()
                        .map(group -> renderAnnotationGroup(group, ascendingOrders))
                        .toList())
                .containsExactlyElementsOf(ascendingReplacementCodes);
        assertThat(sourceGroups)
                .extracting(AnnotationSrcGroup::getReplacementCode)
                .containsExactlyElementsOf(originalReplacementCodes);
    }

    @ParameterizedTest
    @CsvSource({"lexicalContainerFirst, false", "lexicalContainerLast, true", "lexicalExternalContainerFirst, false"})
    void sort_lexicalRepeatableAliases_preservesBothSourceDeclaredContainerOrders(
            @NonNull String fieldName, boolean argumentsFirst) {
        // Given
        AnnotationSrcGroup originalGroup = requireAnnotationGroupForField(partiallyResolvedAliasesModel, fieldName);
        List<AnnotationSrcFragment> originalAnnotations = originalGroup.getAnnotationSrcFragmentsInSrcOrder();
        Comparator<AnnotationDescriptor> nameComparator = Comparator.comparing(AnnotationDescriptor::getName);
        Comparator<AnnotationDescriptor> argumentComparator = Comparator.comparing(
                AnnotationDescriptor::getArguments, Comparator.nullsFirst(Comparator.naturalOrder()));
        Comparator<AnnotationDescriptor> comparator = argumentsFirst
                ? argumentComparator.thenComparing(nameComparator)
                : nameComparator.thenComparing(argumentComparator);

        // When
        Map<Integer, List<AnnotationSrcFragment>> annotationOrderByGroupSrcStart =
                SpoonAnnotationSorter.sort(List.of(originalGroup), Set.of(), comparator);

        // Then
        assertThat(annotationOrderByGroupSrcStart).isNotEmpty();
        assertThat(findAnnotationOrder(originalGroup, annotationOrderByGroupSrcStart))
                .containsExactly(
                        originalAnnotations.get(3),
                        originalAnnotations.get(1),
                        originalAnnotations.get(2),
                        originalAnnotations.get(0));
    }

    @Test
    void sort_scannedGroups_rendersSortedReplacementFromSelectedOrder() {
        // When
        Map<Integer, List<AnnotationSrcFragment>> annotationOrderByGroupSrcStart =
                SpoonAnnotationSorter.sort(sourceGroups, Set.of(), Comparator.comparing(AnnotationDescriptor::getName));

        // Then
        assertThat(annotationOrderByGroupSrcStart).isNotEmpty();
        assertThat(sourceGroups.stream()
                        .map(group -> renderAnnotationGroup(group, annotationOrderByGroupSrcStart))
                        .toList())
                .containsExactlyElementsOf(expectedReplacementCodes);
        assertThat(annotationOrderByGroupSrcStart.keySet())
                .isSubsetOf(
                        sourceGroups.stream().map(AnnotationSrcGroup::getStart).toList());
    }

    @Test
    void sort_transitiveVisibleOverlaps_preservesContainerAndBothAnnotationSequences() {
        // Given
        AnnotationSrcGroup originalGroup =
                requireAnnotationGroupForField(visibleExternalAnnotationsModel, "transitiveOverlap");
        List<AnnotationSrcFragment> originalAnnotations = originalGroup.getAnnotationSrcFragmentsInSrcOrder();

        // When
        Map<Integer, List<AnnotationSrcFragment>> annotationOrderByGroupSrcStart = SpoonAnnotationSorter.sort(
                List.of(originalGroup), Set.of(), Comparator.comparing(AnnotationDescriptor::getName));

        // Then
        assertThat(annotationOrderByGroupSrcStart).isNotEmpty();
        assertThat(findAnnotationOrder(originalGroup, annotationOrderByGroupSrcStart))
                .containsExactly(
                        originalAnnotations.get(4),
                        originalAnnotations.get(1),
                        originalAnnotations.get(2),
                        originalAnnotations.get(3),
                        originalAnnotations.get(0));
    }

    @Test
    void sort_unavailableArrayAndReceiverBindings_sortsUnrelatedTypes() {
        // When
        Map<Integer, List<AnnotationSrcFragment>> annotationOrderByGroupSrcStart = SpoonAnnotationSorter.sort(
                unavailableBindingGroups, Set.of(), Comparator.comparing(AnnotationDescriptor::getName));

        // Then
        assertThat(unavailableBindingGroups).hasSize(4);
        assertThat(annotationOrderByGroupSrcStart).isNotEmpty();
        assertThat(unavailableBindingGroups.subList(0, 2))
                .allSatisfy(group -> assertThat(findAnnotationOrder(group, annotationOrderByGroupSrcStart))
                        .extracting(fragment -> fragment.getDescriptor().getName())
                        .containsExactly("Deprecated", "Documented"));
        assertThat(unavailableBindingGroups.subList(2, 4))
                .allSatisfy(group -> assertThat(findAnnotationOrder(group, annotationOrderByGroupSrcStart))
                        .extracting(fragment -> fragment.getDescriptor().getName())
                        .containsExactly("Tag", "Tags"));
    }

    @Test
    void sort_unprovenExternalHomonyms_preservesRepeatedOrderAndSortsUnrelatedTypes() {
        // Given
        AnnotationSrcGroup originalGroup =
                requireAnnotationGroupForField(visibleExternalAnnotationsModel, "homonymousTypes");
        List<AnnotationSrcFragment> originalAnnotations = originalGroup.getAnnotationSrcFragmentsInSrcOrder();
        Comparator<AnnotationDescriptor> comparator = Comparator.comparing(AnnotationDescriptor::getName)
                .thenComparing(AnnotationDescriptor::getArguments, Comparator.nullsFirst(Comparator.naturalOrder()));

        // When
        Map<Integer, List<AnnotationSrcFragment>> annotationOrderByGroupSrcStart =
                SpoonAnnotationSorter.sort(List.of(originalGroup), Set.of(), comparator);

        // Then
        assertThat(annotationOrderByGroupSrcStart).isNotEmpty();
        assertThat(findAnnotationOrder(originalGroup, annotationOrderByGroupSrcStart))
                .containsExactly(
                        originalAnnotations.get(3),
                        originalAnnotations.get(1),
                        originalAnnotations.get(2),
                        originalAnnotations.get(0));
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                "typeImportOwnerAlias",
                "importedOwnerAlias",
                "staticImportOwnerAlias",
                "directAndNestedOwnerAlias",
                "nestedOwnerAliases",
                "qualifiedOwnerAliases"
            })
    void sort_unprovenExternalOwnerAliases_preservesRepeatedOrderAndSortsUnrelatedTypes(@NonNull String fieldName) {
        // Given
        AnnotationSrcGroup originalGroup = requireAnnotationGroupForField(externalInheritedAliasesModel, fieldName);
        List<AnnotationSrcFragment> originalAnnotations = originalGroup.getAnnotationSrcFragmentsInSrcOrder();
        Comparator<AnnotationDescriptor> comparator = Comparator.comparing(AnnotationDescriptor::getName)
                .thenComparing(AnnotationDescriptor::getArguments, Comparator.nullsFirst(Comparator.naturalOrder()));

        // When
        Map<Integer, List<AnnotationSrcFragment>> annotationOrderByGroupSrcStart =
                SpoonAnnotationSorter.sort(List.of(originalGroup), Set.of(), comparator);

        // Then
        assertThat(annotationOrderByGroupSrcStart).isNotEmpty();
        assertThat(findAnnotationOrder(originalGroup, annotationOrderByGroupSrcStart))
                .containsExactly(
                        originalAnnotations.get(3),
                        originalAnnotations.get(1),
                        originalAnnotations.get(2),
                        originalAnnotations.get(0));
    }

    @ParameterizedTest
    @CsvSource({"independentShortNames, 'Able, Zed'", "mixedNames, 'Able, Tag, Zed'"})
    void sort_unresolvedDistinctNames_sortsUnrelatedTypes(@NonNull String fieldName, @NonNull String expectedNames) {
        AnnotationSrcGroup originalGroup = requireAnnotationGroupForField(unresolvedAnnotationsModel, fieldName);

        // When
        Map<Integer, List<AnnotationSrcFragment>> annotationOrderByGroupSrcStart = SpoonAnnotationSorter.sort(
                List.of(originalGroup), Set.of(), Comparator.comparing(AnnotationDescriptor::getName));

        // Then
        assertThat(annotationOrderByGroupSrcStart).isNotEmpty();
        assertThat(findAnnotationOrder(originalGroup, annotationOrderByGroupSrcStart))
                .extracting(fragment -> fragment.getDescriptor().getName())
                .containsExactly(expectedNames.split(", "));
    }

    @ParameterizedTest
    @CsvSource({"unresolvedContainerValue, 3, 1, 2, 0", "repeatedShortNames, 2, 1, 3, 0"})
    void sort_unresolvedOverlappingNames_preservesRepeatedOrderAndSortsUnrelatedTypes(
            @NonNull String fieldName,
            int firstSlotIndex,
            int secondSlotIndex,
            int thirdSlotIndex,
            int fourthSlotIndex) {
        // Given
        AnnotationSrcGroup originalGroup = requireAnnotationGroupForField(unresolvedAnnotationsModel, fieldName);
        List<AnnotationSrcFragment> originalAnnotations = originalGroup.getAnnotationSrcFragmentsInSrcOrder();
        Comparator<AnnotationDescriptor> comparator = Comparator.comparing(AnnotationDescriptor::getName)
                .thenComparing(AnnotationDescriptor::getArguments, Comparator.nullsFirst(Comparator.naturalOrder()));

        // When
        Map<Integer, List<AnnotationSrcFragment>> annotationOrderByGroupSrcStart =
                SpoonAnnotationSorter.sort(List.of(originalGroup), Set.of(), comparator);

        // Then
        assertThat(annotationOrderByGroupSrcStart).isNotEmpty();
        assertThat(findAnnotationOrder(originalGroup, annotationOrderByGroupSrcStart))
                .containsExactly(
                        originalAnnotations.get(firstSlotIndex),
                        originalAnnotations.get(secondSlotIndex),
                        originalAnnotations.get(thirdSlotIndex),
                        originalAnnotations.get(fourthSlotIndex));
    }

    @Test
    void sort_unresolvedOwnerAlias_preservesDependenciesToDistinctSortableTypes() {
        // Given
        AnnotationSrcGroup originalGroup =
                requireAnnotationGroupForField(partiallyResolvedAliasesModel, "ambiguousThirdOwner");
        List<AnnotationSrcFragment> originalAnnotations = originalGroup.getAnnotationSrcFragmentsInSrcOrder();
        Comparator<AnnotationDescriptor> comparator = Comparator.comparing(AnnotationDescriptor::getName)
                .thenComparing(AnnotationDescriptor::getArguments, Comparator.nullsFirst(Comparator.naturalOrder()));

        // When
        Map<Integer, List<AnnotationSrcFragment>> annotationOrderByGroupSrcStart =
                SpoonAnnotationSorter.sort(List.of(originalGroup), Set.of(), comparator);

        // Then
        assertThat(annotationOrderByGroupSrcStart).isNotEmpty();
        assertThat(findAnnotationOrder(originalGroup, annotationOrderByGroupSrcStart))
                .containsExactly(
                        originalAnnotations.get(4),
                        originalAnnotations.get(2),
                        originalAnnotations.get(1),
                        originalAnnotations.get(3),
                        originalAnnotations.get(0));
    }

    @Test
    void sort_unresolvedRepeatableAlias_preservesSourceDeclaredDefaultedContainerOrder() {
        // Given
        AnnotationSrcGroup originalGroup =
                requireAnnotationGroupForField(partiallyResolvedAliasesModel, "defaultedContainerFirst");
        List<AnnotationSrcFragment> originalAnnotations = originalGroup.getAnnotationSrcFragmentsInSrcOrder();

        // When
        Map<Integer, List<AnnotationSrcFragment>> annotationOrderByGroupSrcStart = SpoonAnnotationSorter.sort(
                List.of(originalGroup), Set.of(), Comparator.comparing(AnnotationDescriptor::getName));

        // Then
        assertThat(annotationOrderByGroupSrcStart).isNotEmpty();
        assertThat(findAnnotationOrder(originalGroup, annotationOrderByGroupSrcStart))
                .containsExactly(
                        originalAnnotations.get(3),
                        originalAnnotations.get(1),
                        originalAnnotations.get(2),
                        originalAnnotations.get(0));
    }

    @Test
    void sort_unresolvedWildcardIdentity_preservesRepeatedOrderAndSortsUnrelatedTypes() {
        // Given
        AnnotationSrcGroup originalGroup =
                requireAnnotationGroupForField(visibleExternalAnnotationsModel, "ambiguousWildcard");
        List<AnnotationSrcFragment> originalAnnotations = originalGroup.getAnnotationSrcFragmentsInSrcOrder();
        Comparator<AnnotationDescriptor> comparator = Comparator.comparing(AnnotationDescriptor::getName)
                .thenComparing(AnnotationDescriptor::getArguments, Comparator.nullsFirst(Comparator.naturalOrder()));

        // When
        Map<Integer, List<AnnotationSrcFragment>> annotationOrderByGroupSrcStart =
                SpoonAnnotationSorter.sort(List.of(originalGroup), Set.of(), comparator);

        // Then
        assertThat(annotationOrderByGroupSrcStart).isNotEmpty();
        assertThat(findAnnotationOrder(originalGroup, annotationOrderByGroupSrcStart))
                .containsExactly(
                        originalAnnotations.get(3),
                        originalAnnotations.get(1),
                        originalAnnotations.get(2),
                        originalAnnotations.get(0));
    }

    @ParameterizedTest
    @CsvSource({
        "directRepeated, 2, 1, 3, 0",
        "explicitContainerFirst, 3, 1, 2, 0",
        "explicitDirectFirst, 3, 1, 2, 0",
        "nestedContainer, 3, 1, 2, 0"
    })
    void sort_visibleExternalAnnotationNames_preservesRepeatedOrderAndSortsUnrelatedTypes(
            @NonNull String fieldName,
            int firstSlotIndex,
            int secondSlotIndex,
            int thirdSlotIndex,
            int fourthSlotIndex) {
        // Given
        AnnotationSrcGroup originalGroup = requireAnnotationGroupForField(visibleExternalAnnotationsModel, fieldName);
        List<AnnotationSrcFragment> originalAnnotations = originalGroup.getAnnotationSrcFragmentsInSrcOrder();
        Comparator<AnnotationDescriptor> comparator = Comparator.comparing(AnnotationDescriptor::getName)
                .thenComparing(AnnotationDescriptor::getArguments, Comparator.nullsFirst(Comparator.naturalOrder()));

        // When
        Map<Integer, List<AnnotationSrcFragment>> annotationOrderByGroupSrcStart =
                SpoonAnnotationSorter.sort(List.of(originalGroup), Set.of(), comparator);

        // Then
        assertThat(annotationOrderByGroupSrcStart).isNotEmpty();
        assertThat(findAnnotationOrder(originalGroup, annotationOrderByGroupSrcStart))
                .containsExactly(
                        originalAnnotations.get(firstSlotIndex),
                        originalAnnotations.get(secondSlotIndex),
                        originalAnnotations.get(thirdSlotIndex),
                        originalAnnotations.get(fourthSlotIndex));
        assertThat(originalGroup.getAnnotationSrcFragmentsInSrcOrder()).containsExactlyElementsOf(originalAnnotations);
    }

    @Test
    void sort_writtenQualifiedNames_sortsWithoutTypeDeclarations() {
        AnnotationSrcGroup originalGroup = scanAnnotationSrcGroups("@external.Zed @other.Able class Sample {}")
                .getFirst();

        // When
        Map<Integer, List<AnnotationSrcFragment>> annotationOrderByGroupSrcStart = SpoonAnnotationSorter.sort(
                List.of(originalGroup), Set.of(), Comparator.comparing(AnnotationDescriptor::getName));

        // Then
        assertThat(annotationOrderByGroupSrcStart).isNotEmpty();
        assertThat(renderAnnotationGroup(originalGroup, annotationOrderByGroupSrcStart))
                .isEqualTo("@other.Able @external.Zed ");
    }

    @NonNull
    private static AnnotationSrcGroup requireAnnotationGroupForField(SpoonAstModel model, String fieldName) {
        CtType<?> mainType = model.getMainType().orElseThrow();
        List<CtField<?>> matchingFields = mainType.getElements(new TypeFilter<CtField<?>>(CtField.class)).stream()
                .filter(field -> fieldName.equals(field.getSimpleName()))
                .toList();
        if (matchingFields.size() != 1) {
            throw new IllegalStateException(
                    "Expected one fixture field: " + fieldName + "; found " + matchingFields.size());
        }
        SourcePosition fieldPosition = matchingFields.getFirst().getPosition();
        return model.getOriginalAnnotationSrcGroups().stream()
                .filter(group -> group.getStart() >= fieldPosition.getSourceStart()
                        && group.getEndExclusive() <= fieldPosition.getSourceEnd() + 1)
                .findFirst()
                .orElseThrow(
                        () -> new IllegalStateException("Missing annotation group for fixture field: " + fieldName));
    }
}
