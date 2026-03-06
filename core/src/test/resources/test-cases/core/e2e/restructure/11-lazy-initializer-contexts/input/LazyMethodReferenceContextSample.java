package io.github.lemon_ant.jharmonizer.core.e2e;

import java.util.function.Supplier;

public class LazyMethodReferenceContextSample {
    // Intentionally placed before aDependent in input to provoke a potential false declaration dependency.
    static Integer zProvider = 7;
    static int bIndependent = 3;

    // Alphabetically first field. Method-reference target expression contains a field access (zProvider::toString).
    // This field access must be treated as lazy and should not force provider-before-dependent ordering.
    static Supplier<String> aDependent = zProvider::toString;

    public static void main(String[] args) {
        if (!"7".equals(aDependent.get()) || bIndependent != 3 || zProvider != 7) {
            throw new IllegalStateException(
                    "Unexpected field values: aDependent=" + aDependent.get() + ", bIndependent=" + bIndependent
                            + ", zProvider=" + zProvider);
        }
    }
}
