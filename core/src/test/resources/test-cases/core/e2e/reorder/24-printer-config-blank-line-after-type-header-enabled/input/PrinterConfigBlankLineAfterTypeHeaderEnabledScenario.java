/*
 * SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.lemon_ant.jharmonizer.core.e2e;

public class PrinterConfigBlankLineAfterTypeHeaderEnabledScenario {
    int beta = 2;
    int alpha = 1;

    void beta() {}
    void alpha() {}

    enum Status {
        ACTIVE,
        INACTIVE;

        void beta() {}
        void alpha() {}
    }

    static class Nested {
        int beta = 2;
        int alpha = 1;

        void beta() {}
        void alpha() {}
    }

    public static void main(String[] args) {
        PrinterConfigBlankLineAfterTypeHeaderEnabledScenario instance =
                new PrinterConfigBlankLineAfterTypeHeaderEnabledScenario();
        if (instance.alpha != 1 || instance.beta != 2) {
            throw new IllegalStateException("Unexpected field values");
        }
        if (Status.values().length != 2) {
            throw new IllegalStateException("Unexpected enum size");
        }
        Nested nested = new Nested();
        if (nested.alpha != 1 || nested.beta != 2) {
            throw new IllegalStateException("Unexpected nested field values");
        }
    }
}
