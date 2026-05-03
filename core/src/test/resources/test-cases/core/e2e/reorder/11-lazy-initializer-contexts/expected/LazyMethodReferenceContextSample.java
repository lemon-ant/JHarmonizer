// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
package io.github.lemon_ant.jharmonizer.core.e2e;

import java.util.function.Supplier;

public class LazyMethodReferenceContextSample {

    // Alphabetically first field. Method-reference target expression contains a field access
    // (LazyMethodReferenceContextSample.zProvider::toString).
    // This field access must be treated as lazy and should not force provider-before-dependent ordering.
    static Supplier<String> aDependent = LazyMethodReferenceContextSample.zProvider::toString;
    static int bIndependent = 3;

    // Intentionally placed before aDependent in input to provoke a potential false declaration dependency.
    static final String zProvider = "7";

    public static void main(String[] args) {
        if (!"7".equals(aDependent.get()) || bIndependent != 3 || !"7".equals(zProvider)) {
            throw new IllegalStateException("Unexpected field values: aDependent=" + aDependent.get()
                    + ", bIndependent=" + bIndependent + ", zProvider=" + zProvider);
        }
    }
}
