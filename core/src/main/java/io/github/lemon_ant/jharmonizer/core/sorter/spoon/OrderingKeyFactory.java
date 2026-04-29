// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
package io.github.lemon_ant.jharmonizer.core.sorter.spoon;

import static io.github.lemon_ant.jharmonizer.core.sorter.spoon.SpoonTypeMemberUtils.deriveAlphaKey;
import static io.github.lemon_ant.jharmonizer.core.sorter.spoon.SpoonTypeMemberUtils.deriveAlphaSortingRank;
import static io.github.lemon_ant.jharmonizer.core.sorter.spoon.SpoonTypeMemberUtils.deriveSrcStart;
import static io.github.lemon_ant.jharmonizer.core.sorter.spoon.SpoonTypeMemberUtils.deriveVisibilityRank;

import io.github.lemon_ant.jharmonizer.core.config.compiled.OrderingRule;
import io.github.lemon_ant.jharmonizer.core.sorter.spoon.dependency_graph.SpoonJavaBeansAccessorUtils;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import lombok.NonNull;
import lombok.experimental.UtilityClass;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.declaration.CtTypeMember;

/**
 * Factory methods for deriving {@link MemberOrderingKey} and {@link ClusteredOrderingKey}
 * instances from {@link CtTypeMember}s.
 *
 * <p>Two entry points are provided:
 * <ul>
 *   <li>{@link #createOrderingKeyProvider()} — a memoizing function suitable for callers that
 *       have no accessor clustering context (for example, top-level type ordering).</li>
 *   <li>{@link #deriveAll(List, boolean, List)} — derives keys for all members of a group,
 *       optionally computing per-property-cluster representative attributes when
 *       {@code keepAccessorsTogether} is {@code true}.</li>
 * </ul>
 */
@UtilityClass
class OrderingKeyFactory {

    /**
     * Creates a memoizing provider that maps each {@link CtTypeMember} to its
     * {@link MemberOrderingKey}.
     *
     * <p>Intended for callers that do not need accessor clustering (for example, top-level
     * types). The returned keys carry only member-own attributes and can be compared with a
     * {@link Comparator} returned by
     * {@link ComparatorUtils#buildMemberOnlyOrderingComparator(List)}.
     *
     * @return the ordering key provider function
     */
    @NonNull
    static Function<CtTypeMember, MemberOrderingKey> createOrderingKeyProvider() {
        @SuppressWarnings("PMD.UseConcurrentHashMap")
        Map<CtTypeMember, MemberOrderingKey> typeMember2OrderingKey = new HashMap<>();
        return typeMember -> typeMember2OrderingKey.computeIfAbsent(typeMember, OrderingKeyFactory::deriveMemberKey);
    }

    /**
     * Derives a {@link MemberOrderingKey} for each member in the given group.
     *
     * <p>Key derivation proceeds in three phases:
     * <ol>
     *   <li>Phase 1 — derive a {@link MemberOrderingKey} (member-own attributes) for every
     *       member, and record recognized accessor property names separately.</li>
     *   <li>Phase 2 — when {@code keepAccessorsTogether} is {@code true}, find each
     *       property cluster's <em>top member</em> (the minimum under the member-only
     *       comparator built from {@code orderingRules}).</li>
     *   <li>Phase 3 — for recognized accessor members, upgrade the base key to a
     *       {@link ClusteredOrderingKey} carrying the resolved cluster top attributes;
     *       non-accessor members keep their plain {@link MemberOrderingKey}.</li>
     * </ol>
     *
     * <p>The downstream cluster-aware comparator (see
     * {@link ComparatorUtils#buildClusteredOrderingComparator(List)}) recognizes
     * {@link ClusteredOrderingKey} instances and uses their cluster attributes
     * <em>only</em> when comparing two accessors of different property clusters; in that case
     * ALPHA additionally compares {@link ClusteredOrderingKey#getPropertyName()} instead of
     * the full method {@link MemberOrderingKey#getAlphaKey()}.
     *
     * @param groupMembers the members to derive ordering keys for
     * @param keepAccessorsTogether whether to cluster recognized JavaBeans accessor methods
     * @param orderingRules the configured ordering rules used to choose each cluster's top
     *     member; the same rules drive the final comparator (with cross-cluster substitutions)
     * @return an immutable map from each input member to its derived {@link MemberOrderingKey}
     *     (which may be a {@link ClusteredOrderingKey} for accessor members when clustering is
     *     enabled)
     */
    @NonNull
    @SuppressWarnings({"PMD.UseConcurrentHashMap", "PMD.AvoidInstantiatingObjectsInLoops"})
    static Map<CtTypeMember, MemberOrderingKey> deriveAll(
            @NonNull List<? extends CtTypeMember> groupMembers,
            boolean keepAccessorsTogether,
            @NonNull List<OrderingRule> orderingRules) {
        int memberCount = groupMembers.size();
        // Capacity * 2 ensures no resize at the default 0.75 load factor.
        Map<CtTypeMember, MemberOrderingKey> memberToBaseKey = new HashMap<>(memberCount * 2);
        // Property name per accessor member; null entries are simply absent from this map.
        Map<CtTypeMember, String> memberToPropertyName = new HashMap<>();
        // Number of unique property clusters is bounded by memberCount and is typically
        // much smaller; use the default initial capacity to avoid over-allocation.
        Map<String, List<CtTypeMember>> propertyToMembers = new HashMap<>();

        // Phase 1: derive MemberOrderingKey for every member; track accessor property names.
        for (CtTypeMember groupMember : groupMembers) {
            memberToBaseKey.put(groupMember, deriveMemberKey(groupMember));
            if (keepAccessorsTogether && groupMember instanceof CtMethod<?> method) {
                SpoonJavaBeansAccessorUtils.findAccessorPropertyName(method).ifPresent(propertyName -> {
                    memberToPropertyName.put(groupMember, propertyName);
                    propertyToMembers
                            .computeIfAbsent(propertyName, ignored -> new ArrayList<>())
                            .add(groupMember);
                });
            }
        }

        // Phase 2: discover the top member of every cluster using the member-only comparator.
        // Within a single cluster every member shares the same propertyName, so the
        // cross-cluster guard is naturally false for any pair, but we use the explicit
        // member-only variant so this method is independent of the cluster-key wiring.
        Comparator<MemberOrderingKey> memberOnlyComparator =
                ComparatorUtils.buildMemberOnlyOrderingComparator(orderingRules);
        Map<String, MemberOrderingKey> propertyToTopKey = new HashMap<>();
        for (Map.Entry<String, List<CtTypeMember>> clusterEntry : propertyToMembers.entrySet()) {
            MemberOrderingKey topKey = clusterEntry.getValue().stream()
                    .map(memberToBaseKey::get)
                    .min(memberOnlyComparator)
                    .orElseThrow(() ->
                            new IllegalStateException("Empty accessor cluster for property: " + clusterEntry.getKey()));
            propertyToTopKey.put(clusterEntry.getKey(), topKey);
        }

        // Phase 3: upgrade accessor members to ClusteredOrderingKey; non-accessors keep baseKey.
        Map<CtTypeMember, MemberOrderingKey> result = new HashMap<>(memberCount * 2);
        for (CtTypeMember groupMember : groupMembers) {
            MemberOrderingKey baseKey = memberToBaseKey.get(groupMember);
            String propertyName = memberToPropertyName.get(groupMember);
            if (propertyName != null) {
                MemberOrderingKey clusterTopKey = propertyToTopKey.get(propertyName);
                result.put(
                        groupMember,
                        new ClusteredOrderingKey(
                                baseKey.getSrcStart(),
                                baseKey.getAlphaKey(),
                                baseKey.getAlphaSortingRank(),
                                baseKey.getVisibilityRank(),
                                propertyName,
                                clusterTopKey.getSrcStart(),
                                clusterTopKey.getVisibilityRank()));
            } else {
                result.put(groupMember, baseKey);
            }
        }
        return Collections.unmodifiableMap(result);
    }

    @NonNull
    private static MemberOrderingKey deriveMemberKey(CtTypeMember typeMember) {
        return new MemberOrderingKey(
                deriveSrcStart(typeMember),
                deriveAlphaKey(typeMember),
                deriveAlphaSortingRank(typeMember),
                deriveVisibilityRank(typeMember));
    }
}
