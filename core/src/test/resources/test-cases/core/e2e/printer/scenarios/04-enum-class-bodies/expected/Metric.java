// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0

enum Metric {
    FIRST {
        @Override
        long value() {
            return 1L;
        }
    },
    SECOND {
        @Override
        long value() {
            return 2L;
        }
    };

    private final long cached = 7L;

    long total() {
        return value() + cached;
    }

    abstract long value();
}
