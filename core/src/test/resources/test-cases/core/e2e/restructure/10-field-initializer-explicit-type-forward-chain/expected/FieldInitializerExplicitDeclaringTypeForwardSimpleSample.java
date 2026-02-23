package e2e;

public class FieldInitializerExplicitDeclaringTypeForwardSimpleSample {
    private static final int zeta = FieldInitializerExplicitDeclaringTypeForwardSimpleSample.alpha + 1;
    private static int alpha = 10;
    private static int beta = 2;
    private static final int gamma = 3;

    public static void main(String[] args) {
        if (alpha != 10 || beta != 2 || gamma != 3 || zeta != 1) {
            throw new IllegalStateException("Unexpected initialization values:"
                    + " alpha="
                    + alpha
                    + ", beta="
                    + beta
                    + ", gamma="
                    + gamma
                    + ", zeta="
                    + zeta);
        }
    }
}
