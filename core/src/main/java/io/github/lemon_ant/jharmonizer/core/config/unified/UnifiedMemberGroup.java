package io.github.lemon_ant.jharmonizer.core.config.unified;

import edu.umd.cs.findbugs.annotations.Nullable;
import java.util.List;
import java.util.Objects;
import lombok.Builder;
import lombok.NonNull;
import lombok.Singular;
import lombok.Value;

/**
 * A group node in the classification tree. Children are ordered. Includes/Excludes carry OR semantics across rule lines.
 */
@Value
// TODO Remove builder
@Builder
public class UnifiedMemberGroup {

    /**
     * Stable, human-readable identifier of the group.
     */
    @Nullable
    String groupName;

    /**
     * Selectors for acceptance logic inside this node.
     */
    @NonNull
    UnifiedMemberGroupSelectorBlock selectorBlock;

    /**
     * Sorting behavior hints to be applied within this group.
     */
    @NonNull
    UnifiedSortingBehavior sortingBehavior;

    /**
     * Ordered list of child groups.
     */
    @NonNull
    @Singular
    List<UnifiedMemberGroup> memberSubGroups;

    /**
     * Separator directive propagated from vendor config and used at the rendering stage.
     */
    @NonNull
    UnifiedSeparator separator;

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof UnifiedMemberGroup that)) {
            return false;
        }

        return Objects.equals(groupName, that.groupName)
                && selectorBlock.equals(that.selectorBlock)
                && sortingBehavior.equals(that.sortingBehavior)
                && memberSubGroups.equals(that.memberSubGroups)
                && Objects.equals(separator, that.separator);
    }

    @Override
    public int hashCode() {
        int result = Objects.hashCode(groupName);
        result = 31 * result + selectorBlock.hashCode();
        result = 31 * result + sortingBehavior.hashCode();
        result = 31 * result + memberSubGroups.hashCode();
        result = 31 * result + Objects.hashCode(separator);
        return result;
    }
}
