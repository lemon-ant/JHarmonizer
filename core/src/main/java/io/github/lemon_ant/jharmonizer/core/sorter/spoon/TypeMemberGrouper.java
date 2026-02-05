package io.github.lemon_ant.jharmonizer.core.sorter.spoon;

import io.github.lemon_ant.jharmonizer.core.config.compiled.CompiledMemberGroup;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.NonNull;
import lombok.experimental.UtilityClass;
import spoon.reflect.declaration.CtTypeMember;

/**
 * Default grouping strategy:
 * - groups by CompiledMemberGroup identity,
 * - orders blocks by group.orderIndex,
 * - uses group.separator for the block.
 */
@UtilityClass
// TODO .filter(typeMember -> !typeMember.isImplicit())
class TypeMemberGrouper {

    @NonNull
    List<@NonNull MemberGroupBlock> groupMembersByEffectiveGroups(
            @NonNull Map<@NonNull CtTypeMember, @NonNull CompiledMemberGroup> member2effectiveGroup) {
        Map<CompiledMemberGroup, List<CtTypeMember>> membersByGroup = member2effectiveGroup.entrySet().stream()
                .collect(Collectors.groupingBy(
                        Map.Entry::getValue,
                        HashMap::new,
                        Collectors.mapping(Map.Entry::getKey, Collectors.toUnmodifiableList())));

        return membersByGroup.entrySet().stream()
                .sorted(Comparator.comparingInt(entry -> entry.getKey().getOrderIndex()))
                .map(entry -> new MemberGroupBlock(entry.getKey(), entry.getValue()))
                .toList();
    }
}
