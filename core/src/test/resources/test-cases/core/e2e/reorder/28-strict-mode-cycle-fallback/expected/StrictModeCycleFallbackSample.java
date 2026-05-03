// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
package io.github.lemon_ant.jharmonizer.core.e2e;

/**
 * Demonstrates the strict-mode cycle fallback.
 *
 * <p>In strict forward-reference mode both fields create opposing dependency edges via
 * {@code this.}-qualified references (which are legal Java even across declaration order):
 * <ul>
 *   <li>{@code z = this.zeta + 1} → strict mode adds {@code z → zeta} (z depends on zeta)</li>
 *   <li>{@code zeta = this.z + 1} → strict mode adds {@code zeta → z} (zeta depends on z)</li>
 * </ul>
 * The two edges form a cycle.  JHarmonizer detects this, logs a warning, and retries the
 * dependency analysis with relaxed forward references.  Under relaxed mode the
 * {@code this.}-backward references are still ignored, producing no cycle and allowing
 * the file to be processed without error.
 */
class StrictModeCycleFallbackSample {
    int z = this.zeta + 1;
    int zeta = this.z + 1;
}
