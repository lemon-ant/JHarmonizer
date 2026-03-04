package io.github.lemon_ant.jharmonizer.core.e2e;

public class LazyLambdaExplicitTypeForwardReferenceSample {
    // Intentionally placed before aDependent in input to provoke a potential false declaration dependency.
    static int zProvider = 7;
    // Alphabetically first field. Expected to stay first after restructuring,
    // because explicit type-qualified access inside lambda should not force provider-before-dependent ordering.
    static Runnable aDependent = () -> {
        if (LazyLambdaExplicitTypeForwardReferenceSample.zProvider != 7) {
            throw new IllegalStateException("Unexpected zProvider value in lambda: "
                    + LazyLambdaExplicitTypeForwardReferenceSample.zProvider);
        }
    };
    static int bIndependent = 3;

    public static void main(String[] args) {
        aDependent.run();
        if (bIndependent != 3 || zProvider != 7) {
            throw new IllegalStateException(
                    "Unexpected field values: bIndependent=" + bIndependent + ", zProvider=" + zProvider);
        }
    }
}
