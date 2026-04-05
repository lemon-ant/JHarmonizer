package io.github.lemon_ant.jharmonizer.sorting;

import lombok.NonNull;
import lombok.Value;

import java.util.List;
import java.util.stream.IntStream;

/**
 * Describes ordering constraints of the form {@code provider → dependent}:
 * the provider item must appear before the dependent item in the final order.
 * <p>
 * Constraints form a DAG; cycles are detected at sort time and cause a
 * {@link SortingException}.
 *
 * @param <TSortableItem> the type of items
 */
@Value
public class Dependencies<TSortableItem> {

    List<Dependency<TSortableItem>> edges;

    private static final Dependencies<?> EMPTY_INSTANCE = new Dependencies<>(List.of());

    /** Returns an empty dependency set (no ordering constraints). */
    @SuppressWarnings("unchecked")
    public static <TSortableItem> Dependencies<TSortableItem> empty() {
        return (Dependencies<TSortableItem>) EMPTY_INSTANCE;
    }

    /**
     * A single {@code provider → dependent} ordering constraint.
     *
     * @param <TSortableItem> the type of items
     * @param provider  the item that must appear first
     * @param dependent the item that must appear after its provider
     */
    @Value
    public static class Dependency<TSortableItem> {
        @NonNull TSortableItem provider;
        @NonNull TSortableItem dependent;
    }

    /**
     * Convenience factory. Arguments are alternating {@code provider, dependent} pairs.
     *
     * <pre>{@code
     * Dependencies.of(a, b)   // => a -> b
     * }</pre>
     */
    @SafeVarargs
    public static <TSortableItem> Dependencies<TSortableItem> of(TSortableItem... pairs) {
        if (pairs.length % 2 != 0) {
            throw new IllegalArgumentException("pairs length must be even");
        }
        List<Dependency<TSortableItem>> list = IntStream.iterate(0, i -> i + 2)
                .limit(pairs.length / 2)
                .mapToObj(i -> new Dependency<>(pairs[i], pairs[i + 1]))
                .toList();
        return new Dependencies<>(list);
    }
}
