// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
package io.github.lemon_ant.jharmonizer.core.translator.spoon.annotation;

import static io.github.lemon_ant.jharmonizer.core.translator.spoon.annotation.AnnotationDependencyKeyUtils.isConfirmedGroupKey;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.NonNull;
import lombok.experimental.UtilityClass;

/** Adds possible alias keys to annotations in one source group. */
@UtilityClass
class AnnotationOrderPreservationKeyCollector {
    private static final int MIN_GROUP_KEYS_FOR_ALIAS_EXPANSION = 2;

    /**
     * Expands unconfirmed keys by matching simple names against the group's original keys and both ends
     * of each source repeatable relationship. The result can share its input collections, so callers must
     * provide read-only collections and leave their backing collections unchanged.
     * @param rootGroupKeyByAnnotationTypeName file-wide map from confirmed type names to terminal group keys
     * @param groupKeysPerAnnotationInSrcOrder one key set per outer annotation in this group, including nested uses
     * @return read-only key sets in a read-only list in the original annotation order
     * @throws IllegalStateException if a confirmed input key has no terminal self-mapping in the file-wide cache
     */
    @NonNull
    static List<Set<String>> collect(
            @NonNull Map<String, String> rootGroupKeyByAnnotationTypeName,
            @NonNull List<Set<String>> groupKeysPerAnnotationInSrcOrder) {
        Map<String, Set<String>> candidateGroupKeysByInputGroupKey =
                collectCandidateGroupKeys(rootGroupKeyByAnnotationTypeName, groupKeysPerAnnotationInSrcOrder);
        // Every singleton candidate set contains only its original key.
        if (candidateGroupKeysByInputGroupKey.values().stream()
                .allMatch(candidateGroupKeys ->
                        candidateGroupKeys.size() == /*TODO Introduce a constant with a meaningful name*/ 1)) {
            return groupKeysPerAnnotationInSrcOrder;
        }
        return groupKeysPerAnnotationInSrcOrder.stream()
                .map(groupKeys -> expandGroupKeys(groupKeys, candidateGroupKeysByInputGroupKey))
                .toList();
    }

    private static void addAliasCandidateGroupKeys(
            Map<String, String> rootGroupKeyByAnnotationTypeName,
            Map<String, Set<String>> candidateGroupKeysByInputGroupKey) {
        candidateGroupKeysByInputGroupKey.forEach((inputGroupKey, candidateGroupKeys) -> {
            if (isConfirmedGroupKey(inputGroupKey)) {
                return;
            }
            String simpleAnnotationTypeName = extractSimpleTypeName(inputGroupKey);
            // Match identities in this sequence, including uncached lexical keys:
            // ?.Derived.Tag may alias either p.Base.Tag or another lexical ?.Other.Tag.
            candidateGroupKeysByInputGroupKey.keySet().stream()
                    .filter(otherGroupKey -> simpleAnnotationTypeName.equals(extractSimpleTypeName(otherGroupKey)))
                    .forEach(candidateGroupKeys::add);
            // A non-self entry proves a source-declared repeatable family, even when its root is unconfirmed.
            // For p.Tag -> ?.library.Tags, match Tag by the source name and Tags by the root value:
            // the unknown terminal has no separate cache entry, but ?.Tag and ?.Tags need that shared candidate.
            rootGroupKeyByAnnotationTypeName.entrySet().stream()
                    .filter(typeEntry -> !typeEntry.getKey().equals(typeEntry.getValue()))
                    .filter(typeEntry -> simpleAnnotationTypeName.equals(extractSimpleTypeName(typeEntry.getKey()))
                            || simpleAnnotationTypeName.equals(extractSimpleTypeName(typeEntry.getValue())))
                    .map(Map.Entry::getValue)
                    .forEach(candidateGroupKeys::add);
        });
    }

    @NonNull
    @SuppressWarnings("PMD.UseConcurrentHashMap")
    private static Map<String, Set<String>> collectCandidateGroupKeys(
            Map<String, String> rootGroupKeyByAnnotationTypeName, List<Set<String>> groupKeysPerAnnotationInSrcOrder) {
        Map<String, Set<String>> candidateGroupKeysByInputGroupKey = new HashMap<>();
        groupKeysPerAnnotationInSrcOrder.stream()
                .flatMap(Set::stream)
                .forEach(groupKey -> candidateGroupKeysByInputGroupKey.computeIfAbsent(groupKey, inputGroupKey -> {
                    // Unconfirmed input keys need no cache entry, including unresolved metadata roots.
                    // Validate confirmed roots before seeding, so singleton inputs cannot hide an incomplete cache.
                    if (isConfirmedGroupKey(inputGroupKey)
                            && !inputGroupKey.equals(rootGroupKeyByAnnotationTypeName.get(inputGroupKey))) {
                        throw new IllegalStateException(
                                "No indexed annotation type names for group key: " + inputGroupKey);
                    }
                    Set<String> candidateGroupKeys = new HashSet<>();
                    candidateGroupKeys.add(inputGroupKey);
                    return candidateGroupKeys;
                }));
        if (candidateGroupKeysByInputGroupKey.isEmpty()) {
            return Map.of();
        }
        if (candidateGroupKeysByInputGroupKey.size() >= MIN_GROUP_KEYS_FOR_ALIAS_EXPANSION) {
            addAliasCandidateGroupKeys(rootGroupKeyByAnnotationTypeName, candidateGroupKeysByInputGroupKey);
        }
        return Collections.unmodifiableMap(candidateGroupKeysByInputGroupKey);
    }

    @NonNull
    private static Set<String> expandGroupKeys(
            Set<String> originalGroupKeys, Map<String, Set<String>> candidateGroupKeysByInputGroupKey) {
        if (originalGroupKeys.stream()
                .allMatch(groupKey -> originalGroupKeys.containsAll(candidateGroupKeysByInputGroupKey.get(groupKey)))) {
            return originalGroupKeys;
        }
        // TODO Add examples
        // Expand only original keys. An ambiguous use can share keys with two confirmed types while
        // each confirmed type retains its own set.
        Set<String> expandedGroupKeys = new HashSet<>(originalGroupKeys);
        originalGroupKeys.forEach(
                groupKey -> expandedGroupKeys.addAll(candidateGroupKeysByInputGroupKey.get(groupKey)));
        return Collections.unmodifiableSet(expandedGroupKeys);
    }

    @NonNull
    private static String extractSimpleTypeName(String typeName) {
        return typeName.substring(typeName.lastIndexOf('.') + 1);
    }
}
