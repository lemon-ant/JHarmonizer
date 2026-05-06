// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
package io.github.lemon_ant.jharmonizer.core.e2e;

public class AccessorBundleSample {
    int value;

    String alpha() {
        return "alpha";
    }

    void setValue(int value) {
        this.value = value;
    }

    int getValue() {
        return value;
    }

    public static void main(String[] args) {
        AccessorBundleSample sample = new AccessorBundleSample();
        sample.setValue(7);
        if (sample.getValue() != 7) {
            throw new IllegalStateException("Unexpected accessor value: " + sample.getValue());
        }
    }
}
