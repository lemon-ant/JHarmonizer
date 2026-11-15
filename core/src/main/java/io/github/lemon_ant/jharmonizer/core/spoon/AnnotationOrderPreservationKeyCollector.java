// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
package io.github.lemon_ant.jharmonizer.core.spoon;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
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
     * @param rootContainerTypeNameByAnnotationTypeName file-wide cache with a terminal self-mapping for each input key
     * @param confirmedCanonicalTypeNames exact canonical-name evidence; imported types need not be annotations
     * @param groupKeysPerAnnotationInSrcOrder one key set per outer annotation of the current adjacent sequence
     * @return unmodifiable expanded key sets in an unmodifiable list in the original annotation order
     * @throws IllegalStateException if an input group key has no terminal self-mapping in the file-wide cache
     */
    @NonNull
    static List<Set<String>> collect(
            @NonNull Map<String, String> rootContainerTypeNameByAnnotationTypeName,
            @NonNull Set<String> confirmedCanonicalTypeNames,
            @NonNull List<Set<String>> groupKeysPerAnnotationInSrcOrder) {
        // Keep possible identities separate from the original keys; they only establish pair dependencies.
        Map<String, Set<String>> candidateGroupKeysByInputGroupKey = collectCandidateGroupKeys(
                rootContainerTypeNameByAnnotationTypeName,
                confirmedCanonicalTypeNames,
                groupKeysPerAnnotationInSrcOrder);
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
            Map<String, String> rootContainerTypeNameByAnnotationTypeName,
            Set<String> confirmedCanonicalTypeNames,
            Map<String, Set<String>> candidateGroupKeysByInputGroupKey) {
        // A confirmed Tag -> Tags mapping proves a repeatable family; self-mappings alone do not.
        // Keep its terminal key even when the container is external or absent from this sequence.
        Set<String> rootContainerGroupKeysOfConfirmedAnnotations = collectRootContainerGroupKeysOfConfirmedAnnotations(
                rootContainerTypeNameByAnnotationTypeName, confirmedCanonicalTypeNames);
        // Only input groups receive candidate sets; the source cache also contains unrelated annotation sequences.
        // Different names share a terminal cached key only through a source-declared @Repeatable chain.
        // Its source annotations are confirmed; the terminal container itself can be external/unconfirmed.
        rootContainerTypeNameByAnnotationTypeName.forEach((annotationTypeName, groupKey) -> {
            if (confirmedCanonicalTypeNames.contains(annotationTypeName)) {
                return;
            }
            Set<String> candidateGroupKeys = candidateGroupKeysByInputGroupKey.get(groupKey);
            if (candidateGroupKeys == null) {
                return;
            }
            // An unconfirmed name can denote a cached annotation with the same simple name:
            // ?.Derived.Tag can match both First.Tag and Second.Tag, which keep their separate own keys.
            String simpleAnnotationTypeName = extractSimpleTypeName(annotationTypeName);
            rootContainerTypeNameByAnnotationTypeName.forEach((otherAnnotationTypeName, candidateGroupKey) -> {
                if (simpleAnnotationTypeName.equals(extractSimpleTypeName(otherAnnotationTypeName))
                        && (
                        // This key already participates in the current sequence:
                        // ?.Derived.Tag may alias the input p.Base.Tag, so retain that shared candidate.
                        candidateGroupKeysByInputGroupKey.containsKey(candidateGroupKey)
                                // Or retain a known repeatable family even when its root key is absent from the input:
                                // with p.Tag -> p.Tags, lexical ?.Tag and ?.Tags can both select p.Tags.
                                || rootContainerGroupKeysOfConfirmedAnnotations.contains(candidateGroupKey))) {
                    candidateGroupKeys.add(candidateGroupKey);
                }
            });
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
            Map<String, String> rootContainerTypeNameByAnnotationTypeName,
            Set<String> confirmedCanonicalTypeNames,
            List<Set<String>> groupKeysPerAnnotationInSrcOrder) {
        Map<String, Set<String>> candidateGroupKeysByInputGroupKey = new HashMap<>();
        groupKeysPerAnnotationInSrcOrder.stream()
                .flatMap(Set::stream)
                .forEach(groupKey -> candidateGroupKeysByInputGroupKey.computeIfAbsent(groupKey, inputGroupKey -> {
                    // Resolved container roots and lexical keys both retain a terminal key -> key cache entry.
                    // Validate before seeding, so a singleton input cannot hide an incomplete cache.
                    if (!inputGroupKey.equals(rootContainerTypeNameByAnnotationTypeName.get(inputGroupKey))) {
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
            addAliasCandidateGroupKeys(
                    rootContainerTypeNameByAnnotationTypeName,
                    confirmedCanonicalTypeNames,
                    candidateGroupKeysByInputGroupKey);
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
    private static Set<String> collectRootContainerGroupKeysOfConfirmedAnnotations(
            Map<String, String> rootContainerTypeNameByAnnotationTypeName, Set<String> confirmedCanonicalTypeNames) {
        return rootContainerTypeNameByAnnotationTypeName.entrySet().stream()
                .filter(typeEntry -> confirmedCanonicalTypeNames.contains(typeEntry.getKey())
                        && !typeEntry.getKey().equals(typeEntry.getValue()))
                .map(Map.Entry::getValue)
                .collect(Collectors.toUnmodifiableSet());
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
