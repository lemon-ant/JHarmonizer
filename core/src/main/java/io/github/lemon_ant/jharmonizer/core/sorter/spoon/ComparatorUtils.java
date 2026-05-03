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

    private static final Comparator<OrderingKey> PRESERVE_COMPARATOR =
            Comparator.comparingInt(OrderingKey::getSrcStart);

    private static final Comparator<OrderingKey> ALPHA_COMPARATOR =
            Comparator.comparingInt(OrderingKey::getAlphaSortingRank).thenComparing(OrderingKey::getAlphaKey);

    private static final Comparator<OrderingKey> VISIBILITY_ASC_COMPARATOR =
            Comparator.comparingInt(OrderingKey::getVisibilityRank).reversed();

    private static final Comparator<OrderingKey> VISIBILITY_DESC_COMPARATOR =
            Comparator.comparingInt(OrderingKey::getVisibilityRank);

    /**
     * Default fallback comparator used when no ordering rules are configured: PRESERVE
     * (source position) followed by ALPHA (alpha key). Tie-breakers are applied on top by
     * {@link #appendTieBreakers(Comparator, List)} when needed, but with no rules at all this
     * default already provides a deterministic ordering.
     */
    private static final Comparator<OrderingKey> DEFAULT_COMPARATOR =
            PRESERVE_COMPARATOR.thenComparing(ALPHA_COMPARATOR);

    /**
     * Builds a comparator over {@link OrderingKey} from the given ordering rules. Tie-breakers
     * (PRESERVE then ALPHA) are appended to ensure deterministic ordering.
     *
     * @param orderingRules the ordering rules to apply, in priority order
     * @return the ordering key comparator
     */
    @NonNull
    static Comparator<OrderingKey> buildOrderingKeyComparator(@NonNull List<OrderingRule> orderingRules) {
        if (orderingRules.isEmpty()) {
            // DEFAULT_COMPARATOR already covers both tie-breakers (PRESERVE then ALPHA).
            return DEFAULT_COMPARATOR;
        }
        Comparator<OrderingKey> configured = orderingRules.stream()
                .map(ComparatorUtils::buildComparatorForRule)
                .reduce(Comparator::thenComparing)
                .orElseThrow();
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
    @SuppressWarnings("PMD.CompareObjectsWithEquals")
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
            case PRESERVE -> PRESERVE_COMPARATOR;
            case ALPHA -> ALPHA_COMPARATOR;
            case VISIBILITY_ASC -> VISIBILITY_ASC_COMPARATOR;
            case VISIBILITY_DESC -> VISIBILITY_DESC_COMPARATOR;
        };
    }
}
