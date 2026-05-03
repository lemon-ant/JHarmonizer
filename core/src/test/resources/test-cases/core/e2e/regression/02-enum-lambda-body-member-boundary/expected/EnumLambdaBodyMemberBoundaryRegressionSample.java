/*
 * SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 */
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
            true);

    private final MetricDescriptor descriptor;
    private final boolean visible;

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

    public MetricDescriptor getDescriptor() {
        return descriptor;
    }

    public boolean isVisible() {
        return visible;
    }

    EnumLambdaBodyMemberBoundaryRegressionSample(final ValueMapper mapper, final boolean visible) {
        this.descriptor = new MetricDescriptor(mapper);
        this.visible = visible;
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
