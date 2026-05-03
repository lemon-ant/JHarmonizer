// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
package io.github.lemon_ant.jharmonizer.core.e2e;

enum NestedClassInEnumCrossTypeBackReferenceRegressionSample {
    FILTER_ALL(
            NestedHolder.FILTER_LABEL
                    + "/"
                    + NestedHolder.RECURSE_LABEL);

    private final String description;

    NestedClassInEnumCrossTypeBackReferenceRegressionSample(String description) {
        this.description = description;
    }

    String getValue() {
        return description;
    }

    public static void main(String[] args) {
        if (!"File Filter/Recurse Subdirectories".equals(NestedHolder.MODE_VALUE)) {
            throw new IllegalStateException(
                    "Expected 'File Filter/Recurse Subdirectories' but was: " + NestedHolder.MODE_VALUE);
        }
    }

    static class NestedHolder {
        static final String RECURSE_LABEL = new String("Recurse Subdirectories");
        static final String FILTER_LABEL = new String("File Filter");
        static final String MODE_VALUE =
                NestedClassInEnumCrossTypeBackReferenceRegressionSample.FILTER_ALL.getValue();
    }
}
