package io.github.lemon_ant.jharmonizer.core.sorter.spoon;

import io.github.lemon_ant.jharmonizer.core.config.compiled.OrderingRule;
import java.util.Comparator;
import java.util.List;
import lombok.NonNull;
import lombok.experimental.UtilityClass;

/**
 * Internal factory methods for comparators used to order type members within member groups.
 *
 * <p>Two comparator families are provided, distinguished by the key type they operate on:
 * <ul>
 *   <li>{@link #buildOrderingComparator(List)} — returns a {@code Comparator<ClusteredOrderingKey>}
 *       that is cluster-aware: for two accessors of <em>different</em> property clusters it
 *       substitutes per-cluster representative attributes (top member's {@code srcStart} /
 *       {@code visibilityRank}) and, for the ALPHA rule, compares {@code propertyName} instead
 *       of the full method-signature {@code alphaKey}. Every other comparison uses the member's
 *       own attributes.</li>
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
     * Builds the cluster-aware comparator for ordering sortable type members by their
     * {@link ClusteredOrderingKey}s.
     *
     * @param orderingRules the ordering rules to apply, in priority order
     * @return the cluster-aware ordering key comparator
     */
    @NonNull
    static Comparator<ClusteredOrderingKey> buildOrderingComparator(@NonNull List<OrderingRule> orderingRules) {
        Comparator<ClusteredOrderingKey> configured = orderingRules.stream()
                .map(ComparatorUtils::buildClusteredComparatorForRule)
                .reduce(Comparator::thenComparing)
                .orElseGet(() -> Comparator.<ClusteredOrderingKey>comparingInt(ClusteredOrderingKey::getSrcStart)
                        .thenComparing(ClusteredOrderingKey::getAlphaKey));
        return appendClusteredTieBreakers(configured, orderingRules);
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
                .orElseGet(() -> Comparator.<MemberOrderingKey>comparingInt(MemberOrderingKey::getSrcStart)
                        .thenComparing(MemberOrderingKey::getAlphaKey));
        return appendMemberOnlyTieBreakers(configured, orderingRules);
    }

    /**
     * Appends deterministic PRESERVE/ALPHA tie-breakers (using member-own attributes only)
     * to the configured clustered comparator so that residual nondeterminism after the
     * configured rules is eliminated.
     */
    @NonNull
    private static Comparator<ClusteredOrderingKey> appendClusteredTieBreakers(
            Comparator<ClusteredOrderingKey> configured, List<OrderingRule> orderingRules) {
        Comparator<ClusteredOrderingKey> result = configured;
        if (!orderingRules.contains(OrderingRule.PRESERVE)) {
            // Tie-breaker always uses own srcStart; cluster substitution must not apply here.
            result = result.thenComparing(buildMemberOnlyComparatorForRule(OrderingRule.PRESERVE));
        }
        if (!orderingRules.contains(OrderingRule.ALPHA)) {
            result = result.thenComparing(buildMemberOnlyComparatorForRule(OrderingRule.ALPHA));
        }
        return result;
    }

    /**
     * Appends deterministic PRESERVE/ALPHA tie-breakers to the configured member-only
     * comparator so that residual nondeterminism after the configured rules is eliminated.
     */
    @NonNull
    private static Comparator<MemberOrderingKey> appendMemberOnlyTieBreakers(
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

    @NonNull
    private static Comparator<ClusteredOrderingKey> buildClusteredComparatorForRule(OrderingRule orderingRule) {
        return switch (orderingRule) {
            case PRESERVE ->
                (left, right) -> {
                    boolean cross = isCrossCluster(left, right);
                    return Integer.compare(
                            cross ? left.getClusterSrcStart() : left.getSrcStart(),
                            cross ? right.getClusterSrcStart() : right.getSrcStart());
                };
            case ALPHA -> buildClusteredAlphaComparator();
            case VISIBILITY_ASC ->
                (left, right) -> {
                    boolean cross = isCrossCluster(left, right);
                    int leftRank = cross ? left.getClusterVisibilityRank() : left.getVisibilityRank();
                    int rightRank = cross ? right.getClusterVisibilityRank() : right.getVisibilityRank();
                    return Integer.compare(rightRank, leftRank);
                };
            case VISIBILITY_DESC ->
                (left, right) -> {
                    boolean cross = isCrossCluster(left, right);
                    int leftRank = cross ? left.getClusterVisibilityRank() : left.getVisibilityRank();
                    int rightRank = cross ? right.getClusterVisibilityRank() : right.getVisibilityRank();
                    return Integer.compare(leftRank, rightRank);
                };
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
    private static Comparator<ClusteredOrderingKey> buildClusteredAlphaComparator() {
        // Compare the alpha sorting rank first. Rank is non-zero only for anonymous
        // initializer blocks (rank 1), ensuring they always sort after all regular
        // named members. Cross-cluster accessor pairs are always methods, so their rank
        // remains the regular member-own rank (0) and does not need a cluster-level copy.
        return (left, right) -> {
            int rankComparison = Integer.compare(left.getAlphaSortingRank(), right.getAlphaSortingRank());
            if (rankComparison != 0) {
                return rankComparison;
            }
            if (isCrossCluster(left, right)) {
                // Cross-cluster ALPHA compares property names; both sides are guaranteed
                // non-null by isCrossCluster.
                return left.getPropertyName().compareTo(right.getPropertyName());
            }
            // Same cluster, or at least one non-accessor: compare full method-signature
            // alphaKeys.
            return left.getAlphaKey().compareTo(right.getAlphaKey());
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
     * Returns {@code true} when both keys belong to <em>different</em> non-null accessor
     * property clusters. This is the only case where cluster-key substitutions apply.
     */
    private static boolean isCrossCluster(ClusteredOrderingKey left, ClusteredOrderingKey right) {
        String leftPropertyName = left.getPropertyName();
        String rightPropertyName = right.getPropertyName();
        return leftPropertyName != null && rightPropertyName != null && !leftPropertyName.equals(rightPropertyName);
    }
}
