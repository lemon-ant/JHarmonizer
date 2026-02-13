package io.github.lemon_ant.jharmonizer.core.config.compiled;

import io.github.lemon_ant.jharmonizer.core.config.unified.UnifiedSeparator;
import java.util.List;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.NonNull;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class CompiledMemberGroupTestCreator {

    @NonNull
    public static CompiledMemberGroup createTrivialMemberGroup(
            @NonNull String groupName, boolean keepAccessorsTogether) {
        return createTrivialMemberGroup(groupName, keepAccessorsTogether, 0);
    }

    @NonNull
    public static CompiledMemberGroup createTrivialMemberGroup(
            @NonNull String groupName, boolean keepAccessorsTogether, int orderIndex) {
        CompiledMemberGroupSelectorBlock selectorBlock = new CompiledMemberGroupSelectorBlock(List.of(), List.of());

        return CompiledMemberGroup.builder()
                .compiledSubGroups(List.of())
                .keepAccessorsTogether(keepAccessorsTogether)
                .orderIndex(orderIndex)
                .name(groupName)
                .selectorBlock(selectorBlock)
                .separator(UnifiedSeparator.NONE)
                .sortKey(SortKey.PRESERVE)
                .build();
    }

    public static CompiledMemberGroup createCompiledMemberGroup(
            String groupName, boolean keepAccessorsTogether, List<SortKey> sortKeys) {
        return CompiledMemberGroup.builder()
                .name(groupName)
                .orderIndex(1)
                .keepAccessorsTogether(keepAccessorsTogether)
                .sortKeys(sortKeys)
                .compiledSubGroups(List.of())
                .selectorBlock(new CompiledMemberGroupSelectorBlock(List.of(), List.of()))
                .separator(UnifiedSeparator.NONE)
                .build();
    }
}
