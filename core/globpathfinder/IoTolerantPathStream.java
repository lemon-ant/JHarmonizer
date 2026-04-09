package io.github.lemon_ant.globpathfinder;

import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

/**
 * Utility to shield {@code Stream<Path>} (e.g., from {@link java.nio.file.Files#find})
 * against late {@link java.io.UncheckedIOException}.
 *
 * <p><strong>Why</strong></p>
 * <ul>
 *   <li>Filesystem traversals may throw {@code UncheckedIOException} while iterating.</li>
 *   <li>We want to log a WARN and gracefully stop <em>only</em> the current base traversal.</li>
 *   <li>We want to preserve splitting/parallel characteristics of the underlying stream.</li>
 * </ul>
 *
 * <p><strong>Behavior</strong></p>
 * <ul>
 *   <li>Logs any {@code UncheckedIOException} at WARN with the {@code basePath} context.</li>
 *   <li>Terminates the current spliterator branch; outer pipelines continue unaffected.</li>
 *   <li>Preserves {@link java.util.Spliterator#characteristics()} and delegates
 *       {@link java.util.Spliterator#trySplit()} to keep parallel streams effective.</li>
 *   <li>Propagates {@link Stream#onClose(Runnable)} to the source stream.</li>
 * </ul>
 *
 * <p><strong>Usage</strong></p>
 * <pre>{@code
 * Stream<Path> raw = Files.find(basePath, maxDepth, fileFilter, visitOptions);
 * Stream<Path> safe = IoShieldingStreams.wrapPathStream(raw, basePath, log);
 * try (safe) {
 *   safe.forEach(...); // your pipeline
 * }
 * }</pre>
 */
@Slf4j
@UtilityClass
@SuppressWarnings("PMD.GuardLogStatement")
class IoTolerantPathStream {

    /**
     * Wrap a {@code Stream<Path>} so that late {@link java.io.UncheckedIOException} are logged
     * and swallowed, while keeping the stream's parallel flag and close propagation.
     *
     * <p><strong>Notes</strong></p>
     * <ul>
     *   <li>The returned stream keeps the same {@code isParallel()} state as {@code sourceStream}.</li>
     *   <li>Closing the returned stream will also close the {@code sourceStream}.</li>
     * </ul>
     *
     * @param sourceStream original stream (e.g., from {@link java.nio.file.Files#find})
     * @param basePath     context for logging; typically the scanned base directory
     * @return a shielded stream that logs and suppresses late {@code UncheckedIOException}
     */
    static Stream<Path> wrap(Stream<Path> sourceStream, Path basePath) {
        boolean isParallel = sourceStream.isParallel();
        Spliterator<Path> sourceSpliterator = sourceStream.spliterator();
        Spliterator<Path> shielded = createIoTolerantSpliterator(sourceSpliterator, basePath);

        return StreamSupport.stream(shielded, isParallel).onClose(sourceStream::close);
    }

    /**
     * Create a shielding {@link java.util.Spliterator} that delegates to the source but
     * catches {@link java.io.UncheckedIOException} in {@code tryAdvance} and
     * {@code forEachRemaining}, logs a WARN, and stops iteration of this branch.
     *
     * <p><strong>Preserved semantics</strong></p>
     * <ul>
     *   <li>Delegates {@link java.util.Spliterator#trySplit()} to retain parallel splitting.</li>
     *   <li>Returns the same {@link java.util.Spliterator#characteristics()} as the source.</li>
     * </ul>
     *
     * @param source   underlying spliterator
     * @param basePath base-path context for logging
     * @return shielding spliterator
     */
    private static Spliterator<Path> createIoTolerantSpliterator(Spliterator<Path> source, Path basePath) {
        return new Spliterator<>() {
            @Override
            public int characteristics() {
                return source.characteristics();
            }

            @Override
            public long estimateSize() {
                return source.estimateSize();
            }

            @Override
            public void forEachRemaining(Consumer<? super Path> action) {
                try {
                    source.forEachRemaining(action);
                } catch (UncheckedIOException ioe) {
                    log.warn(
                            "I/O during traversal of '{}': {}. Stopping this base (forEachRemaining).",
                            basePath,
                            ioe.getMessage(),
                            ioe);
                    // swallow and stop
                }
            }

            @Override
            public boolean tryAdvance(Consumer<? super Path> action) {
                try {
                    return source.tryAdvance(action);
                } catch (UncheckedIOException ioe) {
                    log.warn(
                            "I/O during traversal of '{}': {}. Skipping the rest of this base.",
                            basePath,
                            ioe.getMessage(),
                            ioe);
                    return false; // stop this branch
                }
            }

            @Override
            public Spliterator<Path> trySplit() {
                Spliterator<Path> split = source.trySplit();
                return (split == null) ? null : createIoTolerantSpliterator(split, basePath);
            }
        };
    }
}
