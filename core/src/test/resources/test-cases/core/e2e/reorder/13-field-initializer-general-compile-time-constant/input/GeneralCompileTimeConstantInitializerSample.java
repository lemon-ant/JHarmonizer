/*
 * SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.lemon_ant.jharmonizer.core.e2e;

public class GeneralCompileTimeConstantInitializerSample {
    static final int zConstant = 7;
    static final int cIndependent = 2;
    static final int bProvider = Integer.parseInt("1");
    static final int aDependent = bProvider + GeneralCompileTimeConstantInitializerSample.zConstant;

    public static void main(String[] args) {
        if (aDependent != 8 || bProvider != 1 || cIndependent != 2 || zConstant != 7) {
            throw new IllegalStateException(
                    "Unexpected field values: aDependent=" + aDependent + ", bProvider=" + bProvider
                            + ", cIndependent=" + cIndependent + ", zConstant=" + zConstant);
        }
    }
}
