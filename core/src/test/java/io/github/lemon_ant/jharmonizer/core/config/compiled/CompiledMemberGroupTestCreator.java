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
        CompiledMemberGroupSelectorBlock selectorBlock = new CompiledMemberGroupSelectorBlock(List.of(), List.of());

        return CompiledMemberGroup.builder()
                .compiledSubGroups(List.of())
                .keepAccessorsTogether(keepAccessorsTogether)
                .orderIndex(0)
                .name(groupName)
                .selectorBlock(selectorBlock)
                .separator(UnifiedSeparator.NONE)
                .sortKey(SortKey.PRESERVE)
                .build();
    }
}
