package io.github.lemon_ant.jharmonizer.core.e2e;

public class LazyLambdaContextSample {
    // Intentionally placed before aDependent in input to provoke a potential false declaration dependency.
    static int zProvider = 7;
    static int bIndependent = 3;
    // Alphabetically first field. Expected to stay first after reordering,
    // because explicit type-qualified access inside lambda should not force provider-before-dependent ordering.
    static Runnable aDependent = () -> {
        if (LazyLambdaContextSample.zProvider != 7) {
            throw new IllegalStateException("Unexpected zProvider value in lambda: "
                    + LazyLambdaContextSample.zProvider);
        }
    };

    public static void main(String[] args) {
        aDependent.run();
        if (bIndependent != 3 || zProvider != 7) {
            throw new IllegalStateException(
                    "Unexpected field values: bIndependent=" + bIndependent + ", zProvider=" + zProvider);
        }
    }
}
