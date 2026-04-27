package io.github.lemon_ant.jharmonizer.sorting;

import java.util.List;
import lombok.NonNull;
import lombok.Value;

/**
 * Describes which items form groups (groups that must stay together as an indivisible block).
 * <p>
 * Each {@link Group} contains the actual items that form that group.
 * Items that do not appear in any group are treated as singleton groups internally.
 *
 * @param <TItem> the type of items
 */
@Value
@SuppressWarnings("PMD.AvoidFieldNameMatchingTypeName")
public class Groups<TItem> {

    List<Group<TItem>> groups;

    private static final Groups<?> EMPTY_INSTANCE = new Groups<>(List.of());

    /** Returns an empty grouping (no groups — every item is its own singleton block). */
    @NonNull
    @SuppressWarnings("unchecked")
    public static <TItem> Groups<TItem> empty() {
        return (Groups<TItem>) EMPTY_INSTANCE;
    }

    /** Convenience factory: each vararg is one {@link Group}. */
    @NonNull
    @SafeVarargs
    public static <TItem> Groups<TItem> of(@NonNull Group<TItem>... groups) {
        return new Groups<>(List.of(groups));
    }
}
