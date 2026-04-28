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
 * <p>Two related comparators are produced:
 * <ul>
 *   <li>{@link #buildOrderingComparator(List)} — the cluster-aware comparator used to order
 *       group members and to compare accessor super-clusters against non-accessors. For two
 *       accessors of <em>different</em> property clusters it substitutes per-cluster
 *       representative attributes (top member's {@code srcStart} / {@code visibilityRank})
 *       and, for the ALPHA rule, compares {@code propertyName} instead
 *       of the full method-signature {@code alphaKey}. Every other comparison falls through to
 *       the member's own attributes.</li>
 *   <li>{@link #buildMemberOnlyOrderingComparator(List)} — the same comparator with cluster
 *       substitutions disabled; used internally by
 *       {@link OrderingKey#deriveAll(List, boolean, List)} to discover each cluster's top
 *       member.</li>
 * </ul>
 */
@UtilityClass
class ComparatorUtils {

    /**
     * Builds the cluster-aware comparator used to order sortable type members by their ordering
     * keys.
     *
     * @param orderingRules the ordering rules to apply, in priority order
     * @return the cluster-aware ordering key comparator
     */
    @NonNull
    static Comparator<OrderingKey> buildOrderingComparator(@NonNull List<OrderingRule> orderingRules) {
        return buildOrderingComparator(orderingRules, true);
    }

    /**
     * Builds a member-only comparator from the given ordering rules: the cluster-aware comparator
     * with cross-cluster substitutions disabled. Used by
     * {@link OrderingKey#deriveAll(List, boolean, List)} for discovering each cluster's top member.
     *
     * @param orderingRules the ordering rules to apply, in priority order
     * @return the member-only ordering key comparator
     */
    @NonNull
    static Comparator<OrderingKey> buildMemberOnlyOrderingComparator(@NonNull List<OrderingRule> orderingRules) {
        return buildOrderingComparator(orderingRules, false);
    }

    @NonNull
    private static Comparator<OrderingKey> buildOrderingComparator(
            List<OrderingRule> orderingRules, boolean clusterAware) {
        Comparator<OrderingKey> configuredComparator = orderingRules.stream()
                .map(orderingRule -> buildOrderingComparatorForOrderingRule(orderingRule, clusterAware))
                .reduce(Comparator::thenComparing)
                .orElseGet(() ->
                        Comparator.comparingInt(OrderingKey::getSrcStart).thenComparing(OrderingKey::getAlphaKey));

        return appendDeterministicTieBreakers(configuredComparator, orderingRules);
    }

    /**
     * Appends deterministic PRESERVE/ALPHA tie-breakers to the configured comparator so that
     * residual nondeterminism after the configured rules is removed. Tie-breakers always use
     * each member's own attributes (cluster substitutions disabled): they exist purely to
     * decide ties that the configured rules left unresolved and must not depend on the
     * super-cluster representative.
     */
    @NonNull
    private static Comparator<OrderingKey> appendDeterministicTieBreakers(
            Comparator<OrderingKey> configuredComparator, List<OrderingRule> orderingRules) {
        Comparator<OrderingKey> result = configuredComparator;
        if (!orderingRules.contains(OrderingRule.PRESERVE)) {
            result = result.thenComparing(buildOrderingComparatorForOrderingRule(OrderingRule.PRESERVE, false));
        }
        if (!orderingRules.contains(OrderingRule.ALPHA)) {
            result = result.thenComparing(buildOrderingComparatorForOrderingRule(OrderingRule.ALPHA, false));
        }
        return result;
    }

    @NonNull
    private static Comparator<OrderingKey> buildOrderingComparatorForOrderingRule(
            OrderingRule orderingRule, boolean clusterAware) {
        return switch (orderingRule) {
            case PRESERVE -> buildPreserveComparator(clusterAware);
            case ALPHA -> buildAlphaComparator(clusterAware);
            case VISIBILITY_ASC -> buildVisibilityComparator(clusterAware, true);
            case VISIBILITY_DESC -> buildVisibilityComparator(clusterAware, false);
        };
    }

    @NonNull
    private static Comparator<OrderingKey> buildPreserveComparator(boolean clusterAware) {
        return (left, right) -> {
            boolean cross = clusterAware && isCrossCluster(left, right);
            return Integer.compare(
                    cross ? left.getClusterSrcStart() : left.getSrcStart(),
                    cross ? right.getClusterSrcStart() : right.getSrcStart());
        };
    }

    @NonNull
    private static Comparator<OrderingKey> buildAlphaComparator(boolean clusterAware) {
        return (left, right) -> {
            boolean cross = clusterAware && isCrossCluster(left, right);
            // Compare the alpha sorting rank first. Rank is non-zero only for anonymous
            // initializer blocks (rank 1), ensuring they always sort after all regular
            // named members. Cross-cluster accessor pairs are always methods, so their rank
            // remains the regular member-own rank (0) and does not need a cluster-level copy.
            int rankComparison = Integer.compare(left.getAlphaSortingRank(), right.getAlphaSortingRank());
            if (rankComparison != 0) {
                return rankComparison;
            }
            if (cross) {
                // Cross-cluster ALPHA compares property names; both sides are guaranteed
                // non-null by isCrossCluster.
                return left.getPropertyName().compareTo(right.getPropertyName());
            }
            // Same cluster, or at least one non-accessor: compare full method-signature
            // alphaKeys. Inside a single cluster this orders members by their natural
            // method names (e.g. getValue < setValue); against non-accessors this lets
            // the super-cluster's representative member compete on its own alphaKey.
            return left.getAlphaKey().compareTo(right.getAlphaKey());
        };
    }

    @NonNull
    private static Comparator<OrderingKey> buildVisibilityComparator(boolean clusterAware, boolean ascending) {
        return (left, right) -> {
            boolean cross = clusterAware && isCrossCluster(left, right);
            int leftRank = cross ? left.getClusterVisibilityRank() : left.getVisibilityRank();
            int rightRank = cross ? right.getClusterVisibilityRank() : right.getVisibilityRank();
            return ascending ? Integer.compare(rightRank, leftRank) : Integer.compare(leftRank, rightRank);
        };
    }

    /**
     * Returns {@code true} when both keys belong to <em>different</em> non-null accessor
     * property clusters. This is the only case where cluster-key substitutions apply.
     */
    private static boolean isCrossCluster(OrderingKey left, OrderingKey right) {
        String leftPropertyName = left.getPropertyName();
        String rightPropertyName = right.getPropertyName();
        return leftPropertyName != null && rightPropertyName != null && !leftPropertyName.equals(rightPropertyName);
    }
}
