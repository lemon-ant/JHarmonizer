package io.github.lemon_ant.jharmonizer.sorting;

import lombok.Value;

import java.util.List;

/**
 * Describes which items form groups (groups that must stay together as an indivisible block).
 * <p>
 * Each {@link Group} contains the actual items that form that group.
 * Items that do not appear in any group are treated as singleton groups internally.
 *
 * @param <TSortableItem> the type of items
 */
@Value
public class Groups<TSortableItem> {

    List<Group<TSortableItem>> groups;

    private static final Groups<?> EMPTY_INSTANCE = new Groups<>(List.of());

    /** Returns an empty grouping (no groups — every item is its own singleton block). */
    @SuppressWarnings("unchecked")
    public static <TSortableItem> Groups<TSortableItem> empty() {
        return (Groups<TSortableItem>) EMPTY_INSTANCE;
    }

    /** Convenience factory: each vararg is one {@link Group}. */
    @SafeVarargs
    public static <TSortableItem> Groups<TSortableItem> of(Group<TSortableItem>... groups) {
        return new Groups<>(List.of(groups));
    }
}
