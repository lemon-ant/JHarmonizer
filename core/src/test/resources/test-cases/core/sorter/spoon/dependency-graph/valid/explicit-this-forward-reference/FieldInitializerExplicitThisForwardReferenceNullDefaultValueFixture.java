/*
 * SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.lemon_ant.jharmonizer.core.sorter.spoon.dependency_graph;

public class FieldInitializerExplicitThisForwardReferenceNullDefaultValueFixture {

    private final int alpha = this.bravo == null ? 1 : 0;

    private final Object bravo = null;
}
