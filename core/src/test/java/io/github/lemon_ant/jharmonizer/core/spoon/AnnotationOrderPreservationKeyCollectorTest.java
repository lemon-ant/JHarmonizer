// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
package io.github.lemon_ant.jharmonizer.core.spoon;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class AnnotationOrderPreservationKeyCollectorTest {

    @Test
    void collect_candidatesAlreadyPresent_reusesInputSets() {
        // Given
        String confirmedTypeName = "p.Owner.Tag";
        String unconfirmedTypeKey = "?.Alias.Tag";
        String unrelatedTypeName = "p.Marker";
        Set<String> coveredGroupKeys = Set.of(confirmedTypeName, unconfirmedTypeKey);
        Set<String> unrelatedGroupKeys = Set.of(unrelatedTypeName);
        List<Set<String>> groupKeysPerAnnotationInSrcOrder = List.of(coveredGroupKeys, unrelatedGroupKeys);
        Map<String, String> rootGroupKeyByAnnotationTypeName = Map.of(
                confirmedTypeName, confirmedTypeName,
                unrelatedTypeName, unrelatedTypeName);

        // When
        List<Set<String>> expandedKeysPerAnnotationInSrcOrder = AnnotationOrderPreservationKeyCollector.collect(
                rootGroupKeyByAnnotationTypeName, groupKeysPerAnnotationInSrcOrder);

        // Then
        assertThat(expandedKeysPerAnnotationInSrcOrder).containsExactlyElementsOf(groupKeysPerAnnotationInSrcOrder);
        assertThat(expandedKeysPerAnnotationInSrcOrder.getFirst()).isSameAs(coveredGroupKeys);
        assertThat(expandedKeysPerAnnotationInSrcOrder.getLast()).isSameAs(unrelatedGroupKeys);
    }

    @Test
    void collect_canonicalInputKeyMissingFromCache_rejectsIncompleteIndex() {
        List<Set<String>> groupKeysPerAnnotationInSrcOrder = List.of(Set.of("p.Missing.Tag"));

        // When / Then
        assertThatIllegalStateException()
                .isThrownBy(() ->
                        AnnotationOrderPreservationKeyCollector.collect(Map.of(), groupKeysPerAnnotationInSrcOrder))
                .withMessage("No indexed annotation type names for group key: p.Missing.Tag");
    }

    @Test
    void collect_confirmedContainerHomonyms_keepsIndependentKeys() {
        // Given
        String importedContainerKey = "library.Tags";
        String sourceContainerKey = "p.Owner.Tags";
        List<Set<String>> groupKeysPerAnnotationInSrcOrder =
                List.of(Set.of(importedContainerKey), Set.of(sourceContainerKey));
        Map<String, String> rootGroupKeyByAnnotationTypeName = Map.of(
                "p.FirstTag",
                importedContainerKey,
                importedContainerKey,
                importedContainerKey,
                "p.SecondTag",
                sourceContainerKey,
                sourceContainerKey,
                sourceContainerKey);

        // When
        List<Set<String>> expandedKeysPerAnnotationInSrcOrder = AnnotationOrderPreservationKeyCollector.collect(
                rootGroupKeyByAnnotationTypeName, groupKeysPerAnnotationInSrcOrder);

        // Then
        assertThat(expandedKeysPerAnnotationInSrcOrder).isSameAs(groupKeysPerAnnotationInSrcOrder);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 2})
    void collect_confirmedDisjointGroups_reusesInputList(int inputGroupCount) {
        // Given
        String firstConfirmedTypeName = "p.First.Tag";
        String secondConfirmedTypeName = "p.Second.Tag";
        List<Set<String>> groupKeysPerAnnotationInSrcOrder = List.of(
                        Set.of(firstConfirmedTypeName), Set.of(secondConfirmedTypeName))
                .subList(0, inputGroupCount);
        Map<String, String> rootGroupKeyByAnnotationTypeName = Map.of(
                firstConfirmedTypeName, firstConfirmedTypeName,
                secondConfirmedTypeName, secondConfirmedTypeName);

        // When
        List<Set<String>> expandedKeysPerAnnotationInSrcOrder = AnnotationOrderPreservationKeyCollector.collect(
                rootGroupKeyByAnnotationTypeName, groupKeysPerAnnotationInSrcOrder);

        // Then
        assertThat(expandedKeysPerAnnotationInSrcOrder).isSameAs(groupKeysPerAnnotationInSrcOrder);
    }

    @ParameterizedTest
    @CsvSource({"false, false", "false, true", "true, false", "true, true"})
    void collect_inputIterationOrders_keepsCandidateUnions(
            boolean reverseAnnotationOrder, boolean reverseGroupKeyOrder) {
        // Given
        String firstConfirmedTypeName = "p.First.Tag";
        String secondConfirmedTypeName = "p.Second.Tag";
        String unconfirmedTypeKey = "?.Alias.Tag";
        List<Set<String>> groupKeysPerAnnotationInSrcOrder = Stream.of(
                        List.of(firstConfirmedTypeName, secondConfirmedTypeName),
                        List.of(unconfirmedTypeKey, firstConfirmedTypeName),
                        List.of(unconfirmedTypeKey))
                .map(groupKeys -> reverseGroupKeyOrder ? groupKeys.reversed() : groupKeys)
                .map(groupKeys -> Collections.unmodifiableSet(new LinkedHashSet<>(groupKeys)))
                .toList();
        List<Set<String>> expectedKeysPerAnnotationInSrcOrder = List.of(
                Set.of(firstConfirmedTypeName, secondConfirmedTypeName),
                Set.of(unconfirmedTypeKey, firstConfirmedTypeName, secondConfirmedTypeName),
                Set.of(unconfirmedTypeKey, firstConfirmedTypeName, secondConfirmedTypeName));
        if (reverseAnnotationOrder) {
            groupKeysPerAnnotationInSrcOrder = groupKeysPerAnnotationInSrcOrder.reversed();
            expectedKeysPerAnnotationInSrcOrder = expectedKeysPerAnnotationInSrcOrder.reversed();
        }
        Map<String, String> rootGroupKeyByAnnotationTypeName = Map.of(
                firstConfirmedTypeName, firstConfirmedTypeName,
                secondConfirmedTypeName, secondConfirmedTypeName);

        // When
        List<Set<String>> expandedKeysPerAnnotationInSrcOrder = AnnotationOrderPreservationKeyCollector.collect(
                rootGroupKeyByAnnotationTypeName, groupKeysPerAnnotationInSrcOrder);

        // Then
        assertThat(expandedKeysPerAnnotationInSrcOrder).containsExactlyElementsOf(expectedKeysPerAnnotationInSrcOrder);
    }

    @Test
    void collect_missingTerminalCacheEntry_rejectsIncompleteContainerChain() {
        // Given
        List<Set<String>> groupKeysPerAnnotationInSrcOrder = List.of(Set.of("library.Tags"));
        Map<String, String> incompleteContainerChain = Map.of("p.Tag", "library.Tags");

        // When / Then
        assertThatIllegalStateException()
                .isThrownBy(() -> AnnotationOrderPreservationKeyCollector.collect(
                        incompleteContainerChain, groupKeysPerAnnotationInSrcOrder))
                .withMessage("No indexed annotation type names for group key: library.Tags");
    }

    @Test
    void collect_partiallyRelatedGroups_reusesUnchangedSetsAndProtectsExpandedKeys() {
        // Given
        String confirmedTypeName = "p.Owner.Tag";
        String unconfirmedTypeKey = "?.Alias.Tag";
        String unrelatedTypeName = "p.Marker";
        Set<String> confirmedGroupKeys = Set.of(confirmedTypeName);
        Set<String> unrelatedGroupKeys = Set.of(unrelatedTypeName);
        List<Set<String>> groupKeysPerAnnotationInSrcOrder =
                List.of(confirmedGroupKeys, Set.of(unconfirmedTypeKey), unrelatedGroupKeys);
        Map<String, String> rootGroupKeyByAnnotationTypeName = Map.of(
                confirmedTypeName, confirmedTypeName,
                unrelatedTypeName, unrelatedTypeName);

        // When
        List<Set<String>> expandedKeysPerAnnotationInSrcOrder = AnnotationOrderPreservationKeyCollector.collect(
                rootGroupKeyByAnnotationTypeName, groupKeysPerAnnotationInSrcOrder);

        // Then
        assertThat(expandedKeysPerAnnotationInSrcOrder)
                .isNotSameAs(groupKeysPerAnnotationInSrcOrder)
                .containsExactly(confirmedGroupKeys, Set.of(unconfirmedTypeKey, confirmedTypeName), unrelatedGroupKeys);
        assertThat(expandedKeysPerAnnotationInSrcOrder.getFirst()).isSameAs(confirmedGroupKeys);
        assertThat(expandedKeysPerAnnotationInSrcOrder.get(2)).isSameAs(unrelatedGroupKeys);
        assertThat(groupKeysPerAnnotationInSrcOrder)
                .containsExactly(Set.of(confirmedTypeName), Set.of(unconfirmedTypeKey), Set.of(unrelatedTypeName));
        assertThatThrownBy(expandedKeysPerAnnotationInSrcOrder::clear)
                .isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(expandedKeysPerAnnotationInSrcOrder.getFirst()::clear)
                .isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(expandedKeysPerAnnotationInSrcOrder.get(1)::clear)
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void collect_uncachedLexicalHomonyms_sharesCandidateKeys() {
        // Given
        String firstLexicalKey = "?.First.Tag";
        String secondLexicalKey = "?.Second.Tag";
        List<Set<String>> groupKeysPerAnnotationInSrcOrder = List.of(Set.of(firstLexicalKey), Set.of(secondLexicalKey));

        // When
        List<Set<String>> expandedKeysPerAnnotationInSrcOrder =
                AnnotationOrderPreservationKeyCollector.collect(Map.of(), groupKeysPerAnnotationInSrcOrder);

        // Then
        assertThat(expandedKeysPerAnnotationInSrcOrder)
                .containsExactly(Set.of(firstLexicalKey, secondLexicalKey), Set.of(firstLexicalKey, secondLexicalKey));
    }

    @Test
    void collect_uncachedLexicalSingleton_reusesInputList() {
        List<Set<String>> groupKeysPerAnnotationInSrcOrder = List.of(Set.of("?.Missing.Tag"));

        // When
        List<Set<String>> expandedKeysPerAnnotationInSrcOrder =
                AnnotationOrderPreservationKeyCollector.collect(Map.of(), groupKeysPerAnnotationInSrcOrder);

        // Then
        assertThat(expandedKeysPerAnnotationInSrcOrder).isSameAs(groupKeysPerAnnotationInSrcOrder);
    }

    @Test
    void collect_uncachedRepeatableRoot_linksSourceAndContainerNames() {
        // Given
        String annotationKey = "?.Tag";
        String containerKey = "?.Tags";
        String rootContainerKey = "?.library.Tags";
        List<Set<String>> groupKeysPerAnnotationInSrcOrder = List.of(Set.of(annotationKey), Set.of(containerKey));
        Map<String, String> rootGroupKeyByAnnotationTypeName = Map.of("p.Tag", rootContainerKey);

        // When
        List<Set<String>> expandedKeysPerAnnotationInSrcOrder = AnnotationOrderPreservationKeyCollector.collect(
                rootGroupKeyByAnnotationTypeName, groupKeysPerAnnotationInSrcOrder);

        // Then
        assertThat(expandedKeysPerAnnotationInSrcOrder)
                .containsExactly(Set.of(annotationKey, rootContainerKey), Set.of(containerKey, rootContainerKey));
    }

    @Test
    void collect_unconfirmedExternalContainerSingleton_reusesInputList() {
        // Given
        List<Set<String>> groupKeysPerAnnotationInSrcOrder = List.of(Set.of("?.library.Tags"));
        Map<String, String> rootGroupKeyByAnnotationTypeName = Map.of("p.Tag", "?.library.Tags");

        // When
        List<Set<String>> expandedKeysPerAnnotationInSrcOrder = AnnotationOrderPreservationKeyCollector.collect(
                rootGroupKeyByAnnotationTypeName, groupKeysPerAnnotationInSrcOrder);

        // Then
        assertThat(expandedKeysPerAnnotationInSrcOrder).isSameAs(groupKeysPerAnnotationInSrcOrder);
    }

    @Test
    void collect_unconfirmedHomonym_linksConfirmedTypesWithoutMergingThem() {
        // Given
        String firstConfirmedTypeName = "p.First.Tag";
        String secondConfirmedTypeName = "p.Second.Tag";
        String unconfirmedTypeKey = "?.Alias.Tag";
        Set<String> firstConfirmedGroupKeys = Set.of(firstConfirmedTypeName);
        Set<String> secondConfirmedGroupKeys = Set.of(secondConfirmedTypeName);
        List<Set<String>> groupKeysPerAnnotationInSrcOrder =
                List.of(firstConfirmedGroupKeys, secondConfirmedGroupKeys, Set.of(unconfirmedTypeKey));
        Map<String, String> rootGroupKeyByAnnotationTypeName = Map.of(
                firstConfirmedTypeName, firstConfirmedTypeName,
                secondConfirmedTypeName, secondConfirmedTypeName);

        // When
        List<Set<String>> expandedKeysPerAnnotationInSrcOrder = AnnotationOrderPreservationKeyCollector.collect(
                rootGroupKeyByAnnotationTypeName, groupKeysPerAnnotationInSrcOrder);

        // Then
        assertThat(expandedKeysPerAnnotationInSrcOrder)
                .containsExactly(
                        firstConfirmedGroupKeys,
                        secondConfirmedGroupKeys,
                        Set.of(unconfirmedTypeKey, firstConfirmedTypeName, secondConfirmedTypeName));
        assertThat(expandedKeysPerAnnotationInSrcOrder.getFirst()).isSameAs(firstConfirmedGroupKeys);
        assertThat(expandedKeysPerAnnotationInSrcOrder.get(1)).isSameAs(secondConfirmedGroupKeys);
        assertThat(expandedKeysPerAnnotationInSrcOrder.getFirst())
                .doesNotContainAnyElementsOf(expandedKeysPerAnnotationInSrcOrder.get(1));
        assertThat(expandedKeysPerAnnotationInSrcOrder.getLast())
                .containsAnyElementsOf(firstConfirmedGroupKeys)
                .containsAnyElementsOf(secondConfirmedGroupKeys);
    }
}
