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
 * @param <TItem> the type of items in this group
 */
@Value
public class Group<TItem> {

    List<TItem> items;

    /** Convenience factory: creates a group from the given items. */
    @NonNull
    @SafeVarargs
    public static <TItem> Group<TItem> of(@NonNull TItem... items) {
        return new Group<>(List.of(items));
    }
}
