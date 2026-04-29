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
 * <p>Two comparator families are provided, distinguished by whether cross-accessor-cluster
 * substitutions are applied:
 * <ul>
 *   <li>{@link #buildClusteredOrderingComparator(List)} — returns a
 *       {@code Comparator<MemberOrderingKey>} that is cluster-aware: for two accessors of
 *       <em>different</em> property clusters (both keys are {@link ClusteredOrderingKey}
 *       instances with different property names) it applies each cluster's representative
 *       attributes (top member's {@code srcStart} / {@code visibilityRank}) and, for the ALPHA
 *       rule, compares {@code propertyName} instead of the full method-signature
 *       {@code alphaKey}. All other pairs fall through to the member-only comparator.</li>
 *   <li>{@link #buildMemberOnlyOrderingComparator(List)} — returns a
 *       {@code Comparator<MemberOrderingKey>} that operates solely on member-own attributes
 *       (no cluster substitutions). Used internally by
 *       {@link OrderingKeyFactory#deriveAll(List, boolean, List)} to discover each cluster's top
 *       member, and for top-level type ordering where accessor clustering never applies.</li>
 * </ul>
 */
@UtilityClass
class ComparatorUtils {

    /**
     * Default member-key comparator: PRESERVE then ALPHA by source position and alpha key.
     * Used as the fallback when no ordering rules are configured.
     */
    private static final Comparator<MemberOrderingKey> DEFAULT_MEMBER_KEY_COMPARATOR =
            Comparator.<MemberOrderingKey>comparingInt(MemberOrderingKey::getSrcStart)
                    .thenComparing(MemberOrderingKey::getAlphaKey);

    /**
     * Default cluster-key comparator: PRESERVE then ALPHA by cluster source position and
     * property name. Used as the fallback when no ordering rules are configured.
     */
    private static final Comparator<ClusteredOrderingKey> DEFAULT_CLUSTER_KEY_COMPARATOR =
            Comparator.<ClusteredOrderingKey>comparingInt(ClusteredOrderingKey::getClusterSrcStart)
                    .thenComparing(ClusteredOrderingKey::getPropertyName);

    /**
     * Builds the cluster-aware comparator for ordering sortable type members.
     *
     * <p>Implemented by cascading a cross-cluster-only comparator (which returns {@code 0} for
     * any pair that is not cross-cluster) with the member-only comparator. For cross-cluster
     * pairs, the cluster-only part applies representative attributes (typed as
     * {@link ClusteredOrderingKey}); for all other pairs it returns {@code 0} and the
     * member-only part handles the comparison.
     *
     * @param orderingRules the ordering rules to apply, in priority order
     * @return the cluster-aware ordering key comparator
     */
    @NonNull
    static Comparator<MemberOrderingKey> buildClusteredOrderingComparator(@NonNull List<OrderingRule> orderingRules) {
        Comparator<ClusteredOrderingKey> clusterConfigured = orderingRules.stream()
                .map(ComparatorUtils::buildClusterOnlyComparatorForRule)
                .reduce(Comparator::thenComparing)
                .orElse(DEFAULT_CLUSTER_KEY_COMPARATOR);
        // Cross-cluster part: returns 0 for non-cross-cluster pairs; otherwise delegates to
        // the typed cluster comparator. Both casts are guaranteed safe by isCrossCluster.
        Comparator<MemberOrderingKey> crossClusterPart = (left, right) -> {
            if (!isCrossCluster(left, right)) {
                return 0;
            }
            return clusterConfigured.compare((ClusteredOrderingKey) left, (ClusteredOrderingKey) right);
        };
        // Cascade: cluster rules decide cross-cluster pairs; member-only rules handle everything else.
        return crossClusterPart.thenComparing(buildMemberOnlyOrderingComparator(orderingRules));
    }

    /**
     * Builds a member-only comparator from the given ordering rules, operating solely on
     * member-own attributes with no cross-cluster substitutions. Used by
     * {@link OrderingKeyFactory#deriveAll(List, boolean, List)} for discovering each
     * cluster's top member, and for top-level type ordering.
     *
     * @param orderingRules the ordering rules to apply, in priority order
     * @return the member-only ordering key comparator
     */
    @NonNull
    static Comparator<MemberOrderingKey> buildMemberOnlyOrderingComparator(@NonNull List<OrderingRule> orderingRules) {
        Comparator<MemberOrderingKey> configured = orderingRules.stream()
                .map(ComparatorUtils::buildMemberOnlyComparatorForRule)
                .reduce(Comparator::thenComparing)
                .orElse(DEFAULT_MEMBER_KEY_COMPARATOR);
        return appendTieBreakers(configured, orderingRules);
    }

    /**
     * Appends deterministic PRESERVE/ALPHA tie-breakers (using member-own attributes only)
     * to the configured comparator so that residual nondeterminism after the configured rules
     * is eliminated. Used by both the member-only and clustered comparator paths.
     */
    @NonNull
    private static Comparator<MemberOrderingKey> appendTieBreakers(
            Comparator<MemberOrderingKey> configured, List<OrderingRule> orderingRules) {
        Comparator<MemberOrderingKey> result = configured;
        if (!orderingRules.contains(OrderingRule.PRESERVE)) {
            result = result.thenComparing(buildMemberOnlyComparatorForRule(OrderingRule.PRESERVE));
        }
        if (!orderingRules.contains(OrderingRule.ALPHA)) {
            result = result.thenComparing(buildMemberOnlyComparatorForRule(OrderingRule.ALPHA));
        }
        return result;
    }

    /**
     * Builds a comparator for a single ordering rule that operates solely on
     * {@link ClusteredOrderingKey} cluster-representative attributes. The result is used as the
     * cluster-only part of the cascaded clustered comparator.
     */
    @NonNull
    private static Comparator<ClusteredOrderingKey> buildClusterOnlyComparatorForRule(OrderingRule orderingRule) {
        return switch (orderingRule) {
            case PRESERVE -> Comparator.comparingInt(ClusteredOrderingKey::getClusterSrcStart);
            // Cross-cluster ALPHA compares property names. The alpha sorting rank check is
            // omitted: accessor methods are always regular named methods (alphaSortingRank 0),
            // so rank is equal across all cross-cluster pairs.
            case ALPHA -> Comparator.comparing(ClusteredOrderingKey::getPropertyName);
            case VISIBILITY_ASC ->
                (left, right) -> Integer.compare(right.getClusterVisibilityRank(), left.getClusterVisibilityRank());
            case VISIBILITY_DESC ->
                (left, right) -> Integer.compare(left.getClusterVisibilityRank(), right.getClusterVisibilityRank());
        };
    }

    @NonNull
    private static Comparator<MemberOrderingKey> buildMemberOnlyComparatorForRule(OrderingRule orderingRule) {
        return switch (orderingRule) {
            case PRESERVE -> Comparator.comparingInt(MemberOrderingKey::getSrcStart);
            case ALPHA -> buildMemberOnlyAlphaComparator();
            case VISIBILITY_ASC ->
                (left, right) -> Integer.compare(right.getVisibilityRank(), left.getVisibilityRank());
            case VISIBILITY_DESC ->
                (left, right) -> Integer.compare(left.getVisibilityRank(), right.getVisibilityRank());
        };
    }

    @NonNull
    private static Comparator<MemberOrderingKey> buildMemberOnlyAlphaComparator() {
        return (left, right) -> {
            int rankComparison = Integer.compare(left.getAlphaSortingRank(), right.getAlphaSortingRank());
            if (rankComparison != 0) {
                return rankComparison;
            }
            return left.getAlphaKey().compareTo(right.getAlphaKey());
        };
    }

    /**
     * Returns {@code true} when both keys are {@link ClusteredOrderingKey} instances belonging to
     * <em>different</em> accessor property clusters. This is the only case where cluster-key
     * substitutions apply.
     */
    static boolean isCrossCluster(MemberOrderingKey left, MemberOrderingKey right) {
        return left instanceof ClusteredOrderingKey leftClustered
                && right instanceof ClusteredOrderingKey rightClustered
                && !leftClustered.getPropertyName().equals(rightClustered.getPropertyName());
    }
}
