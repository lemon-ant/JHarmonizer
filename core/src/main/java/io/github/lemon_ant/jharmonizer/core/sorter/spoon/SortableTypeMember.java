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
import lombok.Getter;
import lombok.NonNull;
import lombok.Value;
import spoon.reflect.declaration.CtTypeMember;

@Value
class SortableTypeMember {

    @NonNull
    CtTypeMember typeMember;

    @NonNull
    OrderingKey orderingKey;

    @Getter(AccessLevel.NONE)
    SortableTypeMember representativeTypeMember;

    @NonNull
    Set<@NonNull CtTypeMember> orderingDependentsInGroup;

    SortableTypeMember(
            @NonNull CtTypeMember typeMember,
            SortableTypeMember representativeTypeMember,
            @NonNull Set<@NonNull CtTypeMember> orderingDependentsInGroup,
            Function<CtTypeMember, OrderingKey> orderingKeyProvider) {
        this.typeMember = typeMember;
        this.representativeTypeMember = representativeTypeMember;
        this.orderingDependentsInGroup = orderingDependentsInGroup;
        this.orderingKey = orderingKeyProvider.apply(typeMember);
    }

    @NonNull
    SortableTypeMember getRepresentativeTypeMember() {
        return representativeTypeMember == null ? this : representativeTypeMember;
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
