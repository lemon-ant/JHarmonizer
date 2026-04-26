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
                    int rankCmp = Integer.compare(left.getAlphaSortingRank(), right.getAlphaSortingRank());
                    if (rankCmp != 0) {
                        return rankCmp;
                    }
                    // For accessor clusters, use the property name as the primary ALPHA key so that
                    // clusters sort by property name instead of by the method-name prefix (get/is/has).
                    // Members not in a cluster use their full method-signature alphaKey.
                    String leftKey =
                            left.getClusterPropertyName() != null ? left.getClusterPropertyName() : left.getAlphaKey();
                    String rightKey = right.getClusterPropertyName() != null
                            ? right.getClusterPropertyName()
                            : right.getAlphaKey();
                    int keyCmp = leftKey.compareTo(rightKey);
                    if (keyCmp != 0) {
                        return keyCmp;
                    }
                    // Tie-break within the same cluster (equal property name): fall back to the method
                    // signature so that e.g. getXxx sorts before setXxx within the bundle.
                    return left.getAlphaKey().compareTo(right.getAlphaKey());
                };
            case VISIBILITY_ASC ->
                Comparator.comparingInt(SortableTypeMember.OrderingKey::getVisibilityRank)
                        .reversed();
            case VISIBILITY_DESC -> Comparator.comparingInt(SortableTypeMember.OrderingKey::getVisibilityRank);
        };
    }
}
