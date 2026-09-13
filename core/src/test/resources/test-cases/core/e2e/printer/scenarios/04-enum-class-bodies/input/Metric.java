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

    abstract long value();

    long total() {
        return value() + cached;
    }
}
