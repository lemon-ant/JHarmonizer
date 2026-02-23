package e2e;

public class FieldInitializerExplicitThisDefaultCombinatoricsSample {
    int zeta = this.alpha + 5; // non-default forward reference: must stay before alpha
    int alpha = 10;
    int beta = 0;
    double omega = this.psi + 1.0; // psi explicit minus-zero default
    char phi = '\0';
    boolean pi = false;
    double psi = -0.0d;
    boolean rho = this.pi; // pi explicit false default
    String sigma = this.tau == null ? "null-default" : "set"; // tau explicit null default
    String tau = null;
    int theta = this.beta + 1; // beta has explicit default value: dependency should be skipped
    char upsilon = (char) (this.phi + 1); // phi explicit char default

    public static void main(String[] args) {
        FieldInitializerExplicitThisDefaultCombinatoricsSample sample =
                new FieldInitializerExplicitThisDefaultCombinatoricsSample();

        if (sample.alpha != 10
                || sample.zeta != 5
                || sample.beta != 0
                || sample.theta != 1
                || !"null-default".equals(sample.sigma)
                || sample.upsilon != 1
                || sample.omega != 1.0
                || sample.rho) {
            throw new IllegalStateException("Unexpected initialization values:"
                    + " alpha="
                    + sample.alpha
                    + ", zeta="
                    + sample.zeta
                    + ", beta="
                    + sample.beta
                    + ", theta="
                    + sample.theta
                    + ", sigma="
                    + sample.sigma
                    + ", upsilon="
                    + (int) sample.upsilon
                    + ", omega="
                    + sample.omega
                    + ", rho="
                    + sample.rho);
        }
    }
}
