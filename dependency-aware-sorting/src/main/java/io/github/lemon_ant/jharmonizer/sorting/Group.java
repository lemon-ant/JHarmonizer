package io.github.lemon_ant.jharmonizer.sorting;

import java.util.List;
import lombok.NonNull;
import lombok.Value;

/**
 * An indivisible group of items that must appear together as a contiguous block in the
 * sorted output.
 *
 * <p>Within the block, items are ordered by the comparator supplied to the sorting algorithm.
 *
 * @param <TNode> the type of items in this group
 */
@Value
public class Group<TNode> {

    List<TNode> items;

    /** Convenience factory: creates a group from the given items. */
    @NonNull
    @SafeVarargs
    public static <TNode> Group<TNode> of(@NonNull TNode... items) {
        return new Group<>(List.of(items));
    }
}
