package io.github.lemon_ant.jharmonizer.core.sorter.spoon;

import static io.github.lemon_ant.jharmonizer.core.sorter.spoon.SpoonTypeMemberUtils.deriveAlphaKey;
import static io.github.lemon_ant.jharmonizer.core.sorter.spoon.SpoonTypeMemberUtils.deriveAlphaSortingRank;
import static io.github.lemon_ant.jharmonizer.core.sorter.spoon.SpoonTypeMemberUtils.deriveVisibilityRank;
import static io.github.lemon_ant.jharmonizer.core.sorter.spoon.SpoonTypeMemberUtils.extractSourceStart;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;
import lombok.Value;
import spoon.reflect.declaration.CtTypeMember;

@Getter
@EqualsAndHashCode(exclude = "representative")
@ToString(exclude = "representative")
final class SortableTypeMember {

    @NonNull
    private final CtTypeMember typeMember;

    @NonNull
    private final OrderingKey orderingKey;

    @NonNull
    private final SortableTypeMember representative;

    @NonNull
    private final Set<@NonNull CtTypeMember> orderingDependentsInGroup;

    SortableTypeMember(
            @NonNull CtTypeMember typeMember,
            @NonNull SortableTypeMember representative,
            @NonNull Set<@NonNull CtTypeMember> orderingDependentsInGroup,
            @NonNull Function<CtTypeMember, OrderingKey> orderingKeyProvider) {
        this.typeMember = typeMember;
        this.representative = representative;
        this.orderingDependentsInGroup = orderingDependentsInGroup;
        this.orderingKey = orderingKeyProvider.apply(typeMember);
    }

    private SortableTypeMember(
            @NonNull CtTypeMember typeMember,
            @NonNull Set<@NonNull CtTypeMember> orderingDependentsInGroup,
            @NonNull Function<CtTypeMember, OrderingKey> orderingKeyProvider) {
        this.typeMember = typeMember;
        this.representative = this;
        this.orderingDependentsInGroup = orderingDependentsInGroup;
        this.orderingKey = orderingKeyProvider.apply(typeMember);
    }

    static SortableTypeMember createSelfRepresentative(
            @NonNull CtTypeMember typeMember,
            @NonNull Set<@NonNull CtTypeMember> orderingDependentsInGroup,
            @NonNull Function<CtTypeMember, OrderingKey> orderingKeyProvider) {
        return new SortableTypeMember(typeMember, orderingDependentsInGroup, orderingKeyProvider);
    }

    @SuppressWarnings("PMD.CompareObjectsWithEquals")
    boolean isSelfRepresentative() {
        return representative == this;
    }

    static Function<CtTypeMember, OrderingKey> getOrderingKeyProvider() {
        @SuppressWarnings("PMD.UseConcurrentHashMap")
        Map<CtTypeMember, OrderingKey> orderingKeyByMember = new HashMap<>();
        return typeMember -> orderingKeyByMember.computeIfAbsent(typeMember, SortableTypeMember::deriveOrderingKey);
    }

    @NonNull
    private static SortableTypeMember.OrderingKey deriveOrderingKey(CtTypeMember typeMember) {
        return new SortableTypeMember.OrderingKey(
                extractSourceStart(typeMember),
                deriveAlphaKey(typeMember),
                deriveAlphaSortingRank(typeMember),
                deriveVisibilityRank(typeMember));
    }

    @Value
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    static class OrderingKey {
        int sourceStart;

        @NonNull
        String alphaKey;

        int alphaSortingRank;

        int visibilityRank;
    }
}
