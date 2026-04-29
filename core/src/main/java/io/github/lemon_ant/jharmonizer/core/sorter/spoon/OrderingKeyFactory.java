// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
package io.github.lemon_ant.jharmonizer.core.sorter.spoon;

import static io.github.lemon_ant.jharmonizer.core.sorter.spoon.SpoonTypeMemberUtils.deriveAlphaKey;
import static io.github.lemon_ant.jharmonizer.core.sorter.spoon.SpoonTypeMemberUtils.deriveAlphaSortingRank;
import static io.github.lemon_ant.jharmonizer.core.sorter.spoon.SpoonTypeMemberUtils.deriveSrcStart;
import static io.github.lemon_ant.jharmonizer.core.sorter.spoon.SpoonTypeMemberUtils.deriveVisibilityRank;

import io.github.lemon_ant.jharmonizer.core.config.compiled.OrderingRule;
import io.github.lemon_ant.jharmonizer.core.sorter.spoon.SortableTypeMember.OrderingKey;
import io.github.lemon_ant.jharmonizer.core.sorter.spoon.dependency_graph.SpoonJavaBeansAccessorUtils;
import java.util.ArrayList;
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
 * Factory methods for {@link OrderingKey} instances and for assembling
 * {@link SortableTypeMember} lists with their own and representative ordering keys.
 *
 * <p>Two entry points are provided:
 * <ul>
 *   <li>{@link #createOrderingKeyProvider()} — a memoizing function suitable for callers that
 *       have no accessor clustering context (for example, top-level type ordering).</li>
 *   <li>{@link #createSortableMembers(List, boolean, List)} — derives a {@link SortableTypeMember}
 *       per input member, with each member's own key, its property-cluster representative key and
 *       its super-cluster representative key.</li>
 * </ul>
 */
@UtilityClass
class OrderingKeyFactory {

    /**
     * The accessor super-cluster only matters when the group contains at least two recognized
     * JavaBeans accessors. With a single accessor there is no super-cluster, so the accessor
     * keeps its own key as both representatives (self-reference).
     */
    static final int MIN_ACCESSORS_FOR_SUPER_CLUSTER = 2;

    /**
     * Creates a memoizing provider that maps each {@link CtTypeMember} to its {@link OrderingKey}.
     *
     * <p>Intended for callers that do not need accessor clustering (for example, top-level
     * types). The returned keys carry the member's own attributes; they can be compared with a
     * {@link Comparator} returned by {@link ComparatorUtils#buildOrderingKeyComparator(List)}.
     *
     * @return the ordering key provider function
     */
    @NonNull
    static Function<CtTypeMember, OrderingKey> createOrderingKeyProvider() {
        @SuppressWarnings("PMD.UseConcurrentHashMap")
        Map<CtTypeMember, OrderingKey> typeMemberToOwnKey = new HashMap<>();
        return typeMember -> typeMemberToOwnKey.computeIfAbsent(typeMember, OrderingKeyFactory::deriveOwnKey);
    }

    /**
     * Builds a {@link SortableTypeMember} per input member, populating the member's own ordering
     * key, its property-cluster representative key and its super-cluster representative key.
     *
     * <p>Key derivation proceeds in three phases:
     * <ol>
     *   <li>Phase 1 — derive each member's own {@link OrderingKey} and record recognized
     *       accessor property names separately.</li>
     *   <li>Phase 2 — when {@code keepAccessorsTogether} is {@code true} and the group has at
     *       least two recognized accessors:
     *       <ul>
     *         <li>For each property cluster, pick the property cluster's <em>top</em> accessor as
     *             the minimum own key under
     *             {@link ComparatorUtils#buildOrderingKeyComparator(List)} and synthesize a
     *             property representative {@link OrderingKey} whose {@code alphaKey} is the
     *             property name and whose {@code srcStart} / {@code visibilityRank} come from
     *             the property top member; share that instance among every accessor of the
     *             property.</li>
     *         <li>The super-cluster representative is the global minimum own key among
     *             <em>all</em> accessor methods, across all property clusters, as determined by
     *             {@link ComparatorUtils#buildOrderingKeyComparator(List)}. Sharing this own key
     *             as the super-cluster representative for every accessor makes the super-cluster
     *             sort relative to non-accessors at the position of the very first accessor by
     *             the configured comparator, regardless of property-cluster structure.</li>
     *       </ul></li>
     *   <li>Phase 3 — assemble {@link SortableTypeMember}s. Non-accessors and accessors that do
     *       not belong to a multi-member super-cluster keep their own key as both
     *       representatives (self-reference).</li>
     * </ol>
     *
     * @param groupMembers the members to derive ordering keys for
     * @param keepAccessorsTogether whether to cluster recognized JavaBeans accessor methods
     * @param orderingRules the ordering rules used to choose each cluster's top member
     * @return one {@link SortableTypeMember} per input member, in the input order
     */
    @NonNull
    @SuppressWarnings({"PMD.UseConcurrentHashMap", "PMD.AvoidInstantiatingObjectsInLoops"})
    static List<SortableTypeMember> createSortableMembers(
            @NonNull List<? extends CtTypeMember> groupMembers,
            boolean keepAccessorsTogether,
            @NonNull List<OrderingRule> orderingRules) {
        int memberCount = groupMembers.size();
        // Capacity * 2 ensures no resize at the default 0.75 load factor.
        Map<CtTypeMember, OrderingKey> memberToOwnKey = new HashMap<>(memberCount * 2);
        Map<String, List<CtTypeMember>> propertyToAccessors = new HashMap<>();
        List<CtTypeMember> allAccessors = new ArrayList<>();

        // Phase 1: derive each member's own OrderingKey; track accessor property names.
        for (CtTypeMember groupMember : groupMembers) {
            memberToOwnKey.put(groupMember, deriveOwnKey(groupMember));
            if (keepAccessorsTogether && groupMember instanceof CtMethod<?> method) {
                SpoonJavaBeansAccessorUtils.findAccessorPropertyName(method).ifPresent(propertyName -> {
                    propertyToAccessors
                            .computeIfAbsent(propertyName, ignored -> new ArrayList<>())
                            .add(groupMember);
                    allAccessors.add(groupMember);
                });
            }
        }

        // Phase 2: build super-cluster + property-cluster representatives only when the
        // super-cluster actually contains at least two accessors.
        Map<CtTypeMember, OrderingKey> memberToSuperRep = new HashMap<>();
        Map<CtTypeMember, OrderingKey> memberToPropertyRep = new HashMap<>();
        if (allAccessors.size() >= MIN_ACCESSORS_FOR_SUPER_CLUSTER) {
            Comparator<OrderingKey> orderingKeyComparator = ComparatorUtils.buildOrderingKeyComparator(orderingRules);
            // Per property cluster: pick the top own key (used as the cluster representative's
            // srcStart/visibilityRank source) and synthesize the property representative key.
            for (Map.Entry<String, List<CtTypeMember>> clusterEntry : propertyToAccessors.entrySet()) {
                String propertyName = clusterEntry.getKey();
                List<CtTypeMember> propertyMembers = clusterEntry.getValue();
                OrderingKey propertyTopOwnKey = propertyMembers.stream()
                        .map(memberToOwnKey::get)
                        .min(orderingKeyComparator)
                        .orElseThrow(() ->
                                new IllegalStateException("Empty accessor cluster for property: " + propertyName));
                OrderingKey propertyRepresentativeKey = new OrderingKey(
                        propertyTopOwnKey.getSrcStart(),
                        propertyName,
                        propertyTopOwnKey.getAlphaSortingRank(),
                        propertyTopOwnKey.getVisibilityRank());
                for (CtTypeMember accessor : propertyMembers) {
                    memberToPropertyRep.put(accessor, propertyRepresentativeKey);
                }
            }
            // Super-cluster representative = global minimum own-key among all accessor methods,
            // regardless of property-cluster structure. This ensures the super-cluster sorts
            // relative to non-accessors at the position of the very first accessor by the
            // configured comparator.
            OrderingKey superClusterTopOwnKey = allAccessors.stream()
                    .map(memberToOwnKey::get)
                    .min(orderingKeyComparator)
                    .orElseThrow(() -> new IllegalStateException("Empty accessor super-cluster"));
            for (CtTypeMember accessor : allAccessors) {
                memberToSuperRep.put(accessor, superClusterTopOwnKey);
            }
        }

        // Phase 3: assemble sortable members; both representatives default to own key.
        return groupMembers.stream()
                .map(groupMember -> {
                    OrderingKey ownKey = memberToOwnKey.get(groupMember);
                    OrderingKey propertyRep = memberToPropertyRep.getOrDefault(groupMember, ownKey);
                    OrderingKey superRep = memberToSuperRep.getOrDefault(groupMember, ownKey);
                    return new SortableTypeMember(groupMember, ownKey, propertyRep, superRep);
                })
                .toList();
    }

    @NonNull
    private static OrderingKey deriveOwnKey(CtTypeMember typeMember) {
        return new OrderingKey(
                deriveSrcStart(typeMember),
                deriveAlphaKey(typeMember),
                deriveAlphaSortingRank(typeMember),
                deriveVisibilityRank(typeMember));
    }
}
