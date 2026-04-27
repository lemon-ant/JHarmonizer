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
                    // Compare by the per-cluster representative alpha key first. For accessor
                    // members that share the same JavaBeans property cluster (when
                    // keepAccessorsTogether is enabled) this representative is the cluster's
                    // minimum alphaKey, so all members of one cluster collapse to a single
                    // primary key and sort contiguously where the alphabetically-first cluster
                    // member would sort. For every other member the representative equals the
                    // member's own alphaKey. This makes the comparator total and transitive
                    // even when accessors of different properties are interleaved with
                    // unrelated non-accessor methods in the same group.
                    int clusterComparison = left.getClusterAlphaKey().compareTo(right.getClusterAlphaKey());
                    if (clusterComparison != 0) {
                        return clusterComparison;
                    }
                    // Tie-break members of the same cluster by their own full alphaKey.
                    return left.getAlphaKey().compareTo(right.getAlphaKey());
                };
            case VISIBILITY_ASC ->
                Comparator.comparingInt(SortableTypeMember.OrderingKey::getVisibilityRank)
                        .reversed();
            case VISIBILITY_DESC -> Comparator.comparingInt(SortableTypeMember.OrderingKey::getVisibilityRank);
        };
    }
}
