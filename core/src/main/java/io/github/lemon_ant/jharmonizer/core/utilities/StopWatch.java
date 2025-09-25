package io.github.lemon_ant.jharmonizer.core.utilities;

import java.util.function.Supplier;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.Value;
import lombok.experimental.UtilityClass;

@UtilityClass
public class StopWatch {

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

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof TimedResult<?> that)) {
                return false;
            }

            return nanos == that.nanos && result.equals(that.result);
        }

        public double getMillis() {
            return nanos / 1_000_000.0;
        }

        @Override
        public int hashCode() {
            int result1 = result.hashCode();
            result1 = 31 * result1 + Long.hashCode(nanos);
            return result1;
        }

        @Override
        public String toString() {
            return "Result: " + result + ", time: " + getMillis() + " ms";
        }
    }
}
