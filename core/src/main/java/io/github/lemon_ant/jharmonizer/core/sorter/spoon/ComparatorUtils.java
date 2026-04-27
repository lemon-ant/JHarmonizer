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
                    // When both members expose a derived accessor property name, compare by that
                    // shared property first so accessors sort by the underlying property
                    // (e.g. "clientId") rather than by the method-name prefix
                    // (get/is/has/set). If either side has no derived property name, or the
                    // property names are equal, fall back to the full method-signature alphaKey.
                    if (left.getClusterPropertyName() != null && right.getClusterPropertyName() != null) {
                        int keyCmp = left.getClusterPropertyName().compareTo(right.getClusterPropertyName());
                        if (keyCmp != 0) {
                            return keyCmp;
                        }
                    }
                    // Tie-break equal derived property names, and handle members without a
                    // derived property name, using the full method-signature alphaKey.
                    return left.getAlphaKey().compareTo(right.getAlphaKey());
                };
            case VISIBILITY_ASC ->
                Comparator.comparingInt(SortableTypeMember.OrderingKey::getVisibilityRank)
                        .reversed();
            case VISIBILITY_DESC -> Comparator.comparingInt(SortableTypeMember.OrderingKey::getVisibilityRank);
        };
    }
}
