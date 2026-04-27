package io.github.lemon_ant.jharmonizer.sorting;

import java.util.List;
import java.util.stream.IntStream;
import lombok.NonNull;
import lombok.Value;

/**
 * Describes ordering constraints of the form {@code provider → dependent}:
 * the provider item must appear before the dependent item in the final order.
 * <p>
 * Constraints form a DAG; cycles are detected at sort time and cause a
 * {@link SortingException}.
 *
 * @param <TItem> the type of items
 */
@Value
public class Dependencies<TItem> {

    List<Dependency<TItem>> edges;

    private static final Dependencies<?> EMPTY_INSTANCE = new Dependencies<>(List.of());

    /** Returns an empty dependency set (no ordering constraints). */
    @NonNull
    @SuppressWarnings("unchecked")
    public static <TItem> Dependencies<TItem> empty() {
        return (Dependencies<TItem>) EMPTY_INSTANCE;
    }

    /**
     * A single {@code provider → dependent} ordering constraint.
     *
     * @param <TItem> the type of items
     * @param provider  the item that must appear first
     * @param dependent the item that must appear after its provider
     */
    @Value
    public static class Dependency<TItem> {
        @NonNull
        TItem provider;

        @NonNull
        TItem dependent;
    }

    /**
     * Convenience factory. Arguments are alternating {@code provider, dependent} pairs.
     *
     * <pre>{@code
     * Dependencies.of(a, b)   // => a -> b
     * }</pre>
     */
    @NonNull
    @SafeVarargs
    public static <TItem> Dependencies<TItem> of(@NonNull TItem... pairs) {
        if (pairs.length % 2 != 0) {
            throw new IllegalArgumentException("pairs length must be even");
        }
        List<Dependency<TItem>> list = IntStream.iterate(0, i -> i + 2)
                .limit(pairs.length / 2)
                .mapToObj(i -> new Dependency<>(pairs[i], pairs[i + 1]))
                .toList();
        return new Dependencies<>(list);
    }
}
