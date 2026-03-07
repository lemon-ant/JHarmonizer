package io.github.lemon_ant.jharmonizer.core.e2e;

public class LazyLambdaReadWriteContextSample {

    // Alphabetically first field.
    // Lambda body reads and writes class fields, but this is lazy/deferred execution context
    // and must not force eager provider-before-dependent declaration ordering.
    static Runnable aDependent = () -> {
        zProvider += bIndependent;
    };
    static int bIndependent = 3;

    // Intentionally placed before aDependent in input to provoke potential false declaration dependency.
    static int zProvider = 4;

    public static void main(String[] args) {
        aDependent.run();
        if (bIndependent != 3 || zProvider != 7) {
            throw new IllegalStateException(
                    "Unexpected field values: bIndependent=" + bIndependent + ", zProvider=" + zProvider);
        }
    }
}
