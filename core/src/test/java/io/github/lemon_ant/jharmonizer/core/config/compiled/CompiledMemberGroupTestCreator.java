// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
package io.github.lemon_ant.jharmonizer.core.config.compiled;

import io.github.lemon_ant.jharmonizer.core.config.unified.UnifiedSeparator;
import java.util.List;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.NonNull;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class CompiledMemberGroupTestCreator {

    @NonNull
    public static CompiledMemberGroup createCompiledMemberGroup(
            @NonNull String groupName, boolean keepAccessorsTogether, @NonNull List<OrderingRule> orderingRules) {
        return CompiledMemberGroup.builder()
                .name(groupName)
                .orderIndex(1)
                .keepAccessorsTogether(keepAccessorsTogether)
                .relaxedForwardReferences(true)
                .orderingRules(orderingRules)
                .compiledSubGroups(List.of())
                .selectorBlock(new CompiledMemberGroupSelectorBlock(List.of(), List.of()))
                .separator(UnifiedSeparator.NONE)
                .build();
    }

    @NonNull
    public static CompiledMemberGroup createTrivialMemberGroup(
            @NonNull String groupName, boolean keepAccessorsTogether) {
        return createTrivialMemberGroup(groupName, keepAccessorsTogether, 0);
    }

    @NonNull
    public static CompiledMemberGroup createTrivialMemberGroup(
            @NonNull String groupName, boolean keepAccessorsTogether, int orderIndex) {
        return createTrivialMemberGroup(groupName, keepAccessorsTogether, orderIndex, UnifiedSeparator.NONE);
    }

    @NonNull
    public static CompiledMemberGroup createTrivialMemberGroup(
            @NonNull String groupName,
            boolean keepAccessorsTogether,
            int orderIndex,
            @NonNull UnifiedSeparator separator) {
        CompiledMemberGroupSelectorBlock selectorBlock = new CompiledMemberGroupSelectorBlock(List.of(), List.of());

        return CompiledMemberGroup.builder()
                .compiledSubGroups(List.of())
                .keepAccessorsTogether(keepAccessorsTogether)
                .relaxedForwardReferences(true)
                .orderIndex(orderIndex)
                .name(groupName)
                .selectorBlock(selectorBlock)
                .separator(separator)
                .orderingRule(OrderingRule.PRESERVE)
                .build();
    }
}
