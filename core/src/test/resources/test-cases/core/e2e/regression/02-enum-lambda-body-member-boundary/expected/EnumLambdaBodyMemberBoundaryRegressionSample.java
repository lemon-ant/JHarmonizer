package io.github.lemon_ant.jharmonizer.core.e2e;

public enum EnumLambdaBodyMemberBoundaryRegressionSample {
    FIRST(value -> value, false),

    BROKEN(
            value -> {
                final long normalized = Math.max(1, value);
                if (normalized > 10) {
                    // Keep non-zero values visible in reduced form.
                    return 10;
                }
                return normalized;
            },
            true
    );

    public MetricDescriptor getDescriptor() {
        return descriptor;
    }

    public boolean isVisible() {
        return visible;
    }

    private final MetricDescriptor descriptor;
    private final boolean visible;

    EnumLambdaBodyMemberBoundaryRegressionSample(final ValueMapper mapper, final boolean visible) {
        this.descriptor = new MetricDescriptor(mapper);
        this.visible = visible;
    }

    public static void main(String[] args) {
        if (FIRST.getDescriptor() == null
                || FIRST.isVisible()
                || BROKEN.getDescriptor() == null
                || !BROKEN.isVisible()) {
            throw new IllegalStateException("Unexpected enum field values:"
                    + " FIRST.descriptor=" + FIRST.getDescriptor()
                    + ", FIRST.visible=" + FIRST.isVisible()
                    + ", BROKEN.descriptor=" + BROKEN.getDescriptor()
                    + ", BROKEN.visible=" + BROKEN.isVisible());
        }
    }

    private interface ValueMapper {
        long apply(long value);
    }

    private static final class MetricDescriptor {
        private final ValueMapper mapper;

        private MetricDescriptor(final ValueMapper mapper) {
            this.mapper = mapper;
        }
    }
}
