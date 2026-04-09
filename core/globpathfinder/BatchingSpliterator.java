package io.github.lemon_ant.globpathfinder;

import edu.umd.cs.findbugs.annotations.Nullable;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Consumer;
import lombok.NonNull;

/**
 * A {@link Spliterator} wrapper that redistributes elements from a poorly-splittable source
 * into fixed-size batches suitable for parallel processing by
 * {@link java.util.concurrent.ForkJoinPool}.
 *
 * <h2>Problem</h2>
 * <p>{@link java.nio.file.Files#find} (and {@link java.nio.file.Files#walk}) return a stream
 * backed by {@code FileTreeIterator} → {@code IteratorSpliterator} → {@code WrappingSpliterator}.
 * This spliterator chain reports {@code estimateSize() = Long.MAX_VALUE}, is neither {@code SIZED}
 * nor {@code SUBSIZED}, and its {@code trySplit()} returns {@code null} — it <b>cannot split</b>.</p>
 *
 * <p>Calling {@code .parallel()} on such a stream gives the <i>illusion</i> of parallelism:
 * the {@code ForkJoinPool} sees multiple threads active because work-stealing redistributes
 * downstream computation. However, the file-tree walk itself remains single-threaded — one thread
 * drives {@code FileTreeIterator} and others only pick up downstream tasks. With lightweight
 * downstream operations (path filtering, collecting) the sequential source becomes the
 * bottleneck.</p>
 *
 * <p>{@code flatMap} over multiple {@code Files.find} streams inherits the same limitation:
 * the resulting {@code FlatMapSpliterator} also returns {@code null} from {@code trySplit()}.</p>
 *
 * <h2>Solution</h2>
 * <p>This spliterator wraps the unsplittable source and, on each {@link #trySplit()} call, eagerly
 * pulls up to {@code batchSize} elements via {@link Spliterator#tryAdvance} and returns them as an
 * {@link java.util.Spliterators#spliterator(Object[], int, int, int) array-backed spliterator}.
 * The {@code ForkJoinPool} can then recursively halve these batches down to individual elements,
 * achieving true fine-grained parallelism regardless of the source's splitting capability.</p>
 *
 * <p>Memory usage is {@code O(batchSize)} per split, not {@code O(totalElements)} — the source
 * is never collected into an intermediate collection.</p>
 *
 * <h2>Empirical verification (JDK 21)</h2>
 * <pre>{@code
 * Files.find spliterator:
 *   Class:     StreamSpliterators$WrappingSpliterator
 *   SIZED:     false
 *   SUBSIZED:  false
 *   estimateSize: Long.MAX_VALUE
 *   trySplit(): null              ← cannot split
 *
 * flatMap over Files.find streams:
 *   trySplit(): null              ← also cannot split
 *
 * Files.find().parallel() thread distribution (100 files, cheap downstream):
 *   Threads used: 2               ← only work-stealing, not true splitting
 *   main: 53 files
 *   worker-1: 53 files
 * }</pre>
 *
 * @param <T> the element type
 */
class BatchingSpliterator<T> implements Spliterator<T> {

    private final Spliterator<T> source;
    private final int batchSize;

    /**
     * Wraps the given source spliterator with fixed-batch splitting capability.
     *
     * @param source    the underlying spliterator to pull elements from
     * @param batchSize maximum number of elements per split batch; must be positive
     */
    BatchingSpliterator(@NonNull Spliterator<T> source, int batchSize) {
        if (batchSize <= 0) {
            throw new IllegalArgumentException("batchSize must be positive: " + batchSize);
        }
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

        @Nullable
        T value;

        @Override
        public void accept(T value) {
            this.value = value;
        }
    }
}
