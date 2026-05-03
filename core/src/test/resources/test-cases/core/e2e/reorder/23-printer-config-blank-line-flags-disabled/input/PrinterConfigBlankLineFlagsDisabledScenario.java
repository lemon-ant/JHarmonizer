/*
 * SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.lemon_ant.jharmonizer.core.e2e;

public class PrinterConfigBlankLineFlagsDisabledScenario {

    @Deprecated
    int beta = 2;

    // comment before field
    int gamma = 3;
    int alpha = 1;

    @SuppressWarnings("unused")
    void beta() {}
    // utility comment
    void gamma() {}
    void alpha() {}

    enum Status {
        ACTIVE,
        INACTIVE;

        void beta() {}
        void alpha() {}
    }

    public static void main(String[] args) {
        PrinterConfigBlankLineFlagsDisabledScenario instance = new PrinterConfigBlankLineFlagsDisabledScenario();
        if (instance.alpha != 1 || instance.beta != 2 || instance.gamma != 3) {
            throw new IllegalStateException("Unexpected field values");
        }
        if (Status.values().length != 2) {
            throw new IllegalStateException("Unexpected enum size");
        }
    }
}
