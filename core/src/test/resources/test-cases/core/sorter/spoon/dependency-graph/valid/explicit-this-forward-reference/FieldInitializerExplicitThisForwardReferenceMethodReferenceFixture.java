/*
 * SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.lemon_ant.jharmonizer.core.sorter.spoon.dependency_graph;

public class FieldInitializerExplicitThisForwardReferenceMethodReferenceFixture {

    private final int alpha = this.bravo;

    private final int bravo = java.util.Optional.<java.util.function.Supplier<Integer>>of(() -> 0)
            .map(java.util.function.Supplier::get)
            .orElse(0);
}
