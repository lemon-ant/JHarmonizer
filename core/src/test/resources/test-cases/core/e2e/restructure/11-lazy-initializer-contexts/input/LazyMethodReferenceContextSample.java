package io.github.lemon_ant.jharmonizer.core.e2e;

import java.util.function.IntSupplier;

public class LazyMethodReferenceContextSample {
    static int zProvider = 7;
    static IntSupplier aDependent = LazyMethodReferenceContextSample::resolveProvider;
    static int bIndependent = 3;

    static int resolveProvider() {
        return LazyMethodReferenceContextSample.zProvider;
    }

    public static void main(String[] args) {
        if (aDependent.getAsInt() != 7 || bIndependent != 3 || zProvider != 7) {
            throw new IllegalStateException("Unexpected values: "
                    + "aDependent=" + aDependent.getAsInt()
                    + ", bIndependent=" + bIndependent
                    + ", zProvider=" + zProvider);
        }
    }
}
