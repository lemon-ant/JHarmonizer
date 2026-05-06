// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
package io.github.lemon_ant.jharmonizer.core.e2e;

public class PrinterConfigBlankLineBetweenFieldsEnabledScenario {

    int gamma = 3;
    @Deprecated
    int beta = 2;
    int alpha = 1;

    void beta() {}
    void alpha() {}

    public static void main(String[] args) {
        PrinterConfigBlankLineBetweenFieldsEnabledScenario instance =
                new PrinterConfigBlankLineBetweenFieldsEnabledScenario();
        if (instance.alpha != 1 || instance.beta != 2 || instance.gamma != 3) {
            throw new IllegalStateException("Unexpected field values");
        }
    }
}
