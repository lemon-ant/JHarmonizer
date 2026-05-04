// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
package io.github.lemon_ant.jharmonizer.core.e2e;

public class FieldInitializerExplicitDeclaringTypeFinalConstantVsNonConstantSample {
    private static final int zFromNonConst =
            FieldInitializerExplicitDeclaringTypeFinalConstantVsNonConstantSample.aNonConstValue + 1;
    private static final int aNonConstValue = Integer.parseInt("41");
    private static final int anchor = 100;
    private static final int bConstValue = 41;
    private static final int mFromConst =
            FieldInitializerExplicitDeclaringTypeFinalConstantVsNonConstantSample.bConstValue + 1;

    public static void main(String[] args) {
        if (anchor != 100 || bConstValue != 41 || aNonConstValue != 41 || mFromConst != 42 || zFromNonConst != 1) {
            throw new IllegalStateException("Unexpected initialization values:"
                    + " anchor="
                    + anchor
                    + ", bConstValue="
                    + bConstValue
                    + ", aNonConstValue="
                    + aNonConstValue
                    + ", mFromConst="
                    + mFromConst
                    + ", zFromNonConst="
                    + zFromNonConst);
        }
    }
}
