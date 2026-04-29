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

    private static final Comparator<OrderingKey> PRESERVE_COMPARATOR =
            Comparator.comparingInt(OrderingKey::getSrcStart);
    private static final Comparator<OrderingKey> ALPHA_COMPARATOR =
            Comparator.comparingInt(OrderingKey::getAlphaSortingRank).thenComparing(OrderingKey::getAlphaKey);
    private static final Comparator<OrderingKey> VISIBILITY_ASC_COMPARATOR =
            Comparator.comparingInt(OrderingKey::getVisibilityRank).reversed();
    private static final Comparator<OrderingKey> VISIBILITY_DESC_COMPARATOR =
            Comparator.comparingInt(OrderingKey::getVisibilityRank);
    private static final Comparator<OrderingKey> DEFAULT_ORDERING_COMPARATOR =
            PRESERVE_COMPARATOR.thenComparing(ALPHA_COMPARATOR);

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
                .orElse(DEFAULT_ORDERING_COMPARATOR);

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

    /**
     * Builds a comparator for one configured ordering rule.
     *
     * @param orderingRule the ordering rule to apply
     * @return the ordering key comparator for the rule
     */
    @NonNull
    private static Comparator<OrderingKey> buildOrderingComparatorForOrderingRule(OrderingRule orderingRule) {
        return switch (orderingRule) {
            case PRESERVE -> PRESERVE_COMPARATOR;
            case ALPHA -> ALPHA_COMPARATOR;
            case VISIBILITY_ASC -> VISIBILITY_ASC_COMPARATOR;
            case VISIBILITY_DESC -> VISIBILITY_DESC_COMPARATOR;
        };
    }

    /**
     * Compares sortable members by super-cluster representative, then property-cluster representative, then own key.
     *
     * @param left the left sortable member
     * @param right the right sortable member
     * @param orderingKeyComparator the comparator for representative and own ordering keys
     * @return the comparison result
     */
    @SuppressWarnings("PMD.CompareObjectsWithEquals")
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
