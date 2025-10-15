package io.github.lemon_ant.jharmonizer.core.config.compiled;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import lombok.NonNull;
import lombok.Value;

/**
 * Tree node with precompiled include/exclude blocks and children.
 * Fallback to the parent when none of the children matched.
 */
@Value
public class EffectiveGroup {
    @NonNull
    String name;

    int orderIndex; // assigned post-order

    @NonNull
    CompiledSelectorBlock selectorBlock;

    @NonNull
    GroupSortingBehavior sortingBehavior;

    @NonNull
    List<EffectiveGroup> nestedEffectiveGroups; // immutable, ordered

    public EffectiveGroup(
            @NonNull String name,
            int orderIndex,
            @NonNull CompiledSelectorBlock selectorBlock,
            @NonNull GroupSortingBehavior sortingBehavior,
            @NonNull List<EffectiveGroup> nestedEffectiveGroups) {
        this.name = name;
        this.orderIndex = orderIndex;
        this.selectorBlock = selectorBlock;
        this.sortingBehavior = sortingBehavior;
        this.nestedEffectiveGroups = Collections.unmodifiableList(nestedEffectiveGroups);
    }

    public Optional<EffectiveGroup> classify(@NonNull MemberDescriptor descriptor) {
        if (!selectorBlock.match(descriptor)) {
            return Optional.empty();
        }
        for (EffectiveGroup child : nestedEffectiveGroups) {
            Optional<EffectiveGroup> hit = child.classify(descriptor);
            if (hit.isPresent()) {
                return hit;
            }
        }
        return Optional.of(this); // fallback to parent bucket
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof EffectiveGroup that)) return false;

        return orderIndex == that.orderIndex
                && name.equals(that.name)
                && selectorBlock.equals(that.selectorBlock)
                && sortingBehavior.equals(that.sortingBehavior)
                && nestedEffectiveGroups.equals(that.nestedEffectiveGroups);
    }

    @Override
    public int hashCode() {
        int result = name.hashCode();
        result = 31 * result + orderIndex;
        result = 31 * result + selectorBlock.hashCode();
        result = 31 * result + sortingBehavior.hashCode();
        result = 31 * result + nestedEffectiveGroups.hashCode();
        return result;
    }
}
