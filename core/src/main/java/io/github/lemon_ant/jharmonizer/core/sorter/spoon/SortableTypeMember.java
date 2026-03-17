package io.github.lemon_ant.jharmonizer.core.sorter.spoon;

import static io.github.lemon_ant.jharmonizer.core.sorter.spoon.SpoonTypeMemberUtils.deriveAlphaKey;
import static io.github.lemon_ant.jharmonizer.core.sorter.spoon.SpoonTypeMemberUtils.deriveAlphaSortingRank;
import static io.github.lemon_ant.jharmonizer.core.sorter.spoon.SpoonTypeMemberUtils.deriveVisibilityRank;
import static io.github.lemon_ant.jharmonizer.core.sorter.spoon.SpoonTypeMemberUtils.extractSourceStart;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import javax.annotation.Nullable;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.Value;
import spoon.reflect.declaration.CtTypeMember;

/**
 * A sortable wrapper around a Spoon {@code CtTypeMember} that caches the ordering key,
 * the representative member for accessor-pair bundling, and the set of declaration dependents
 * within the same member group.
 */
@Value
class SortableTypeMember {

    @NonNull
    CtTypeMember typeMember;

    @NonNull
    OrderingKey orderingKey;

    @NonNull
    SortableTypeMember representativeTypeMember;

    @NonNull
    Set<@NonNull CtTypeMember> orderingDependentsInGroup;

    /**
     * Creates a new SortableTypeMember.
     * @param typeMember the type member
     * @param representativeTypeMember the representative type member
     * @param orderingDependentsInGroup the ordering dependents in group
     * @param orderingKeyProvider the ordering key provider
     */
    SortableTypeMember(
            @NonNull CtTypeMember typeMember,
            @Nullable SortableTypeMember representativeTypeMember,
            @NonNull Set<@NonNull CtTypeMember> orderingDependentsInGroup,
            Function<CtTypeMember, OrderingKey> orderingKeyProvider) {
        this.typeMember = typeMember;
        this.representativeTypeMember = representativeTypeMember == null ? this : representativeTypeMember;
        this.orderingDependentsInGroup = orderingDependentsInGroup;
        this.orderingKey = orderingKeyProvider.apply(typeMember);
    }

    @Override
    @NonNull
    public String toString() {
        return "member=" + describeTypeMember(typeMember)
                + ", orderingKey=" + orderingKey
                + ", representative=" + describeTypeMember(representativeTypeMember.getTypeMember())
                + ", orderingDependentsInGroupCount=" + orderingDependentsInGroup.size();
    }

    @Override
    public boolean equals(@Nullable Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof SortableTypeMember otherSortableTypeMember)) {
            return false;
        }
        return Objects.equals(typeMember, otherSortableTypeMember.typeMember)
                && Objects.equals(orderingKey, otherSortableTypeMember.orderingKey)
                && Objects.equals(
                        representativeTypeMember.getTypeMember(),
                        otherSortableTypeMember.representativeTypeMember.getTypeMember())
                && Objects.equals(orderingDependentsInGroup, otherSortableTypeMember.orderingDependentsInGroup);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                typeMember, orderingKey, representativeTypeMember.getTypeMember(), orderingDependentsInGroup);
    }

    @NonNull
    private static SortableTypeMember.OrderingKey deriveOrderingKey(CtTypeMember typeMember) {
        return new SortableTypeMember.OrderingKey(
                extractSourceStart(typeMember),
                deriveAlphaKey(typeMember),
                deriveAlphaSortingRank(typeMember),
                deriveVisibilityRank(typeMember));
    }

    @NonNull
    private static String describeTypeMember(CtTypeMember typeMember) {
        return typeMember.getClass().getSimpleName() + "@" + System.identityHashCode(typeMember);
    }

    @Value
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    static class OrderingKey {

        /**
         * Returns the ordering key provider.
         * @return the ordering key provider
         */
        @NonNull
        static Function<CtTypeMember, OrderingKey> getOrderingKeyProvider() {
            @SuppressWarnings("PMD.UseConcurrentHashMap")
            Map<CtTypeMember, OrderingKey> typeMember2OrderingKey = new HashMap<>();
            return typeMember ->
                    typeMember2OrderingKey.computeIfAbsent(typeMember, SortableTypeMember::deriveOrderingKey);
        }

        int sourceStart;

        @NonNull
        String alphaKey;

        int alphaSortingRank;

        int visibilityRank;
    }
}
