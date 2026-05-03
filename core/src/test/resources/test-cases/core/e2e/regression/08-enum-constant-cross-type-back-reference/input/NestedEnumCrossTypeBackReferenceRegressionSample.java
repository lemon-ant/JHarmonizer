// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
package io.github.lemon_ant.jharmonizer.core.e2e;

class NestedEnumCrossTypeBackReferenceRegressionSample {
    static final String RECURSE_LABEL = new String("Recurse Subdirectories");
    static final String FILTER_LABEL = new String("File Filter");
    static final String MODE_VALUE = NestedBackRefMode.FILTER_ALL.getValue();

    public static void main(String[] args) {
        if (!"File Filter/Recurse Subdirectories".equals(MODE_VALUE)) {
            throw new IllegalStateException(
                    "Expected 'File Filter/Recurse Subdirectories' but was: " + MODE_VALUE);
        }
    }

    enum NestedBackRefMode {
        FILTER_ALL(
                NestedEnumCrossTypeBackReferenceRegressionSample.FILTER_LABEL
                        + "/"
                        + NestedEnumCrossTypeBackReferenceRegressionSample.RECURSE_LABEL);

        private final String description;

        NestedBackRefMode(String description) {
            this.description = description;
        }

        String getValue() {
            return description;
        }
    }
}
