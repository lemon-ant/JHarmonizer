/*
 * SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.lemon_ant.jharmonizer.core.e2e;

public class WriteOnlyQualifiedAssignmentSample {
    static int zTarget;
    static int bIndependent = 1;
    static int aWriter = (WriteOnlyQualifiedAssignmentSample.zTarget = 5);

    public static void main(String[] args) {
        if (aWriter != 5 || bIndependent != 1 || zTarget != 5) {
            throw new IllegalStateException("Unexpected values: aWriter=" + aWriter
                    + ", bIndependent=" + bIndependent
                    + ", zTarget=" + zTarget);
        }
    }
}
