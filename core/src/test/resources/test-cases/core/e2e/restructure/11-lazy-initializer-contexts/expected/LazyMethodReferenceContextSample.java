package io.github.lemon_ant.jharmonizer.core.e2e;

import java.util.function.IntSupplier;

public class LazyMethodReferenceContextSample {

    // Alphabetically first field. Expected to stay first after restructuring,
    // because method reference should be treated as lazy and not force provider-before-dependent ordering.
    static IntSupplier aDependent = LazyMethodReferenceContextSample::provideZ;
    static int bIndependent = 3;

    // Intentionally placed before aDependent in input to provoke a potential false declaration dependency.
    static int zProvider = 7;

    static int provideZ() {
        return zProvider;
    }

    public static void main(String[] args) {
        if (aDependent.getAsInt() != 7 || bIndependent != 3 || zProvider != 7) {
            throw new IllegalStateException(
                    "Unexpected field values: aDependent=" + aDependent.getAsInt() + ", bIndependent=" + bIndependent
                            + ", zProvider=" + zProvider);
        }
    }
}
