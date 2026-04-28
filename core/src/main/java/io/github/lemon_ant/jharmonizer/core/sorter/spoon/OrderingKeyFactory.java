// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
package io.github.lemon_ant.jharmonizer.core.sorter.spoon;

import static io.github.lemon_ant.jharmonizer.core.sorter.spoon.SpoonTypeMemberUtils.deriveAlphaKey;
import static io.github.lemon_ant.jharmonizer.core.sorter.spoon.SpoonTypeMemberUtils.deriveAlphaSortingRank;
import static io.github.lemon_ant.jharmonizer.core.sorter.spoon.SpoonTypeMemberUtils.deriveVisibilityRank;
import static io.github.lemon_ant.jharmonizer.core.sorter.spoon.SpoonTypeMemberUtils.extractSrcStart;

import edu.umd.cs.findbugs.annotations.Nullable;
import io.github.lemon_ant.jharmonizer.core.config.compiled.OrderingRule;
import io.github.lemon_ant.jharmonizer.core.sorter.spoon.SortableTypeMember.OrderingKey;
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
 * Factory methods for deriving {@link OrderingKey} instances from {@link CtTypeMember}s.
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
     * Creates a memoizing provider that maps each {@link CtTypeMember} to its {@link OrderingKey}.
     *
     * <p>Intended for callers that do not need accessor clustering (for example, top-level types).
     * Every key is derived with no cluster context, so each member's cluster attributes equal its
     * own attributes and {@link OrderingKey#getPropertyName()} returns {@code null}.
     *
     * @return the ordering key provider function
     */
    @NonNull
    static Function<CtTypeMember, OrderingKey> createOrderingKeyProvider() {
        @SuppressWarnings("PMD.UseConcurrentHashMap")
        Map<CtTypeMember, OrderingKey> typeMember2OrderingKey = new HashMap<>();
        return typeMember ->
                typeMember2OrderingKey.computeIfAbsent(typeMember, OrderingKeyFactory::deriveWithoutClustering);
    }

    /**
     * Derives an {@link OrderingKey} for each member in the given group.
     *
     * <p>When {@code keepAccessorsTogether} is {@code true}, every recognized JavaBeans accessor
     * method (see {@link SpoonJavaBeansAccessorUtils#findAccessorPropertyName}) is assigned a
     * non-null {@link OrderingKey#getPropertyName()}. All accessors sharing the same property name
     * form one cluster. For each cluster the <em>top member</em> is chosen as the minimum under
     * the member-only comparator built from {@code orderingRules}, and its
     * {@code srcStart} / {@code visibilityRank} become the cluster's representative attributes for
     * every member of that cluster. For non-accessor members and for accessors when clustering is
     * disabled, {@link OrderingKey#getPropertyName()} is {@code null} and cluster attributes equal
     * the member's own attributes.
     *
     * <p>The downstream cluster-aware comparator (see
     * {@link ComparatorUtils#buildOrderingComparator(List)}) uses these cluster attributes
     * <em>only</em> when comparing two accessors of different property clusters; in that case ALPHA
     * additionally compares {@link OrderingKey#getPropertyName()} instead of the full method
     * {@link OrderingKey#getAlphaKey()}. All other comparisons use member-own attributes.
     *
     * @param groupMembers the members to derive ordering keys for
     * @param keepAccessorsTogether whether to cluster recognized JavaBeans accessor methods
     * @param orderingRules the configured ordering rules used to choose each cluster's top member;
     *     the same rules drive the final comparator (with cross-cluster substitutions)
     * @return an immutable map from each input member to its derived {@link OrderingKey}
     */
    @NonNull
    @SuppressWarnings({"PMD.UseConcurrentHashMap", "PMD.AvoidInstantiatingObjectsInLoops"})
    static Map<CtTypeMember, OrderingKey> deriveAll(
            @NonNull List<? extends CtTypeMember> groupMembers,
            boolean keepAccessorsTogether,
            @NonNull List<OrderingRule> orderingRules) {
        int memberCount = groupMembers.size();
        // Capacity * 2 ensures no resize at the default 0.75 load factor.
        Map<CtTypeMember, OrderingKey> memberToOwnKey = new HashMap<>(memberCount * 2);
        // Number of unique property clusters is bounded by memberCount and is typically
        // much smaller; use the default initial capacity to avoid over-allocation.
        Map<String, List<CtTypeMember>> propertyToMembers = new HashMap<>();

        for (CtTypeMember groupMember : groupMembers) {
            String propertyName = null;
            if (keepAccessorsTogether && groupMember instanceof CtMethod<?> method) {
                propertyName = SpoonJavaBeansAccessorUtils.findAccessorPropertyName(method)
                        .orElse(null);
            }
            OrderingKey ownKey = deriveWithoutClustering(groupMember, propertyName);
            memberToOwnKey.put(groupMember, ownKey);
            if (propertyName != null) {
                propertyToMembers
                        .computeIfAbsent(propertyName, ignored -> new ArrayList<>())
                        .add(groupMember);
            }
        }

        // Discover the top member of every cluster using the member-only comparator,
        // i.e., the configured comparator with cross-cluster substitutions disabled.
        // Within a single cluster every member shares the same propertyName, so the
        // cross-cluster guard is naturally false for any pair, but we use the explicit
        // member-only variant so this method is independent of the cluster-key wiring.
        Comparator<OrderingKey> memberOnlyComparator = ComparatorUtils.buildMemberOnlyOrderingComparator(orderingRules);
        Map<String, OrderingKey> propertyToTopMemberKey = new HashMap<>();
        for (Map.Entry<String, List<CtTypeMember>> clusterEntry : propertyToMembers.entrySet()) {
            OrderingKey topMemberKey = clusterEntry.getValue().stream()
                    .map(memberToOwnKey::get)
                    .min(memberOnlyComparator)
                    .orElseThrow(() ->
                            new IllegalStateException("Empty accessor cluster for property: " + clusterEntry.getKey()));
            propertyToTopMemberKey.put(clusterEntry.getKey(), topMemberKey);
        }

        Map<CtTypeMember, OrderingKey> memberToOrderingKey = new HashMap<>(memberCount * 2);
        for (CtTypeMember groupMember : groupMembers) {
            OrderingKey ownKey = memberToOwnKey.get(groupMember);
            String propertyName = ownKey.getPropertyName();
            OrderingKey clusterTopKey = propertyName == null ? ownKey : propertyToTopMemberKey.get(propertyName);
            memberToOrderingKey.put(
                    groupMember,
                    new OrderingKey(
                            ownKey.getSrcStart(),
                            ownKey.getAlphaKey(),
                            ownKey.getAlphaSortingRank(),
                            ownKey.getVisibilityRank(),
                            propertyName,
                            clusterTopKey.getSrcStart(),
                            clusterTopKey.getVisibilityRank()));
        }
        return Collections.unmodifiableMap(memberToOrderingKey);
    }

    @NonNull
    private static OrderingKey deriveWithoutClustering(CtTypeMember typeMember) {
        return deriveWithoutClustering(typeMember, null);
    }

    @NonNull
    private static OrderingKey deriveWithoutClustering(CtTypeMember typeMember, @Nullable String propertyName) {
        int srcStart = extractSrcStart(typeMember);
        int alphaSortingRank = deriveAlphaSortingRank(typeMember);
        int visibilityRank = deriveVisibilityRank(typeMember);
        return new OrderingKey(
                srcStart,
                deriveAlphaKey(typeMember),
                alphaSortingRank,
                visibilityRank,
                propertyName,
                srcStart,
                visibilityRank);
    }
}
