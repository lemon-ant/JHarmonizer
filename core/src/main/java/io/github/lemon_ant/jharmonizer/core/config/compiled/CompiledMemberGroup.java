package io.github.lemon_ant.jharmonizer.core.config.compiled;

import edu.umd.cs.findbugs.annotations.Nullable;
import io.github.lemon_ant.jharmonizer.core.config.unified.MemberDescriptor;
import io.github.lemon_ant.jharmonizer.core.config.unified.UnifiedSeparator;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

/**
 * Tree node with precompiled include/exclude blocks and children.
 * Fallback to the parent when none of the children matched.
 */
@Value
public class CompiledMemberGroup {
    @NonNull
    List<CompiledMemberGroup> compiledSubGroups; // immutable, ordered

    @NonNull
    CompiledMemberGroupSortingBehavior groupSortingBehavior;

    @Nullable
    String name;

    int orderIndex;

    @NonNull
    CompiledMemberGroupSelectorBlock selectorBlock;

    @NonNull
    UnifiedSeparator separator;

    @Builder
    CompiledMemberGroup(
            @Nullable String name,
            int orderIndex,
            @NonNull CompiledMemberGroupSelectorBlock selectorBlock,
            @NonNull CompiledMemberGroupSortingBehavior groupSortingBehavior,
            @NonNull List<CompiledMemberGroup> compiledSubGroups,
            @NonNull UnifiedSeparator separator) {
        this.name = name;
        this.orderIndex = orderIndex;
        this.selectorBlock = selectorBlock;
        this.groupSortingBehavior = groupSortingBehavior;
        this.compiledSubGroups = Collections.unmodifiableList(compiledSubGroups);
        this.separator = separator;
    }

    public Optional<CompiledMemberGroup> classify(@NonNull MemberDescriptor descriptor) {
        if (!selectorBlock.match(descriptor)) {
            return Optional.empty();
        }
        for (CompiledMemberGroup child : compiledSubGroups) {
            Optional<CompiledMemberGroup> hit = child.classify(descriptor);
            if (hit.isPresent()) {
                return hit;
            }
        }
        return Optional.of(this); // fallback to parent bucket
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof CompiledMemberGroup that)) return false;

        return orderIndex == that.orderIndex
                && Objects.equals(name, that.name)
                && selectorBlock.equals(that.selectorBlock)
                && groupSortingBehavior.equals(that.groupSortingBehavior)
                && compiledSubGroups.equals(that.compiledSubGroups);
    }

    @Override
    public int hashCode() {
        int result = Objects.hashCode(name);
        result = 31 * result + orderIndex;
        result = 31 * result + selectorBlock.hashCode();
        result = 31 * result + groupSortingBehavior.hashCode();
        result = 31 * result + compiledSubGroups.hashCode();
        return result;
    }
}
