// =====================================================================================
// FILE: UnifiedMemberGroup.java
// =====================================================================================
package io.github.lemon_ant.jharmonizer.core.config.unified;

import edu.umd.cs.findbugs.annotations.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import lombok.Builder;
import lombok.NonNull;
import lombok.Singular;
import lombok.Value;

/**
 * A group node in the classification tree. Children are ordered. Post-order indexing will be
 * assigned during compilation. Includes/Excludes carry OR semantics across rule lines.
 */
@Value
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
    UnifiedSelectorBlock selectorBlock;

    /**
     * Sorting behavior hints to be applied within this group.
     */
    @NonNull
    UnifiedSortingBehavior sortingBehavior;

    /**
     * Ordered children (DFS will try children first, then fallback to this node).
     */
    @NonNull
    @Singular
    List<UnifiedMemberGroup> memberSubGroups;

    @Builder
    public UnifiedMemberGroup(
            @Nullable String groupName,
            @NonNull UnifiedSelectorBlock selectorBlock,
            @NonNull UnifiedSortingBehavior sortingBehavior,
            @NonNull @Singular List<UnifiedMemberGroup> memberSubGroups) {
        this.groupName = groupName;
        this.selectorBlock = selectorBlock;
        this.sortingBehavior = sortingBehavior;
        this.memberSubGroups = Collections.unmodifiableList(memberSubGroups);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof UnifiedMemberGroup that)) {
            return false;
        }

        return Objects.equals(groupName, that.groupName)
                && selectorBlock.equals(that.selectorBlock)
                && sortingBehavior.equals(that.sortingBehavior)
                && memberSubGroups.equals(that.memberSubGroups);
    }

    @Override
    public int hashCode() {
        int result = Objects.hashCode(groupName);
        result = 31 * result + selectorBlock.hashCode();
        result = 31 * result + sortingBehavior.hashCode();
        result = 31 * result + memberSubGroups.hashCode();
        return result;
    }
}
