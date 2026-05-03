/*
 * SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.lemon_ant.jharmonizer.core.e2e;

public class BaselineOrderingSample {
    int a = 1;
    int b = 2;

    static {
        int x = 1;
    }

    void alpha() {}

    public static void main(String[] args) {
        BaselineOrderingSample sample = new BaselineOrderingSample();
        if (sample.a != 1 || sample.b != 2) {
            throw new IllegalStateException("Unexpected field values: a=" + sample.a + ", b=" + sample.b);
        }
    }

    void zeta() {}

    class Nested {}
}
