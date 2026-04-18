package io.github.lemon_ant.jharmonizer.core.e2e;

/**
 * Demonstrates InitializerBlockDependencyProvider in strict forward-reference mode.
 *
 * <p>In relaxed mode (default), the forward reference from the instance initializer block to
 * {@code zeta} is ignored. The alphabetical field ordering gives {@code x} before {@code zeta},
 * and the init block keeps its source position.
 *
 * <p>In strict mode ({@code relaxedForwardReferences: false}), the init block's read of
 * {@code zeta} creates a {@code zeta → initBlock} dependency edge even though {@code zeta} is
 * declared after the block in source. The constraint ensures {@code zeta} is placed before the
 * init block in the output.
 */
public class StrictInitBlockFieldRefSample {
    {
        x = zeta + 1;
    }
    int x;
    int zeta = 5;
}
