package io.github.lemon_ant.jharmonizer.core.e2e;

/**
 * Demonstrates the strict-mode cycle fallback.
 *
 * <p>In strict forward-reference mode both fields create opposing dependency edges:
 * <ul>
 *   <li>{@code z} reads {@code zeta} (forward reference) → strict mode adds {@code zeta → z}</li>
 *   <li>{@code zeta} reads {@code z} (backward reference) → relaxed and strict both add {@code z → zeta}</li>
 * </ul>
 * The two edges form a cycle.  JHarmonizer detects this, logs a warning, and retries the
 * dependency analysis with relaxed forward references.  Under relaxed mode only the
 * {@code z → zeta} edge survives, which is consistent with alphabetical ordering, so the
 * file is processed without error and the member order is preserved.
 */
class StrictModeCycleFallbackSample {
    int z = zeta + 1;
    int zeta = z + 1;
}
