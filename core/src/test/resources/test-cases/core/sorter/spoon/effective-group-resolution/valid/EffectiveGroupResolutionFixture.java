// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
package io.github.lemon_ant.jharmonizer.core.sorter.spoon;

public class EffectiveGroupResolutionFixture {

    public static final int PROVIDER = Integer.parseInt("1");
    public static final int DIRECT_DEPENDENT = PROVIDER + 1;
    public static final int TRANSITIVE_PROVIDER = PROVIDER + 10;
    public static final int TRANSITIVE_DEPENDENT = TRANSITIVE_PROVIDER + 1;
    public static final int UNRELATED = 123;

    static {
        int referencedProviderValue = PROVIDER + 100;
        if (referencedProviderValue == 0) {
            throw new IllegalStateException("Not expected");
        }
    }

    private EffectiveGroupResolutionFixture() {}
}
