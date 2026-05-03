// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
package io.github.lemon_ant.jharmonizer.core.e2e;

public class InitializerBlockStaticConstantExclusionSample {
    static final int zConstant = 7;
    static int bProvider = Integer.parseInt("1");

    static {
        sink = InitializerBlockStaticConstantExclusionSample.bProvider
                + InitializerBlockStaticConstantExclusionSample.zConstant;
    }

    static int sink;

    public static void main(String[] args) {
        if (bProvider != 1 || zConstant != 7 || sink != 8) {
            throw new IllegalStateException(
                    "Unexpected values: bProvider=" + bProvider + ", zConstant=" + zConstant + ", sink=" + sink);
        }
    }
}
