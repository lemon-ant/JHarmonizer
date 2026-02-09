package io.github.lemon_ant.jharmonizer.core.config.compiled;

import io.github.lemon_ant.jharmonizer.core.config.unified.UnifiedSeparator;
import java.util.List;

/**
 * Test-only factory to create a synthetic root group that wraps real root groups.
 * <p>
 * This makes {@link io.github.lemon_ant.jharmonizer.core.sorter.spoon.NaturalMemberGroupResolver} tests
 * independent from the concrete configuration shape (single vs multiple root groups).
 */
@Deprecated
public final class CompiledMemberGroupCreator {

    private CompiledMemberGroupCreator() {}

    public static CompiledMemberGroup createSyntheticRoot(
            String syntheticGroupName, List<CompiledMemberGroup> rootGroups) {
        return CompiledMemberGroup.builder()
                .name(syntheticGroupName)
                .orderIndex(-1)
                .sortKeys(List.of(SortKey.PRESERVE))
                .keepAccessorsTogether(false)
                .compiledSubGroups(rootGroups)
                .selectorBlock(new CompiledMemberGroupSelectorBlock(List.of(), List.of()))
                .separator(UnifiedSeparator.NONE)
                .build();
    }
}
