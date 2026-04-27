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
                .orElseGet(() -> Comparator.comparingInt(SortableTypeMember.OrderingKey::getSrcStart)
                        .thenComparing(SortableTypeMember.OrderingKey::getAlphaKey));

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
            case PRESERVE -> Comparator.comparingInt(SortableTypeMember.OrderingKey::getSrcStart);
            case ALPHA ->
                (left, right) -> {
                    // Compare the primary ALPHA pre-key first. Rank is non-zero only for
                    // anonymous initializer blocks (rank 1), ensuring they always sort
                    // after all named members regardless of their alphabetical position.
                    int rankComparison = Integer.compare(left.getAlphaSortingRank(), right.getAlphaSortingRank());
                    if (rankComparison != 0) {
                        return rankComparison;
                    }
                    // Compare the accessor super-cluster as one unit against non-accessor members
                    // before ordering individual accessor properties inside that unit. This keeps
                    // the comparator transitive when accessor property order disagrees with full
                    // method-name order.
                    int clusterKeyComparison = left.getAlphaClusterKey().compareTo(right.getAlphaClusterKey());
                    if (clusterKeyComparison != 0) {
                        return clusterKeyComparison;
                    }
                    if (left.getClusterPropertyName() != null && right.getClusterPropertyName() != null) {
                        int propertyComparison =
                                left.getClusterPropertyName().compareTo(right.getClusterPropertyName());
                        if (propertyComparison != 0) {
                            return propertyComparison;
                        }
                    }
                    return left.getAlphaKey().compareTo(right.getAlphaKey());
                };
            case VISIBILITY_ASC ->
                Comparator.comparingInt(SortableTypeMember.OrderingKey::getVisibilityRank)
                        .reversed();
            case VISIBILITY_DESC -> Comparator.comparingInt(SortableTypeMember.OrderingKey::getVisibilityRank);
        };
    }
}
