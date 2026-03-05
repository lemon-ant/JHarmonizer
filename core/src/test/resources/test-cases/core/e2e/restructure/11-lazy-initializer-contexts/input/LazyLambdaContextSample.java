package io.github.lemon_ant.jharmonizer.core.e2e;

import java.util.function.IntSupplier;

public class LazyLambdaContextSample {
    // Intentionally placed before aDependent fields in input to provoke a potential false declaration dependency.
    static int zProvider = 7;
    static int bIndependent = 3;

    // Alphabetically first fields. Expected to stay first after restructuring,
    // because lazy contexts (lambda + method reference) should not force provider-before-dependent ordering.
    static Runnable aDependentLambda = () -> {
        if (LazyLambdaContextSample.zProvider != 7) {
            throw new IllegalStateException("Unexpected zProvider value in lambda: "
                    + LazyLambdaContextSample.zProvider);
        }
    };
    static IntSupplier aDependentMethodRef = LazyLambdaContextSample::provideZ;

    static int provideZ() {
        return zProvider;
    }

    public static void main(String[] args) {
        aDependentLambda.run();
        if (aDependentMethodRef.getAsInt() != 7 || bIndependent != 3 || zProvider != 7) {
            throw new IllegalStateException(
                    "Unexpected field values: aDependentMethodRef=" + aDependentMethodRef.getAsInt()
                            + ", bIndependent=" + bIndependent + ", zProvider=" + zProvider);
        }
    }
}
