package io.github.lemon_ant.jharmonizer.core.sorter.spoon;

import io.github.lemon_ant.jharmonizer.core.config.compiled.OrderingRule;
import io.github.lemon_ant.jharmonizer.core.sorter.spoon.SortableTypeMember.OrderingKey;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;
import lombok.NonNull;
import lombok.experimental.UtilityClass;
import spoon.reflect.declaration.CtTypeMember;

/**
 * Internal factory methods for comparators used to order type members within and across member groups.
 */
@UtilityClass
class ComparatorUtils {

    /**
     * Performs the build type member base comparator.
     * @param orderingKeyProvider the ordering key provider
     * @param orderingKeyComparator the ordering key comparator
     * @return the result
     */
    static @NonNull Comparator<CtTypeMember> buildTypeMemberBaseComparator(
            @NonNull Function<CtTypeMember, OrderingKey> orderingKeyProvider,
            @NonNull Comparator<SortableTypeMember.OrderingKey> orderingKeyComparator) {
        return Comparator.comparing(orderingKeyProvider, orderingKeyComparator);
    }

    /**
     * Builds the comparator used to choose the next eligible grouped type member.
     *
     * @param orderingKeyComparator the comparator for member ordering keys
     * @return the comparator for sortable members
     */
    @NonNull
    static Comparator<SortableTypeMember> buildGroupSelectionComparator(
            @NonNull Comparator<SortableTypeMember.OrderingKey> orderingKeyComparator) {
        return (leftSortable, rightSortable) -> {
            if (leftSortable == rightSortable) {
                return 0;
            }

            int representativeComparison = compareByRepresentatives(leftSortable, rightSortable, orderingKeyComparator);
            if (representativeComparison != 0) {
                return representativeComparison;
            }
            return compareByBaseComparatorOrThrow(leftSortable, rightSortable, orderingKeyComparator);
        };
    }

    /**
     * Performs the build ordering comparator.
     * @param orderingRules the ordering rules
     * @return the result
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

    private static int compareByRepresentatives(
            SortableTypeMember leftSortable,
            SortableTypeMember rightSortable,
            Comparator<SortableTypeMember.OrderingKey> orderingKeyComparator) {
        SortableTypeMember leftRepresentative = leftSortable.getRepresentativeTypeMember();
        SortableTypeMember rightRepresentative = rightSortable.getRepresentativeTypeMember();

        if (leftRepresentative == rightRepresentative) {
            return 0;
        }

        int representativeComparison = orderingKeyComparator.compare(
                leftRepresentative.getOrderingKey(), rightRepresentative.getOrderingKey());
        if (representativeComparison != 0) {
            return representativeComparison;
        }

        throw new IllegalStateException(composeEqualRepresentativesMessage(leftSortable, rightSortable));
    }

    private static int compareByBaseComparatorOrThrow(
            SortableTypeMember leftSortable,
            SortableTypeMember rightSortable,
            Comparator<SortableTypeMember.OrderingKey> orderingKeyComparator) {
        int directComparison =
                orderingKeyComparator.compare(leftSortable.getOrderingKey(), rightSortable.getOrderingKey());
        if (directComparison != 0) {
            return directComparison;
        }

        throw new IllegalStateException(composeEqualMembersMessage(leftSortable, rightSortable));
    }

    @NonNull
    private static Comparator<SortableTypeMember.OrderingKey> buildOrderingComparatorForOrderingRule(
            OrderingRule orderingRule) {
        return switch (orderingRule) {
            case PRESERVE -> Comparator.comparingInt(SortableTypeMember.OrderingKey::getSrcStart);
            case ALPHA ->
                Comparator.comparingInt(SortableTypeMember.OrderingKey::getAlphaSortingRank)
                        .thenComparing(SortableTypeMember.OrderingKey::getAlphaKey);
            case VISIBILITY_ASC ->
                Comparator.comparingInt(SortableTypeMember.OrderingKey::getVisibilityRank)
                        .reversed();
            case VISIBILITY_DESC -> Comparator.comparingInt(SortableTypeMember.OrderingKey::getVisibilityRank);
        };
    }

    @NonNull
    private static String composeEqualRepresentativesMessage(
            SortableTypeMember leftSortable, SortableTypeMember rightSortable) {
        return "Two different representative members compare as equal by the base comparator. "
                + "This breaks deterministic representative ordering.\n"
                + "Left:  " + leftSortable + "\n"
                + "Right: " + rightSortable + "\n"
                + "Left representative:  "
                + leftSortable.getRepresentativeTypeMember()
                + "\n"
                + "Right representative: "
                + rightSortable.getRepresentativeTypeMember()
                + "\n"
                + "Hint: ensure the OrderingKey comparator has a deterministic tie-breaker for representatives.";
    }

    @NonNull
    private static String composeEqualMembersMessage(
            SortableTypeMember leftSortable, SortableTypeMember rightSortable) {
        return "Two distinct members compare as equal by the configured base comparator, which violates deterministic ordering.\n"
                + "Left:  " + leftSortable + "\n"
                + "Right: " + rightSortable + "\n"
                + "Hint: ensure the OrderingKey comparator produces a strict order for distinct members "
                + "(e.g., add a stable tie-breaker when all configured keys match).";
    }
}
