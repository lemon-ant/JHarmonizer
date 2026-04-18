package io.github.lemon_ant.jharmonizer.core.e2e;

/**
 * Demonstrates FieldInitializerBackwardReferenceDependencyProvider in strict forward-reference mode.
 *
 * <p>In relaxed mode (default), the forward reference from {@code z} to {@code zeta} is ignored
 * and pure alphabetical ordering gives: {@code z, zeta}.
 *
 * <p>In strict mode ({@code relaxedForwardReferences: false}), the reference {@code z = zeta + 1}
 * creates a {@code zeta → z} dependency edge even though {@code zeta} is declared after {@code z}
 * in source. The constraint overrides alphabetical ordering, placing {@code zeta} before {@code z}.
 */
public class StrictFieldInitForwardRefSample {
    int z = zeta + 1;
    int zeta = 5;
}
