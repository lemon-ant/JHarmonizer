// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
package io.github.lemon_ant.jharmonizer.core.e2e;

public class OuterThisAnonymousInitializerForwardReferenceSample {
    private final int zDependent = new Object() {
        private final int captured =
                OuterThisAnonymousInitializerForwardReferenceSample.this.aProvider + 1;
    }.captured;
    private int aProvider = 10;

    public static void main(String[] args) {
        OuterThisAnonymousInitializerForwardReferenceSample sample =
                new OuterThisAnonymousInitializerForwardReferenceSample();
        if (sample.zDependent != 1 || sample.aProvider != 10) {
            throw new IllegalStateException(
                    "Unexpected values: zDependent=" + sample.zDependent + ", aProvider=" + sample.aProvider);
        }
    }
}
