// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
package io.github.lemon_ant.jharmonizer.core.sorter.spoon.dependency_graph;

public class InitializerBlockCompileTimeConstantExclusionFixture {

    private static final int Z_CONSTANT = 7;
    private static int B_PROVIDER = Integer.parseInt("1");
    private static int SINK;

    static {
        SINK = B_PROVIDER + InitializerBlockCompileTimeConstantExclusionFixture.Z_CONSTANT;
    }
}
