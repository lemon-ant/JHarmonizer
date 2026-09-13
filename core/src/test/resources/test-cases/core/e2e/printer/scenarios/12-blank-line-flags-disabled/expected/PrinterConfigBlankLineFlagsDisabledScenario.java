// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
package io.github.lemon_ant.jharmonizer.core.e2e;

public class PrinterConfigBlankLineFlagsDisabledScenario {
    int alpha = 1;

    @Deprecated
    int beta = 2;

    // comment before field
    int gamma = 3;

    void alpha() {}

    @SuppressWarnings("unused")
    void beta() {}

    // utility comment
    void gamma() {}

    public static void main(String[] args) {
        PrinterConfigBlankLineFlagsDisabledScenario instance = new PrinterConfigBlankLineFlagsDisabledScenario();
        if (instance.alpha != 1 || instance.beta != 2 || instance.gamma != 3) {
            throw new IllegalStateException("Unexpected field values");
        }
        if (Status.values().length != 2) {
            throw new IllegalStateException("Unexpected enum size");
        }
    }

    enum Status {
        ACTIVE,
        INACTIVE;

        void alpha() {}

        void beta() {}
    }
}
