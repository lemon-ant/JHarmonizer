// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
package io.github.lemon_ant.jharmonizer.core.spoon;

import static io.github.lemon_ant.jharmonizer.core.spoon.AnnotationDependencyKeyUtils.isConfirmedGroupKey;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.NonNull;
import lombok.experimental.UtilityClass;

/** Prepares annotation dependency keys without merging potentially distinct type identities. */
@UtilityClass
class AnnotationOrderPreservationKeyCollector {
    private static final int MIN_GROUP_KEYS_FOR_PAIR = 2;

    /**
     * Adds conservative dependency keys for possible aliases and source-declared repeatable relationships.
     * Each list entry is one outer annotation's key set, including keys of annotations nested in its arguments.
     * The list covers only the adjacent outer annotations currently being parsed.
     * For {@code @Envelope({@Nested}) @Marker int first; @Nested int second;}, the calls receive
     * {@code [{Envelope, Nested}, {Marker}]} and {@code [{Nested}]} respectively (type qualification omitted).
     * Reuses unchanged input collections. The input list and its sets must be unmodifiable, and their producers
     * must not mutate the backing collections after handoff.
     * @param rootGroupKeyByAnnotationTypeName confirmed canonical type names mapped to terminal group keys;
     *                                         confirmed input keys require terminal self-mappings
     * @param groupKeysPerAnnotationInSrcOrder one key set per outer annotation of the current adjacent sequence
     * @return unmodifiable expanded key sets in an unmodifiable list in the original annotation order
     * @throws IllegalStateException if a confirmed input key has no terminal self-mapping in the file-wide cache
     */
    @NonNull
    static List<Set<String>> collect(
            @NonNull Map<String, String> rootGroupKeyByAnnotationTypeName,
            @NonNull List<Set<String>> groupKeysPerAnnotationInSrcOrder) {
        // Keep possible identities separate from the original keys; they only establish pair dependencies.
        Map<String, Set<String>> candidateGroupKeysByInputGroupKey =
                collectCandidateGroupKeys(rootGroupKeyByAnnotationTypeName, groupKeysPerAnnotationInSrcOrder);
        // Candidate collection validates cache coverage even when no pair can exist.
        if (candidateGroupKeysByInputGroupKey.size() < MIN_GROUP_KEYS_FOR_PAIR) {
            return groupKeysPerAnnotationInSrcOrder;
        }
        Map<String, Set<String>> pairKeysByGroupKey = collectPairKeys(candidateGroupKeysByInputGroupKey);
        if (pairKeysByGroupKey.isEmpty()) {
            return groupKeysPerAnnotationInSrcOrder;
        }
        return groupKeysPerAnnotationInSrcOrder.stream()
                .map(groupKeys -> expandGroupKeys(groupKeys, pairKeysByGroupKey))
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

    private static void addPairKey(Map<String, Set<String>> pairKeysByGroupKey, String groupKey, String pairKey) {
        pairKeysByGroupKey
                .computeIfAbsent(groupKey, unusedGroupKey -> new HashSet<>())
                .add(pairKey);
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
        if (candidateGroupKeysByInputGroupKey.size() >= MIN_GROUP_KEYS_FOR_PAIR) {
            addAliasCandidateGroupKeys(rootGroupKeyByAnnotationTypeName, candidateGroupKeysByInputGroupKey);
        }
        return Collections.unmodifiableMap(candidateGroupKeysByInputGroupKey);
    }

    @NonNull
    @SuppressWarnings("PMD.UseConcurrentHashMap")
    private static Map<String, Set<String>> collectPairKeys(
            Map<String, Set<String>> candidateGroupKeysByInputGroupKey) {
        Map<String, Set<String>> pairKeysByGroupKey = new HashMap<>();
        for (String firstGroupKey : candidateGroupKeysByInputGroupKey.keySet()) {
            for (String secondGroupKey : candidateGroupKeysByInputGroupKey.keySet()) {
                // Both loops use the same key set, so they visit (A, B), (B, A), and self-pairs such as (A, A).
                // For A < B: skip (A, A), whose occurrences already share their original key A.
                // Skip (B, A): (A, B) checks the same unordered pair, regardless of map iteration order.
                // Only (A, B) reaches candidate matching below and can create the stable pair key A|B.
                // The sorter derives dependency direction from source order; this fixes the key's spelling.
                if (firstGroupKey.compareTo(secondGroupKey) >= 0) {
                    continue;
                }
                if (!Collections.disjoint(
                        candidateGroupKeysByInputGroupKey.get(firstGroupKey),
                        candidateGroupKeysByInputGroupKey.get(secondGroupKey))) {
                    // The separator cannot occur in a Java type name.
                    // Do not merge aliases transitively: uncertain Derived.Tag may denote First.Tag or
                    // Second.Tag while those two confirmed annotations remain independently sortable.
                    String pairKey = firstGroupKey + '|' + secondGroupKey;
                    addPairKey(pairKeysByGroupKey, firstGroupKey, pairKey);
                    addPairKey(pairKeysByGroupKey, secondGroupKey, pairKey);
                }
            }
        }
        return Collections.unmodifiableMap(pairKeysByGroupKey);
    }

    @NonNull
    private static Set<String> expandGroupKeys(
            Set<String> originalGroupKeys, Map<String, Set<String>> pairKeysByGroupKey) {
        // Raw type/container and lexical keys never contain '|'; synthetic pair keys are added only here.
        // Every pair-map entry is nonempty, so its presence guarantees keys absent from the original set.
        if (originalGroupKeys.stream().noneMatch(pairKeysByGroupKey::containsKey)) {
            return originalGroupKeys;
        }
        Set<String> expandedGroupKeys = new HashSet<>(originalGroupKeys);
        originalGroupKeys.forEach(
                groupKey -> expandedGroupKeys.addAll(pairKeysByGroupKey.getOrDefault(groupKey, Set.of())));
        return Collections.unmodifiableSet(expandedGroupKeys);
    }

    @NonNull
    private static String extractSimpleTypeName(String typeName) {
        return typeName.substring(typeName.lastIndexOf('.') + 1);
    }
}
