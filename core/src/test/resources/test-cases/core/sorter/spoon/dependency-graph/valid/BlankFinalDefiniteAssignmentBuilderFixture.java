// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
package io.github.lemon_ant.jharmonizer.core.sorter.spoon.dependency_graph;

public class BlankFinalDefiniteAssignmentBuilderFixture {

    private final int BLANK_FINAL;

    {
        BLANK_FINAL = 1;
    }

    private final int READ_AFTER_ASSIGNMENT = BLANK_FINAL + 1;
}
