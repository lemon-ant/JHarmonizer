package io.github.lemon_ant.jharmonizer.core.sorter.spoon;

import static io.github.lemon_ant.jharmonizer.core.sorter.spoon.SpoonTypeMemberUtils.deriveAlphaKey;
import static io.github.lemon_ant.jharmonizer.core.sorter.spoon.SpoonTypeMemberUtils.deriveVisibilityRank;
import static io.github.lemon_ant.jharmonizer.core.sorter.spoon.SpoonTypeMemberUtils.extractSourceStart;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.Value;
import spoon.reflect.declaration.CtTypeMember;

@Value
class SortableTypeMember {

    @NonNull
    CtTypeMember typeMember;

    @NonNull
    OrderingRuleValues orderingRuleValues;

    @NonNull
    CtTypeMember representativeTypeMember;

    @NonNull
    Set<@NonNull CtTypeMember> orderingDependentsInGroup;

    SortableTypeMember(
            @NonNull CtTypeMember typeMember,
            @NonNull CtTypeMember representativeTypeMember,
            @NonNull Set<@NonNull CtTypeMember> orderingDependentsInGroup,
            Function<CtTypeMember, OrderingRuleValues> orderingRuleValuesProvider) {
        this.typeMember = typeMember;
        this.representativeTypeMember = representativeTypeMember;
        this.orderingDependentsInGroup = orderingDependentsInGroup;
        this.orderingRuleValues = orderingRuleValuesProvider.apply(typeMember);
    }

    static Function<CtTypeMember, OrderingRuleValues> getOrderingRuleValuesProvider() {
        @SuppressWarnings("PMD.UseConcurrentHashMap")
        Map<CtTypeMember, OrderingRuleValues> orderingRuleValuesByMember = new HashMap<>();
        return typeMember -> orderingRuleValuesByMember.computeIfAbsent(typeMember, SortableTypeMember::deriveOrderingRuleValues);
    }

    @NonNull
    private static SortableTypeMember.OrderingRuleValues deriveOrderingRuleValues(CtTypeMember typeMember) {
        return new SortableTypeMember.OrderingRuleValues(
                extractSourceStart(typeMember), deriveAlphaKey(typeMember), deriveVisibilityRank(typeMember));
    }

    @Value
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    // TODO Remove plural form
    static class OrderingRuleValues {
        int sourceStart;

        @NonNull
        String alphaKey;

        int visibilityRank;
    }
}
