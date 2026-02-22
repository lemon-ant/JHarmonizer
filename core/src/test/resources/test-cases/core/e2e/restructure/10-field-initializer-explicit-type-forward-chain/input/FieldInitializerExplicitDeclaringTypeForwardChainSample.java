package e2e;

public class FieldInitializerExplicitDeclaringTypeForwardChainSample {
    private static final int zeta = FieldInitializerExplicitDeclaringTypeForwardChainSample.alpha + 2;
    private static final int theta = FieldInitializerExplicitDeclaringTypeForwardChainSample.beta + 1;
    private static final boolean rho = FieldInitializerExplicitDeclaringTypeForwardChainSample.pi;
    private static final String sigma =
            FieldInitializerExplicitDeclaringTypeForwardChainSample.tau == null ? "null-default" : "set";
    private static final char upsilon = (char) (FieldInitializerExplicitDeclaringTypeForwardChainSample.phi + 1);
    private static final double omega = FieldInitializerExplicitDeclaringTypeForwardChainSample.psi + 1.0;

    private static final int alpha = 10;
    private static final int beta = 0;
    private static final boolean pi = false;
    private static final String tau = null;
    private static final char phi = '\0';
    private static final double psi = -0.0d;

    public static void main(String[] args) {
        if (alpha != 10
                || zeta != 2
                || beta != 0
                || theta != 1
                || !"null-default".equals(sigma)
                || upsilon != 1
                || omega != 1.0
                || rho) {
            throw new IllegalStateException(
                    "Unexpected initialization values:"
                            + " alpha="
                            + alpha
                            + ", zeta="
                            + zeta
                            + ", beta="
                            + beta
                            + ", theta="
                            + theta
                            + ", sigma="
                            + sigma
                            + ", upsilon="
                            + (int) upsilon
                            + ", omega="
                            + omega
                            + ", rho="
                            + rho);
        }
    }
}
