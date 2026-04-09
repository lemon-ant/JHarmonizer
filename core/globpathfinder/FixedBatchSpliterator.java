package io.github.lemon_ant.globpathfinder;

import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Consumer;
import lombok.NonNull;

/**
 * A {@link Spliterator} wrapper that enables effective parallel splitting for sources whose
 * native spliterator cannot split (e.g. {@link java.nio.file.Files#find} or {@code flatMap}
 * on a single-element stream).
 *
 * <p>On each {@link #trySplit()} call the wrapper eagerly pulls up to {@code batchSize} elements
 * from the underlying source and returns them as an array-backed spliterator that the
 * {@link java.util.concurrent.ForkJoinPool} can process on a separate thread. The remaining
 * elements stay in the source and are consumed either via subsequent splits or through
 * {@link #tryAdvance}/{@link #forEachRemaining}.</p>
 *
 * <p>This approach avoids collecting the entire source into an intermediate collection: memory
 * usage is {@code O(batchSize)} per split, not {@code O(totalElements)}.</p>
 *
 * @param <T> the element type
 */
class FixedBatchSpliterator<T> implements Spliterator<T> {

    private final Spliterator<T> source;
    private final int batchSize;

    /**
     * Wraps the given source spliterator with fixed-batch splitting capability.
     *
     * @param source    the underlying spliterator to pull elements from
     * @param batchSize maximum number of elements per split batch
     */
    FixedBatchSpliterator(@NonNull Spliterator<T> source, int batchSize) {
        this.source = source;
        this.batchSize = batchSize;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Spliterator<T> trySplit() {
        Object[] batch = new Object[batchSize];
        int count = 0;
        HoldingConsumer<T> holder = new HoldingConsumer<>();
        while (count < batchSize && source.tryAdvance(holder)) {
            batch[count++] = holder.value;
        }
        if (count == 0) {
            return null;
        }
        return (Spliterator<T>) Spliterators.spliterator(batch, 0, count, characteristics());
    }

    @Override
    public boolean tryAdvance(Consumer<? super T> action) {
        return source.tryAdvance(action);
    }

    @Override
    public void forEachRemaining(Consumer<? super T> action) {
        source.forEachRemaining(action);
    }

    @Override
    public long estimateSize() {
        return source.estimateSize();
    }

    @Override
    public int characteristics() {
        return source.characteristics() & ~SIZED & ~SUBSIZED;
    }

    private static final class HoldingConsumer<T> implements Consumer<T> {

        T value;

        @Override
        public void accept(T value) {
            this.value = value;
        }
    }
}
