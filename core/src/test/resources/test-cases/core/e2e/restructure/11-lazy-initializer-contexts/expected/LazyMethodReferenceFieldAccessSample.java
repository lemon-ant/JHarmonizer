package io.github.lemon_ant.jharmonizer.core.e2e;

import java.util.function.IntSupplier;

public class LazyMethodReferenceFieldAccessSample {

    // Alphabetically first field. Method-reference target expression contains a field access
    // (LazyMethodReferenceFieldAccessSample.zProvider::intValue).
    // This field access must be treated as lazy and should not force provider-before-dependent ordering.
    static IntSupplier aDependent = LazyMethodReferenceFieldAccessSample.zProvider::intValue;
    static int bIndependent = 3;

    // Intentionally placed before aDependent in input to provoke a potential false declaration dependency.
    static Integer zProvider = Integer.valueOf(7);
}
