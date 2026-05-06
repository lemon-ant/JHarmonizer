// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
package io.github.lemon_ant.jharmonizer.core.e2e;

public class FieldInitializerInstanceRefChainSample {
    int a = 0; // explicit 0; same value as default, included for clarity
    int h = this.e + 1; // e is default 0 here, so h initializes to 1 (forward ref)
    int e = this.b + 3; // b is default 0 here, so e initializes to 3 (forward ref)
    int b = this.h + 9; // h is already 1, so b becomes 10
    int c = e + 5; // e is already 3, so c becomes 8
    int d = this.g + 11; // g is default 0 here, so d initializes to 11 (forward ref)
    int f = this.g + 9; // g is default 0 here, so f initializes to 9 (forward ref)
    int g = 15; // literal 15; later assignment does not recalc d/f
    int i = this.a + 17; // a is default 0 here, so i initializes to 17 (forward ref)

    public static void main(String[] args) {
        FieldInitializerInstanceRefChainSample sample = new FieldInitializerInstanceRefChainSample();
        if (sample.a != 0
                || sample.b != 10
                || sample.c != 8
                || sample.d != 11
                || sample.e != 3
                || sample.f != 9
                || sample.g != 15
                || sample.h != 1
                || sample.i != 17) {
            throw new IllegalStateException("Unexpected field values after initialization:"
                    + " a="
                    + sample.a
                    + ", b="
                    + sample.b
                    + ", c="
                    + sample.c
                    + ", d="
                    + sample.d
                    + ", e="
                    + sample.e
                    + ", f="
                    + sample.f
                    + ", g="
                    + sample.g
                    + ", h="
                    + sample.h
                    + ", i="
                    + sample.i);
        }
    }
}
