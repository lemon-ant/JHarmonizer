package io.github.lemon_ant.jharmonizer.core.e2e;

import java.util.function.LongUnaryOperator;

public enum EnumAnonymousArgumentBodyBoundaryRegressionSample {
    FIRST(
            value -> value + 1,
            new Reducer() {
                @Override
                public long reduce(long value) {
                    if (value > 10) {
                        return value - 10;
                    }

                    if (value > 0) {
                        return value + 10;
                    }

                    return value;
                }
            },
            true
    ),

    SECOND(
            value -> value * 2,
            new Reducer() {
                @Override
                public long reduce(long value) {
                    return value;
                }
            },
            false
    );

    public static void main(String[] args) {
        if (FIRST.compute(1) != 12) {
            throw new IllegalStateException("Unexpected FIRST value");
        }
        if (SECOND.compute(3) != 6) {
            throw new IllegalStateException("Unexpected SECOND value");
        }
    }

    private final LongUnaryOperator mapper;

    private final Reducer reducer;

    private final boolean visible;

    EnumAnonymousArgumentBodyBoundaryRegressionSample(LongUnaryOperator mapper, Reducer reducer, boolean visible) {
        this.mapper = mapper;
        this.reducer = reducer;
        this.visible = visible;
    }

    private long compute(long value) {
        return reducer.reduce(mapper.applyAsLong(value));
    }

    private interface Reducer {
        long reduce(long value);
    }
}
