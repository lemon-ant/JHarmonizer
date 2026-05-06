// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
package io.github.lemon_ant.jharmonizer.core.e2e;

public class FieldAlphaOrderingMultiLevelDependenciesSample {
    private static int C3 = 30;
    private static int C2 = 20;
    private static int C1 = 10;

    private static int B3 = C3 + C2 + C1;
    private static int B2 = C3 + C2 + C1 + 2;
    private static int B1 = C3 + C2 + C1 + 1;

    private static int A3 = C3 + C2 + C1 + B3 + B2 + B1;
    private static int A2 = C3 + C2 + C1 + B3 + B2 + B1 + 2;
    private static int A1 = C3 + C2 + C1 + B3 + B2 + B1 + 1;

    public static void main(String[] args) {
        if (C3 != 30 || C2 != 20 || C1 != 10) {
            throw new IllegalStateException("Unexpected C-level values: C3=" + C3 + ", C2=" + C2 + ", C1=" + C1);
        }

        if (B3 != 60 || B2 != 62 || B1 != 61) {
            throw new IllegalStateException("Unexpected B-level values: B3=" + B3 + ", B2=" + B2 + ", B1=" + B1);
        }

        if (A3 != 243 || A2 != 245 || A1 != 244) {
            throw new IllegalStateException("Unexpected A-level values: A3=" + A3 + ", A2=" + A2 + ", A1=" + A1);
        }
    }
}
