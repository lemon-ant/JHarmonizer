// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
package io.github.lemon_ant.jharmonizer.core.e2e;

class TwoTypeCrossTypeBackRefRegressionSample {
    static final String SECOND_LABEL = new String("Second");
    static final String FIRST_LABEL = new String("First");
    static final String COMBINED_VALUE = TwoTypeCrossTypeBackRefHelper.COMBINED_VALUE;

    public static void main(String[] args) {
        if (!"Second/suffix".equals(COMBINED_VALUE)) {
            throw new IllegalStateException("Expected 'Second/suffix' but was: " + COMBINED_VALUE);
        }
    }
}

class TwoTypeCrossTypeBackRefHelper {
    static final String COMBINED_VALUE = new String(TwoTypeCrossTypeBackRefRegressionSample.SECOND_LABEL + "/suffix");
}
