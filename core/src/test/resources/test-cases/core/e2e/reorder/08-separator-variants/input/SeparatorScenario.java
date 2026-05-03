// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
package io.github.lemon_ant.jharmonizer.core.e2e;

public class SeparatorScenario {
    void gamma() {}

    void alpha() {}

    int z = 2;
    int a = 1;

    public static void main(String[] args) {
        SeparatorScenario sample = new SeparatorScenario();
        if (sample.a != 1 || sample.z != 2) {
            throw new IllegalStateException("Unexpected separator scenario fields");
        }
    }
}
