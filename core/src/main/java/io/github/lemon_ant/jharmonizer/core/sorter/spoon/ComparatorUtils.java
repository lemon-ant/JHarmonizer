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
    static Comparator<SortableTypeMember.OrderingKey> buildOrderingComparator(
            @NonNull List<OrderingRule> orderingRules) {
        Comparator<SortableTypeMember.OrderingKey> configuredComparator = orderingRules.stream()
                .map(ComparatorUtils::buildOrderingComparatorForOrderingRule)
                .reduce(Comparator::thenComparing)
                .orElseGet(() -> buildOrderingComparatorForOrderingRule(OrderingRule.PRESERVE)
                        .thenComparing(buildOrderingComparatorForOrderingRule(OrderingRule.ALPHA)));

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

    @NonNull
    private static Comparator<OrderingKey> buildOrderingComparatorForOrderingRule(OrderingRule orderingRule) {
        return switch (orderingRule) {
            case PRESERVE ->
                (left, right) -> Integer.compare(resolveSrcStart(left, right), resolveSrcStart(right, left));
            case ALPHA ->
                (left, right) -> {
                    int rankComparison =
                            Integer.compare(resolveAlphaSortingRank(left, right), resolveAlphaSortingRank(right, left));
                    if (rankComparison != 0) {
                        return rankComparison;
                    }
                    return resolveAlphaKey(left, right).compareTo(resolveAlphaKey(right, left));
                };
            case VISIBILITY_ASC ->
                (left, right) ->
                        Integer.compare(resolveVisibilityRank(right, left), resolveVisibilityRank(left, right));
            case VISIBILITY_DESC ->
                (left, right) ->
                        Integer.compare(resolveVisibilityRank(left, right), resolveVisibilityRank(right, left));
        };
    }

    private static int resolveSrcStart(OrderingKey orderingKey, OrderingKey otherOrderingKey) {
        return shouldCompareByAccessorCluster(orderingKey, otherOrderingKey)
                ? orderingKey.getClusterSrcStart()
                : orderingKey.getSrcStart();
    }

    @NonNull
    private static String resolveAlphaKey(OrderingKey orderingKey, OrderingKey otherOrderingKey) {
        return shouldCompareByAccessorCluster(orderingKey, otherOrderingKey)
                ? orderingKey.getClusterAlphaKey()
                : orderingKey.getAlphaKey();
    }

    private static int resolveAlphaSortingRank(OrderingKey orderingKey, OrderingKey otherOrderingKey) {
        return shouldCompareByAccessorCluster(orderingKey, otherOrderingKey)
                ? orderingKey.getClusterAlphaSortingRank()
                : orderingKey.getAlphaSortingRank();
    }

    private static int resolveVisibilityRank(OrderingKey orderingKey, OrderingKey otherOrderingKey) {
        return shouldCompareByAccessorCluster(orderingKey, otherOrderingKey)
                ? orderingKey.getClusterVisibilityRank()
                : orderingKey.getVisibilityRank();
    }

    private static boolean shouldCompareByAccessorCluster(OrderingKey orderingKey, OrderingKey otherOrderingKey) {
        return orderingKey.getClusterPropertyName() != null
                && otherOrderingKey.getClusterPropertyName() != null
                && !orderingKey.getClusterPropertyName().equals(otherOrderingKey.getClusterPropertyName());
    }
}
