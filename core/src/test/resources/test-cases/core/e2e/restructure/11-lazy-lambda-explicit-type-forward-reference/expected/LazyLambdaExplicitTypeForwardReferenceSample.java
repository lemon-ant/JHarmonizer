package io.github.lemon_ant.jharmonizer.core.e2e;

public class LazyLambdaExplicitTypeForwardReferenceSample {
    // Alphabetical order in this fixture: aDependent, bIndependent, zProvider.
    // If tool keeps zProvider before aDependent, it means an extra dependency was added.
    static Runnable aDependent = () -> {
        if (LazyLambdaExplicitTypeForwardReferenceSample.zProvider != 7) {
            throw new IllegalStateException("Unexpected zProvider value in lambda: "
                    + LazyLambdaExplicitTypeForwardReferenceSample.zProvider);
        }
    };
    static int bIndependent = 3;
    static int zProvider = 7;

    public static void main(String[] args) {
        aDependent.run();
        if (bIndependent != 3 || zProvider != 7) {
            throw new IllegalStateException(
                    "Unexpected field values: bIndependent=" + bIndependent + ", zProvider=" + zProvider);
        }
    }
}
