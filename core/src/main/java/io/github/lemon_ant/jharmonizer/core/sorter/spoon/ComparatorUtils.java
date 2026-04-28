// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
package io.github.lemon_ant.jharmonizer.core.sorter.spoon;

import io.github.lemon_ant.jharmonizer.core.config.compiled.OrderingRule;
import io.github.lemon_ant.jharmonizer.core.sorter.spoon.SortableTypeMember.OrderingKey;
import io.github.lemon_ant.jharmonizer.core.sorter.spoon.SortableTypeMember.OrderingKey.AccessorClusterOrderingKey;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;
import java.util.function.ToIntFunction;
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
        return buildOrderingComparator(orderingRules, false);
    }

    /**
     * Builds the comparator used when accessor-cluster metadata is available.
     *
     * @param orderingRules the ordering rules to apply
     * @return the accessor-aware ordering key comparator
     */
    @NonNull
    static Comparator<SortableTypeMember.OrderingKey> buildAccessorClusterOrderingComparator(
            @NonNull List<OrderingRule> orderingRules) {
        Comparator<OrderingKey> memberComparator = buildOrderingComparator(orderingRules, false);
        Comparator<OrderingKey> accessorClusterRepresentativeComparator =
                buildOrderingComparator(orderingRules, true, false);
        Comparator<OrderingKey> accessorPropertyClusterComparator = buildOrderingComparator(orderingRules, true, true);
        return (left, right) -> selectAccessorOrderingComparator(
                        left,
                        right,
                        memberComparator,
                        accessorClusterRepresentativeComparator,
                        accessorPropertyClusterComparator)
                .compare(left, right);
    }

    @NonNull
    private static Comparator<OrderingKey> selectAccessorOrderingComparator(
            OrderingKey left,
            OrderingKey right,
            Comparator<OrderingKey> memberComparator,
            Comparator<OrderingKey> accessorClusterRepresentativeComparator,
            Comparator<OrderingKey> accessorPropertyClusterComparator) {
        if (shouldCompareByAccessorPropertyCluster(left, right)) {
            return accessorPropertyClusterComparator;
        }
        return shouldCompareByAccessorSuperCluster(left, right)
                ? accessorClusterRepresentativeComparator
                : memberComparator;
    }

    @NonNull
    private static Comparator<SortableTypeMember.OrderingKey> buildOrderingComparator(
            List<OrderingRule> orderingRules, boolean useAccessorClusterKeys) {
        return buildOrderingComparator(orderingRules, useAccessorClusterKeys, false);
    }

    @NonNull
    private static Comparator<SortableTypeMember.OrderingKey> buildOrderingComparator(
            List<OrderingRule> orderingRules, boolean useAccessorClusterKeys, boolean useAccessorPropertyClusterKeys) {
        Comparator<SortableTypeMember.OrderingKey> configuredComparator = orderingRules.stream()
                .map(orderingRule -> buildOrderingComparatorForOrderingRule(
                        orderingRule, useAccessorClusterKeys, useAccessorPropertyClusterKeys))
                .reduce(Comparator::thenComparing)
                .orElse(buildDefaultOrderingComparator(useAccessorClusterKeys, useAccessorPropertyClusterKeys));

        // Deterministic tie-breakers regardless of configured keys.
        if (!orderingRules.contains(OrderingRule.PRESERVE)) {
            configuredComparator = configuredComparator.thenComparing(buildOrderingComparatorForOrderingRule(
                    OrderingRule.PRESERVE, useAccessorClusterKeys, useAccessorPropertyClusterKeys));
        }
        if (!orderingRules.contains(OrderingRule.ALPHA)) {
            configuredComparator = configuredComparator.thenComparing(buildOrderingComparatorForOrderingRule(
                    OrderingRule.ALPHA, useAccessorClusterKeys, useAccessorPropertyClusterKeys));
        }

        return configuredComparator;
    }

    @NonNull
    private static Comparator<OrderingKey> buildDefaultOrderingComparator(
            boolean useAccessorClusterKeys, boolean useAccessorPropertyClusterKeys) {
        return buildOrderingComparatorForOrderingRule(
                        OrderingRule.PRESERVE, useAccessorClusterKeys, useAccessorPropertyClusterKeys)
                .thenComparing(buildOrderingComparatorForOrderingRule(
                        OrderingRule.ALPHA, useAccessorClusterKeys, useAccessorPropertyClusterKeys));
    }

    @NonNull
    private static Comparator<OrderingKey> buildOrderingComparatorForOrderingRule(
            OrderingRule orderingRule, boolean useAccessorClusterKeys, boolean useAccessorPropertyClusterKeys) {
        ToIntFunction<OrderingKey> srcStartResolver;
        Function<OrderingKey, String> alphaKeyResolver;
        ToIntFunction<OrderingKey> visibilityRankResolver;
        if (!useAccessorClusterKeys) {
            srcStartResolver = OrderingKey::getSrcStart;
            alphaKeyResolver = OrderingKey::getAlphaKey;
            visibilityRankResolver = OrderingKey::getVisibilityRank;
        } else if (useAccessorPropertyClusterKeys) {
            srcStartResolver = OrderingKey::resolveAccessorPropertySrcStart;
            alphaKeyResolver = OrderingKey::resolveAccessorPropertyAlphaKey;
            visibilityRankResolver = OrderingKey::resolveAccessorPropertyVisibilityRank;
        } else {
            srcStartResolver = OrderingKey::resolveClusterSrcStart;
            alphaKeyResolver = OrderingKey::resolveClusterAlphaKey;
            visibilityRankResolver = OrderingKey::resolveClusterVisibilityRank;
        }
        return switch (orderingRule) {
            case PRESERVE ->
                (left, right) -> Integer.compare(srcStartResolver.applyAsInt(left), srcStartResolver.applyAsInt(right));
            case ALPHA ->
                (left, right) -> {
                    int rankComparison = Integer.compare(left.getAlphaSortingRank(), right.getAlphaSortingRank());
                    if (rankComparison != 0) {
                        return rankComparison;
                    }
                    return alphaKeyResolver.apply(left).compareTo(alphaKeyResolver.apply(right));
                };
            case VISIBILITY_ASC ->
                (left, right) -> Integer.compare(
                        visibilityRankResolver.applyAsInt(right), visibilityRankResolver.applyAsInt(left));
            case VISIBILITY_DESC ->
                (left, right) -> Integer.compare(
                        visibilityRankResolver.applyAsInt(left), visibilityRankResolver.applyAsInt(right));
        };
    }

    private static boolean shouldCompareByAccessorPropertyCluster(
            OrderingKey orderingKey, OrderingKey otherOrderingKey) {
        if (orderingKey instanceof AccessorClusterOrderingKey leftClusterKey
                && otherOrderingKey instanceof AccessorClusterOrderingKey rightClusterKey) {
            return !leftClusterKey.getClusterPropertyName().equals(rightClusterKey.getClusterPropertyName());
        }
        return false;
    }

    private static boolean shouldCompareByAccessorSuperCluster(OrderingKey orderingKey, OrderingKey otherOrderingKey) {
        if (orderingKey instanceof AccessorClusterOrderingKey
                && otherOrderingKey instanceof AccessorClusterOrderingKey) {
            return false;
        }
        return orderingKey instanceof AccessorClusterOrderingKey
                || otherOrderingKey instanceof AccessorClusterOrderingKey;
    }
}
