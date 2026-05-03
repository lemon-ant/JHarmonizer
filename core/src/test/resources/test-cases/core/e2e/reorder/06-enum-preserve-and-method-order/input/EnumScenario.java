// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
package io.github.lemon_ant.jharmonizer.core.e2e;

public enum EnumScenario {
    BETA,
    ALPHA;

    void beta() {}

    void alpha() {}

    public static void main(String[] args) {
        if (values().length != 2 || values()[0] != BETA || values()[1] != ALPHA) {
            throw new IllegalStateException("Unexpected enum constants order");
        }
    }
}
