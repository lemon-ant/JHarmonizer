/*
 * SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 */
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
 * Groups type members into {@link MemberGroupBlock}s according to their effective
 * compiled member group, preserving group declaration order.
 */
@UtilityClass
class TypeMemberGrouper {

    /**
     * Performs the group members by effective groups.
     * @param member2effectiveGroup the member2effective group
     * @return the resulting list
     */
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
