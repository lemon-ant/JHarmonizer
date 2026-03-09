package io.github.lemon_ant.jharmonizer.core.sorter.spoon;

import io.github.lemon_ant.jharmonizer.core.config.compiled.OrderingRule;
import io.github.lemon_ant.jharmonizer.core.sorter.spoon.SortableTypeMember.OrderingKey;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;
import lombok.NonNull;
import lombok.experimental.UtilityClass;
import spoon.reflect.declaration.CtTypeMember;

@UtilityClass
@SuppressWarnings("PMD.TooManyMethods")
class ComparatorUtils {

    static @NonNull Comparator<CtTypeMember> buildTypeMemberBaseComparator(
            @NonNull Function<CtTypeMember, OrderingKey> orderingKeyProvider,
            @NonNull Comparator<SortableTypeMember.OrderingKey> orderingKeyComparator) {
        return Comparator.comparing(orderingKeyProvider, orderingKeyComparator);
    }

    @NonNull
    @SuppressWarnings("PMD.CompareObjectsWithEquals")
    static Comparator<SortableTypeMember> buildGroupComparator(
            @NonNull Comparator<CtTypeMember> typeMemberBaseComparator) {
        return (leftSortable, rightSortable) -> {
            CtTypeMember leftMember = leftSortable.getTypeMember();
            CtTypeMember rightMember = rightSortable.getTypeMember();

            // Comparator contract: same reference must be equal.
            if (leftMember == rightMember) {
                return 0;
            }

            int declarationDependencyComparison = compareByDeclarationDependency(leftSortable, rightSortable);
            if (declarationDependencyComparison != 0) {
                return declarationDependencyComparison;
            }

            int representativeComparison =
                    compareByRepresentatives(leftSortable, rightSortable, typeMemberBaseComparator);
            if (representativeComparison != 0) {
                return representativeComparison;
            }
            return compareByBaseComparatorOrThrow(leftSortable, rightSortable, typeMemberBaseComparator);
        };
    }

    @NonNull
    static Comparator<SortableTypeMember.OrderingKey> buildOrderingComparator(
            @NonNull List<OrderingRule> orderingRules) {
        Comparator<SortableTypeMember.OrderingKey> configuredComparator = orderingRules.stream()
                .map(ComparatorUtils::buildOrderingComparatorForOrderingRule)
                .reduce(Comparator::thenComparing)
                .orElseGet(() -> Comparator.comparingInt(SortableTypeMember.OrderingKey::getSourceStart)
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

    private static int compareByDeclarationDependency(
            SortableTypeMember leftSortable, SortableTypeMember rightSortable) {
        CtTypeMember leftMember = leftSortable.getTypeMember();
        CtTypeMember rightMember = rightSortable.getTypeMember();

        boolean leftMustBeBeforeRight =
                leftSortable.getOrderingDependentsInGroup().contains(rightMember);

        boolean rightMustBeBeforeLeft =
                rightSortable.getOrderingDependentsInGroup().contains(leftMember);

        if (leftMustBeBeforeRight && !rightMustBeBeforeLeft) {
            return -1;
        }
        if (rightMustBeBeforeLeft && !leftMustBeBeforeRight) {
            return 1;
        }

        if (leftMustBeBeforeRight) {
            throw new IllegalStateException(composeCyclicDeclarationDependencyMessage(leftSortable, rightSortable));
        }

        return 0;
    }

    @SuppressWarnings("PMD.CompareObjectsWithEquals")
    private static int compareByRepresentatives(
            SortableTypeMember leftSortable,
            SortableTypeMember rightSortable,
            Comparator<CtTypeMember> typeMemberBaseComparator) {
        CtTypeMember leftRepresentative = leftSortable.getRepresentativeTypeMember();
        CtTypeMember rightRepresentative = rightSortable.getRepresentativeTypeMember();

        if (leftRepresentative == rightRepresentative) {
            return 0;
        }

        int representativeComparison = typeMemberBaseComparator.compare(leftRepresentative, rightRepresentative);
        if (representativeComparison != 0) {
            return representativeComparison;
        }

        throw new IllegalStateException(composeEqualRepresentativesMessage(leftSortable, rightSortable));
    }

    private static int compareByBaseComparatorOrThrow(
            SortableTypeMember leftSortable,
            SortableTypeMember rightSortable,
            Comparator<CtTypeMember> typeMemberBaseComparator) {
        int directComparison =
                typeMemberBaseComparator.compare(leftSortable.getTypeMember(), rightSortable.getTypeMember());
        if (directComparison != 0) {
            return directComparison;
        }

        throw new IllegalStateException(composeEqualMembersMessage(leftSortable, rightSortable));
    }

    @NonNull
    private static Comparator<SortableTypeMember.OrderingKey> buildOrderingComparatorForOrderingRule(
            OrderingRule orderingRule) {
        return switch (orderingRule) {
            case PRESERVE -> Comparator.comparingInt(SortableTypeMember.OrderingKey::getSourceStart);
            case ALPHA -> Comparator.comparingInt(SortableTypeMember.OrderingKey::getAlphaSortingRank)
                    .thenComparing(SortableTypeMember.OrderingKey::getAlphaKey);
            case VISIBILITY_ASC -> Comparator.comparingInt(SortableTypeMember.OrderingKey::getVisibilityRank);
            case VISIBILITY_DESC ->
                buildOrderingComparatorForOrderingRule(OrderingRule.VISIBILITY_ASC)
                        .reversed();
        };
    }

    @NonNull
    private static String composeCyclicDeclarationDependencyMessage(
            SortableTypeMember leftSortable, SortableTypeMember rightSortable) {
        return "Detected a cyclic DECLARATION_DEPENDENCY ordering inside a member group. "
                + "Both members are mutually reachable via declaration dependency edges, so a strict provider-before-dependent "
                + "order cannot be derived for this pair.\n"
                + "Left:  " + describeSortableTypeMember(leftSortable) + "\n"
                + "Right: " + describeSortableTypeMember(rightSortable) + "\n"
                + "Hint: validate and report cycles in MemberDependencyGraph (or in a dedicated validator) before ordering.";
    }

    @NonNull
    private static String describeTypeMemberForDebug(CtTypeMember typeMember) {
        return typeMember.getClass().getSimpleName() + "@" + System.identityHashCode(typeMember);
    }

    @NonNull
    private static String composeEqualRepresentativesMessage(
            SortableTypeMember leftSortable, SortableTypeMember rightSortable) {
        return "Two different representative members compare as equal by the base comparator. "
                + "This breaks deterministic representative ordering.\n"
                + "Left:  " + describeSortableTypeMember(leftSortable) + "\n"
                + "Right: " + describeSortableTypeMember(rightSortable) + "\n"
                + "Left representative:  " + describeTypeMemberForDebug(leftSortable.getRepresentativeTypeMember())
                + "\n"
                + "Right representative: " + describeTypeMemberForDebug(rightSortable.getRepresentativeTypeMember())
                + "\n"
                + "Hint: ensure the OrderingKey comparator has a deterministic tie-breaker for representatives.";
    }

    @NonNull
    private static String describeSortableTypeMember(SortableTypeMember sortableTypeMember) {
        return "member=" + describeTypeMemberForDebug(sortableTypeMember.getTypeMember())
                + ", orderingKey=" + sortableTypeMember.getOrderingKey()
                + ", representative=" + describeTypeMemberForDebug(sortableTypeMember.getRepresentativeTypeMember())
                + ", orderingDependentsInGroupCount="
                + sortableTypeMember.getOrderingDependentsInGroup().size();
    }

    @NonNull
    private static String composeEqualMembersMessage(
            SortableTypeMember leftSortable, SortableTypeMember rightSortable) {
        return "Two distinct members compare as equal by the configured base comparator, which violates deterministic ordering.\n"
                + "Left:  " + describeSortableTypeMember(leftSortable) + "\n"
                + "Right: " + describeSortableTypeMember(rightSortable) + "\n"
                + "Hint: ensure the OrderingKey comparator produces a strict order for distinct members "
                + "(e.g., add a stable tie-breaker when all configured keys match).";
    }
}
