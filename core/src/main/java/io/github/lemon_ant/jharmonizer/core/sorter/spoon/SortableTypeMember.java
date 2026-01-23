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
    SortKeyValues sortKeyValues;

    @NonNull
    CtTypeMember representativeTypeMember;

    @NonNull
    Set<@NonNull CtTypeMember> orderingDependentsInGroup;

    public SortableTypeMember(
            @NonNull CtTypeMember typeMember,
            @NonNull CtTypeMember representativeTypeMember,
            @NonNull Set<@NonNull CtTypeMember> orderingDependentsInGroup,
            Function<CtTypeMember, SortKeyValues> sortKeyValuesProvider) {
        this.typeMember = typeMember;
        this.representativeTypeMember = representativeTypeMember;
        this.orderingDependentsInGroup = orderingDependentsInGroup;
        this.sortKeyValues = sortKeyValuesProvider.apply(typeMember);
    }

    static Function<CtTypeMember, SortKeyValues> getSortKeyValuesProvider() {
        @SuppressWarnings("PMD.UseConcurrentHashMap")
        Map<CtTypeMember, SortKeyValues> sortKeyValuesByMember = new HashMap<>();
        return typeMember -> sortKeyValuesByMember.computeIfAbsent(typeMember, SortableTypeMember::deriveSortKeyValues);
    }

    @NonNull
    private static SortableTypeMember.SortKeyValues deriveSortKeyValues(@NonNull CtTypeMember typeMember) {
        return new SortableTypeMember.SortKeyValues(
                extractSourceStart(typeMember), deriveAlphaKey(typeMember), deriveVisibilityRank(typeMember));
    }

    @Value
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    static class SortKeyValues {
        int sourceStart;

        @NonNull
        String alphaKey;

        int visibilityRank;
    }
}
