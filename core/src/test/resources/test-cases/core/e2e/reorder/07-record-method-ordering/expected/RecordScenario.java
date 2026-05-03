/*
 * SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.lemon_ant.jharmonizer.core.e2e;

public record RecordScenario(int value) {
    String alpha() {
        return "a";
    }

    public static void main(String[] args) {
        RecordScenario sample = new RecordScenario(5);
        if (sample.value() != 5 || !"a".equals(sample.alpha()) || !"z".equals(sample.zeta())) {
            throw new IllegalStateException("Unexpected record behavior");
        }
    }

    String zeta() {
        return "z";
    }
}
