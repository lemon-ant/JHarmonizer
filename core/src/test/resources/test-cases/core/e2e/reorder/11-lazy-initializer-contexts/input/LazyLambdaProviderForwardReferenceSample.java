// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
package io.github.lemon_ant.jharmonizer.core.e2e;

import java.util.function.IntSupplier;

public class LazyLambdaProviderForwardReferenceSample {
    // Intentionally placed before aDependent in input to emulate a potential false dependency.
    static int bProvider = 1;

    // This lambda is lazy and references provider via explicit type-qualified access.
    // Reordering should still follow alpha rules instead of forcing bProvider before aDependent.
    static IntSupplier aDependent = () -> LazyLambdaProviderForwardReferenceSample.bProvider;

    public static void main(String[] args) {
        if (aDependent.getAsInt() != 1 || bProvider != 1) {
            throw new IllegalStateException(
                    "Unexpected field values: aDependent=" + aDependent.getAsInt() + ", bProvider=" + bProvider);
        }
    }
}
