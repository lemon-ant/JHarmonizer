package io.github.lemon_ant.jharmonizer.core.e2e;

import java.util.function.Supplier;

public class LazyMethodReferenceContextSample {

    // Alphabetically first field. Method-reference target expression contains a field access
    // (LazyMethodReferenceContextSample.zProvider::toString).
    // This field access must be treated as lazy and should not force provider-before-dependent ordering.
    static Supplier<String> aDependent = LazyMethodReferenceContextSample.zProvider::toString;
    static int bIndependent = 3;

    // Intentionally placed before aDependent in input to provoke a potential false declaration dependency.
    static Integer zProvider = 7;

    public static void main(String[] args) {
        if (!"7".equals(aDependent.get()) || bIndependent != 3 || zProvider != 7) {
            throw new IllegalStateException(
                    "Unexpected field values: aDependent=" + aDependent.get() + ", bIndependent=" + bIndependent
                            + ", zProvider=" + zProvider);
        }
    }
}
