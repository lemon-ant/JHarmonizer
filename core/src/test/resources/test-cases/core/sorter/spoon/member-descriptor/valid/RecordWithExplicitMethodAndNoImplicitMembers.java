/*
 * SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.lemon_ant.jharmonizer.core.sorter.spoon;

public record RecordWithExplicitMethodAndNoImplicitMembers(int alpha, int beta) {
  public int sum() { return alpha + beta; }
}
