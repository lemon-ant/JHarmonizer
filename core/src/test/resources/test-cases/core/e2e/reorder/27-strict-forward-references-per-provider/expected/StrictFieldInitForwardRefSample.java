/*
 * SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.lemon_ant.jharmonizer.core.e2e;

/**
 * Demonstrates {@code ExplicitThisInitializerFieldDependencyProvider} in strict forward-reference mode.
 *
 * <p>In relaxed mode (default), the backward {@code this.b} reference in {@code a}'s initializer
 * (where {@code b} is declared before {@code a}) does NOT create a dependency edge, so alphabetical
 * ordering reorders the fields to: {@code a, b}.
 *
 * <p>In strict mode ({@code relaxedForwardReferences: false}), the {@code this.b} reference in
 * {@code a = this.b + 1} creates an {@code a → b} dependency edge even though {@code b} is already
 * declared before {@code a}. That edge prevents alphabetical reordering: {@code b} must remain
 * before {@code a}, so this input is already in strict-conforming order.
 */
public class StrictFieldInitForwardRefSample {
    int b = 5;
    int a = this.b + 1;
}
