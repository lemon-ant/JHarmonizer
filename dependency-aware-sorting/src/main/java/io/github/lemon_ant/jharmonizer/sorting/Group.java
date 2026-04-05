package io.github.lemon_ant.jharmonizer.sorting;

import lombok.Value;

import java.util.List;

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
    @SafeVarargs
    public static <TSortableItem> Group<TSortableItem> of(TSortableItem... items) {
        return new Group<>(List.of(items));
    }
}
