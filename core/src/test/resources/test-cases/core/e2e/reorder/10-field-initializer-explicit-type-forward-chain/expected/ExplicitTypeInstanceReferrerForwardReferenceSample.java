/*
 * SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.lemon_ant.jharmonizer.core.e2e;

public class ExplicitTypeInstanceReferrerForwardReferenceSample {
    private static int aStatic = 10;
    private int zInstance = ExplicitTypeInstanceReferrerForwardReferenceSample.aStatic + 1;

    public static void main(String[] args) {
        ExplicitTypeInstanceReferrerForwardReferenceSample sample =
                new ExplicitTypeInstanceReferrerForwardReferenceSample();
        if (sample.zInstance != 11 || aStatic != 10) {
            throw new IllegalStateException(
                    "Unexpected values: zInstance=" + sample.zInstance + ", aStatic=" + aStatic);
        }
    }
}
