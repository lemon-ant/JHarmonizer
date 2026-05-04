// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
package io.github.lemon_ant.jharmonizer.core.e2e;

class ThreeTypeCrossTypeChainBackRefRegressionSample {
    static final String SECOND_LABEL = new String("Second");
    static final String COMBINED_VALUE = ThreeTypeCrossTypeChainLink1.VALUE;
    static final String FIRST_LABEL = new String("First");

    public static void main(String[] args) {
        if (!"Second/via-2/via-1".equals(COMBINED_VALUE)) {
            throw new IllegalStateException("Expected 'Second/via-2/via-1' but was: " + COMBINED_VALUE);
        }
    }
}

class ThreeTypeCrossTypeChainLink1 {
    static final String VALUE = new String(ThreeTypeCrossTypeChainLink2.VALUE + "/via-1");
}

class ThreeTypeCrossTypeChainLink2 {
    static final String VALUE = new String(ThreeTypeCrossTypeChainBackRefRegressionSample.SECOND_LABEL + "/via-2");
}
