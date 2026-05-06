// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
package io.github.lemon_ant.jharmonizer.core.sorter.spoon;

public class NaturalGroupResolutionType {

    private final int alpha;

    private int beta;

    public NaturalGroupResolutionType(int alpha) {
        this.alpha = alpha;
    }

    public int sum() {
        return alpha + beta;
    }

    static class NestedType {}
}
