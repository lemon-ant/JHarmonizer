package io.github.lemon_ant.jharmonizer.core.e2e;

import java.util.List;

public enum ProcessorStatusDescriptorShapeRegressionSample {
    FLOWFILES_REMOVED(
            value -> value,
            null,
            false
    ),

    OUTPUT_COUNT(
            value -> value,
            null,
            false
    ),

    AVERAGE_LINEAGE_DURATION(
            value -> value,
            new ValueReducer() {
                @Override
                public long reduce(final List<StatusSnapshot> values) {
                    long millis = 0L;
                    long count = 0;

                    for (final StatusSnapshot snapshot : values) {
                        final long removed = snapshot.getStatusMetric(FLOWFILES_REMOVED.getDescriptor());
                        final long outputCount = snapshot.getStatusMetric(OUTPUT_COUNT.getDescriptor());
                        final long processed = removed + outputCount;

                        count += processed;

                        final long avgMillis = snapshot.getStatusMetric(AVERAGE_LINEAGE_DURATION.getDescriptor());
                        final long totalMillis = avgMillis * processed;
                        millis += totalMillis;
                    }

                    return count == 0 ? 0 : millis / count;
                }
            },
            true
    );

    public static void main(String[] args) {
        if (!AVERAGE_LINEAGE_DURATION.isVisible()) {
            throw new IllegalStateException("Average lineage metric should be visible");
        }
    }

    public MetricDescriptor getDescriptor() {
        return descriptor;
    }

    public boolean isVisible() {
        return visible;
    }

    private final MetricDescriptor descriptor;
    private final boolean visible;

    ProcessorStatusDescriptorShapeRegressionSample(
            final ValueProvider valueFunction,
            final ValueReducer reducer,
            final boolean visible
    ) {
        this.descriptor = new MetricDescriptor(this::ordinal, valueFunction, reducer);
        this.visible = visible;
    }

    private static final class MetricDescriptor {
        private final ValueProvider valueProvider;

        private final ValueReducer valueReducer;

        private final IntProvider indexProvider;

        private MetricDescriptor(final IntProvider indexProvider, final ValueProvider valueProvider, final ValueReducer valueReducer) {
            this.indexProvider = indexProvider;
            this.valueProvider = valueProvider;
            this.valueReducer = valueReducer;
        }
    }

    private interface ValueProvider {
        long apply(long value);
    }

    private interface ValueReducer {
        long reduce(List<StatusSnapshot> values);
    }

    private interface IntProvider {
        int value();
    }

    private static final class StatusSnapshot {
        private long getStatusMetric(final MetricDescriptor metricDescriptor) {
            return 1L;
        }
    }
}
