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
    void collect_inputIterationOrders_keepsCanonicalPairKeys(
            boolean reverseAnnotationOrder, boolean reverseGroupKeyOrder) {
        // Given
        String firstConfirmedTypeName = "p.First.Tag";
        String secondConfirmedTypeName = "p.Second.Tag";
        String unconfirmedTypeKey = "?.Alias.Tag";
        String firstAliasPairKey = "?.Alias.Tag|p.First.Tag";
        String secondAliasPairKey = "?.Alias.Tag|p.Second.Tag";
        List<Set<String>> groupKeysPerAnnotationInSrcOrder = Stream.of(
                        List.of(firstConfirmedTypeName, secondConfirmedTypeName),
                        List.of(unconfirmedTypeKey, firstConfirmedTypeName),
                        List.of(unconfirmedTypeKey))
                .map(groupKeys -> reverseGroupKeyOrder ? groupKeys.reversed() : groupKeys)
                .map(groupKeys -> Collections.unmodifiableSet(new LinkedHashSet<>(groupKeys)))
                .toList();
        List<Set<String>> expectedKeysPerAnnotationInSrcOrder = List.of(
                Set.of(firstConfirmedTypeName, secondConfirmedTypeName, firstAliasPairKey, secondAliasPairKey),
                Set.of(unconfirmedTypeKey, firstConfirmedTypeName, firstAliasPairKey, secondAliasPairKey),
                Set.of(unconfirmedTypeKey, firstAliasPairKey, secondAliasPairKey));
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
        String aliasPairKey = "?.Alias.Tag|p.Owner.Tag";
        Set<String> unrelatedGroupKeys = Set.of(unrelatedTypeName);
        List<Set<String>> groupKeysPerAnnotationInSrcOrder =
                List.of(Set.of(confirmedTypeName), Set.of(unconfirmedTypeKey), unrelatedGroupKeys);
        Map<String, String> rootGroupKeyByAnnotationTypeName = Map.of(
                confirmedTypeName, confirmedTypeName,
                unrelatedTypeName, unrelatedTypeName);

        // When
        List<Set<String>> expandedKeysPerAnnotationInSrcOrder = AnnotationOrderPreservationKeyCollector.collect(
                rootGroupKeyByAnnotationTypeName, groupKeysPerAnnotationInSrcOrder);

        // Then
        assertThat(expandedKeysPerAnnotationInSrcOrder)
                .isNotSameAs(groupKeysPerAnnotationInSrcOrder)
                .containsExactly(
                        Set.of(confirmedTypeName, aliasPairKey),
                        Set.of(unconfirmedTypeKey, aliasPairKey),
                        unrelatedGroupKeys);
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
    void collect_uncachedLexicalHomonyms_preservesPairDependencies() {
        // Given
        String firstLexicalKey = "?.First.Tag";
        String secondLexicalKey = "?.Second.Tag";
        String pairKey = "?.First.Tag|?.Second.Tag";
        List<Set<String>> groupKeysPerAnnotationInSrcOrder = List.of(Set.of(firstLexicalKey), Set.of(secondLexicalKey));

        // When
        List<Set<String>> expandedKeysPerAnnotationInSrcOrder =
                AnnotationOrderPreservationKeyCollector.collect(Map.of(), groupKeysPerAnnotationInSrcOrder);

        // Then
        assertThat(expandedKeysPerAnnotationInSrcOrder)
                .containsExactly(Set.of(firstLexicalKey, pairKey), Set.of(secondLexicalKey, pairKey));
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
        String pairKey = "?.Tag|?.Tags";
        List<Set<String>> groupKeysPerAnnotationInSrcOrder = List.of(Set.of(annotationKey), Set.of(containerKey));
        Map<String, String> rootGroupKeyByAnnotationTypeName = Map.of("p.Tag", "?.library.Tags");

        // When
        List<Set<String>> expandedKeysPerAnnotationInSrcOrder = AnnotationOrderPreservationKeyCollector.collect(
                rootGroupKeyByAnnotationTypeName, groupKeysPerAnnotationInSrcOrder);

        // Then
        assertThat(expandedKeysPerAnnotationInSrcOrder)
                .containsExactly(Set.of(annotationKey, pairKey), Set.of(containerKey, pairKey));
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
}
