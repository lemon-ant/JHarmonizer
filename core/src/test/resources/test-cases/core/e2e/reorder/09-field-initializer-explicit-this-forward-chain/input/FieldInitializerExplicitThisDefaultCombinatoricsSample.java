/*
 * SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.lemon_ant.jharmonizer.core.e2e;

public class FieldInitializerExplicitThisDefaultCombinatoricsSample {
    int zeta = this.alpha + 5; // non-default forward reference: must stay before alpha
    int theta = this.beta + 1; // beta has explicit default value: dependency should be skipped
    String ySigma = this.aTau == null ? "null-default" : "set"; // aTau explicit null default
    char upsilon = (char) (this.phi + 1); // phi explicit char default
    double omega = this.psi + 1.0; // psi explicit minus-zero default
    boolean rho = this.pi; // pi explicit false default
    int alpha = 10;
    int beta = 0;
    String aTau = null;
    char phi = '\0';
    double psi = -0.0d;
    boolean pi = false;

    public static void main(String[] args) {
        FieldInitializerExplicitThisDefaultCombinatoricsSample sample =
                new FieldInitializerExplicitThisDefaultCombinatoricsSample();

        if (sample.alpha != 10
                || sample.zeta != 5
                || sample.beta != 0
                || sample.theta != 1
                || !"null-default".equals(sample.ySigma)
                || sample.upsilon != 1
                || sample.omega != 1.0
                || sample.rho) {
            throw new IllegalStateException(
                    "Unexpected initialization values:"
                            + " alpha="
                            + sample.alpha
                            + ", zeta="
                            + sample.zeta
                            + ", beta="
                            + sample.beta
                            + ", theta="
                            + sample.theta
                            + ", ySigma="
                            + sample.ySigma
                            + ", upsilon="
                            + (int) sample.upsilon
                            + ", omega="
                            + sample.omega
                            + ", rho="
                            + sample.rho);
        }
    }
}
