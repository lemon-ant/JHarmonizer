// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
package io.github.lemon_ant.jharmonizer.core.sorter.spoon;

import io.github.lemon_ant.jharmonizer.core.config.compiled.OrderingRule;
import io.github.lemon_ant.jharmonizer.core.sorter.spoon.SortableTypeMember.OrderingKey;
import java.util.Comparator;
import java.util.List;
import lombok.NonNull;
import lombok.experimental.UtilityClass;

/**
 * Internal factory methods for comparators used to order type members within member groups.
 */
@UtilityClass
class ComparatorUtils {

    /**
     * Builds the comparator used to order sortable type members by their ordering keys.
     *
     * @param orderingRules the ordering rules to apply
     * @return the ordering key comparator
     */
    @NonNull
    static Comparator<OrderingKey> buildOrderingComparator(@NonNull List<OrderingRule> orderingRules) {
        Comparator<OrderingKey> configuredComparator = orderingRules.stream()
                .map(ComparatorUtils::buildOrderingComparatorForOrderingRule)
                .reduce(Comparator::thenComparing)
                .orElseGet(ComparatorUtils::buildDefaultOrderingComparator);

        // Deterministic tie-breakers regardless of configured keys.
        if (!orderingRules.contains(OrderingRule.PRESERVE)) {
            configuredComparator =
                    configuredComparator.thenComparing(buildOrderingComparatorForOrderingRule(OrderingRule.PRESERVE));
        }
        if (!orderingRules.contains(OrderingRule.ALPHA)) {
            configuredComparator =
                    configuredComparator.thenComparing(buildOrderingComparatorForOrderingRule(OrderingRule.ALPHA));
        }

        return configuredComparator;
    }

    /**
     * Builds the comparator used when accessor-cluster metadata is available.
     *
     * @param orderingRules the ordering rules to apply
     * @return the accessor-aware ordering key comparator
     */
    @NonNull
    static Comparator<SortableTypeMember> buildAccessorClusterOrderingComparator(
            @NonNull List<OrderingRule> orderingRules) {
        Comparator<OrderingKey> orderingKeyComparator = buildOrderingComparator(orderingRules);
        return (left, right) -> compareByRepresentatives(left, right, orderingKeyComparator);
    }

    @NonNull
    private static Comparator<OrderingKey> buildDefaultOrderingComparator() {
        return buildOrderingComparatorForOrderingRule(OrderingRule.PRESERVE)
                .thenComparing(buildOrderingComparatorForOrderingRule(OrderingRule.ALPHA));
    }

    @NonNull
    private static Comparator<OrderingKey> buildOrderingComparatorForOrderingRule(OrderingRule orderingRule) {
        return switch (orderingRule) {
            case PRESERVE -> (left, right) -> Integer.compare(left.getSrcStart(), right.getSrcStart());
            case ALPHA ->
                (left, right) -> {
                    int rankComparison = Integer.compare(left.getAlphaSortingRank(), right.getAlphaSortingRank());
                    if (rankComparison != 0) {
                        return rankComparison;
                    }
                    return left.getAlphaKey().compareTo(right.getAlphaKey());
                };
            case VISIBILITY_ASC ->
                (left, right) -> Integer.compare(right.getVisibilityRank(), left.getVisibilityRank());
            case VISIBILITY_DESC ->
                (left, right) -> Integer.compare(left.getVisibilityRank(), right.getVisibilityRank());
        };
    }

    private static int compareByRepresentatives(
            SortableTypeMember left, SortableTypeMember right, Comparator<OrderingKey> orderingKeyComparator) {
        if (left.getSuperClusterRepresentative() != right.getSuperClusterRepresentative()) {
            return orderingKeyComparator.compare(
                    left.getSuperClusterRepresentative(), right.getSuperClusterRepresentative());
        }
        if (left.getPropertyClusterRepresentative() != right.getPropertyClusterRepresentative()) {
            return orderingKeyComparator.compare(
                    left.getPropertyClusterRepresentative(), right.getPropertyClusterRepresentative());
        }
        return orderingKeyComparator.compare(left.getOrderingKey(), right.getOrderingKey());
    }
}
