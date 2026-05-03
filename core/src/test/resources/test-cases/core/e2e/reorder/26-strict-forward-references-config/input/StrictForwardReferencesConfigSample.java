// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
package io.github.lemon_ant.jharmonizer.core.e2e;

public class StrictForwardReferencesConfigSample {
    int alpha = 10;
    int zeta = alpha + 1;
    int delta = 5;
    int beta = alpha + 2;
    int mu = beta + delta;

    public static void main(String[] args) {
        StrictForwardReferencesConfigSample sample = new StrictForwardReferencesConfigSample();
        if (sample.alpha != 10 || sample.delta != 5) {
            throw new IllegalStateException(
                    "Unexpected field values: alpha=" + sample.alpha + ", delta=" + sample.delta);
        }
    }
}
