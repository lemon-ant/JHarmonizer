// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
package io.github.lemon_ant.jharmonizer.core.sorter.spoon;

import io.github.lemon_ant.jharmonizer.core.config.compiled.OrderingRule;
import java.util.Comparator;
import java.util.List;
import lombok.NonNull;
import lombok.experimental.UtilityClass;

/**
 * Internal factory methods for comparators used to order type members within member groups.
 *
 * <p>A single uniform comparator is built over {@link OrderingKey}: it knows nothing about
 * accessor clusters. Cluster handling is folded into the {@link OrderingKey} representatives
 * carried by each {@link SortableTypeMember} (see {@link OrderingKeyFactory}). The
 * {@link SortableTypeMember} comparator dispatches between the super-cluster representative,
 * the property-cluster representative and the member's own key based on reference equality of
 * the representatives, so the only logic the {@link OrderingKey}-level comparator needs to know
 * is how to compare two plain {@link OrderingKey}s under the configured ordering rules.
 */
@UtilityClass
class ComparatorUtils {

    /**
     * Builds a comparator over {@link OrderingKey} from the given ordering rules. Tie-breakers
     * (PRESERVE then ALPHA) are appended to ensure deterministic ordering.
     *
     * @param orderingRules the ordering rules to apply, in priority order
     * @return the ordering key comparator
     */
    @NonNull
    static Comparator<OrderingKey> buildOrderingKeyComparator(@NonNull List<OrderingRule> orderingRules) {
        Comparator<OrderingKey> configured = orderingRules.stream()
                .map(ComparatorUtils::buildComparatorForRule)
                .reduce(Comparator::thenComparing)
                .orElseGet(() -> Comparator.<OrderingKey>comparingInt(OrderingKey::getSrcStart)
                        .thenComparing(OrderingKey::getAlphaKey));
        return appendTieBreakers(configured, orderingRules);
    }

    /**
     * Builds a comparator over {@link SortableTypeMember} that decides ordering by comparing the
     * members' representative ordering keys. When the two representatives are the same instance
     * (same accessor cluster), the comparator falls back to comparing the members' own keys.
     *
     * @param orderingRules the ordering rules to apply, in priority order
     * @return the sortable type member comparator
     */
    @NonNull
    static Comparator<SortableTypeMember> buildSortableTypeMemberComparator(@NonNull List<OrderingRule> orderingRules) {
        Comparator<OrderingKey> base = buildOrderingKeyComparator(orderingRules);
        return (left, right) -> {
            // Reference equality is intentional throughout: members that belong to the same
            // accessor super-cluster share the same super-cluster representative instance, and
            // members of the same property cluster share the same property representative
            // instance. Non-clustered members use their own key as the representative
            // (self-reference).
            OrderingKey leftSuperRep = left.getSuperClusterRepresentativeKey();
            OrderingKey rightSuperRep = right.getSuperClusterRepresentativeKey();
            if (leftSuperRep != rightSuperRep) {
                return base.compare(leftSuperRep, rightSuperRep);
            }
            OrderingKey leftPropertyRep = left.getPropertyClusterRepresentativeKey();
            OrderingKey rightPropertyRep = right.getPropertyClusterRepresentativeKey();
            if (leftPropertyRep != rightPropertyRep) {
                return base.compare(leftPropertyRep, rightPropertyRep);
            }
            return base.compare(left.getOwnKey(), right.getOwnKey());
        };
    }

    @NonNull
    private static Comparator<OrderingKey> appendTieBreakers(
            Comparator<OrderingKey> configured, List<OrderingRule> orderingRules) {
        Comparator<OrderingKey> result = configured;
        if (!orderingRules.contains(OrderingRule.PRESERVE)) {
            result = result.thenComparing(buildComparatorForRule(OrderingRule.PRESERVE));
        }
        if (!orderingRules.contains(OrderingRule.ALPHA)) {
            result = result.thenComparing(buildComparatorForRule(OrderingRule.ALPHA));
        }
        return result;
    }

    @NonNull
    private static Comparator<OrderingKey> buildComparatorForRule(OrderingRule orderingRule) {
        return switch (orderingRule) {
            case PRESERVE -> Comparator.comparingInt(OrderingKey::getSrcStart);
            case ALPHA -> buildAlphaComparator();
            case VISIBILITY_ASC ->
                (left, right) -> Integer.compare(right.getVisibilityRank(), left.getVisibilityRank());
            case VISIBILITY_DESC ->
                (left, right) -> Integer.compare(left.getVisibilityRank(), right.getVisibilityRank());
        };
    }

    @NonNull
    private static Comparator<OrderingKey> buildAlphaComparator() {
        // Compare the alpha sorting rank first. Rank is non-zero only for anonymous initializer
        // blocks (rank 1), ensuring they always sort after all regular named members.
        return (left, right) -> {
            int rankComparison = Integer.compare(left.getAlphaSortingRank(), right.getAlphaSortingRank());
            if (rankComparison != 0) {
                return rankComparison;
            }
            return left.getAlphaKey().compareTo(right.getAlphaKey());
        };
    }
}
