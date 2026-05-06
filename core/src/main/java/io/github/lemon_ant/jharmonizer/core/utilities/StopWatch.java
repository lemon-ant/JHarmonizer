// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
package io.github.lemon_ant.jharmonizer.core.utilities;

import java.util.function.Supplier;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.Value;
import lombok.experimental.UtilityClass;

/**
 * Utility for measuring the wall-clock execution time of a supplier expression.
 * Returns a {@link TimedResult} that combines the result value with the elapsed nanoseconds.
 */
@UtilityClass
public class StopWatch {

    /**
     * Measures the wall-clock execution time of the given supplier.
     *
     * @param supplier the supplier to measure
     * @return a timed result containing the supplier value and the elapsed nanoseconds
     */
    @NonNull
    public static <TResult> TimedResult<TResult> measure(@NonNull Supplier<TResult> supplier) {
        long start = System.nanoTime();
        TResult result = supplier.get();
        long duration = System.nanoTime() - start;
        return new TimedResult<>(duration, result);
    }

    @Value
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    public static class TimedResult<T> {
        long nanos;

        @NonNull
        T result;

        /**
         * Converts the measured nanoseconds to milliseconds.
         *
         * @return the elapsed time in milliseconds
         */
        public double getMillis() {
            return nanos / 1_000_000.0;
        }
    }
}
