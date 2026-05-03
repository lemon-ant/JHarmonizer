/*
 * SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.lemon_ant.jharmonizer.core.e2e;

public class FieldInitializerExplicitDeclaringTypeForwardChainSample {
    private static final int zeta = FieldInitializerExplicitDeclaringTypeForwardChainSample.alpha + 2;
    private static final int theta = FieldInitializerExplicitDeclaringTypeForwardChainSample.beta + 1;
    private static final boolean rho = FieldInitializerExplicitDeclaringTypeForwardChainSample.pi;
    private static final String sigma =
            FieldInitializerExplicitDeclaringTypeForwardChainSample.tau == null ? "null-default" : "set";
    private static final char upsilon = (char) (FieldInitializerExplicitDeclaringTypeForwardChainSample.phi + 1);
    private static final double omega = FieldInitializerExplicitDeclaringTypeForwardChainSample.psi + 1.0;

    private static int alpha = 10;
    private static int beta = 3;
    private static boolean pi = false;
    private static String tau = null;
    private static char phi = '\0';
    private static double psi = -0.0d;

    public static void main(String[] args) {
        if (alpha != 10
                || zeta != 2
                || beta != 3
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
