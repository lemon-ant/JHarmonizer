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
 * @param <TSortableItem> the type of items in this group
 */
@Value
public class Group<TSortableItem> {

    List<TSortableItem> items;

    /** Convenience factory: creates a group from the given items. */
    @NonNull
    @SafeVarargs
    public static <TSortableItem> Group<TSortableItem> of(@NonNull TSortableItem... items) {
        return new Group<>(List.of(items));
    }
}
