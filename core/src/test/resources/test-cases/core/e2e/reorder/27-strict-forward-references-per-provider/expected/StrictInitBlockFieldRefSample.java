package io.github.lemon_ant.jharmonizer.core.e2e;

/**
 * Demonstrates {@code InitializerBlockDependencyProvider} in strict forward-reference mode.
 *
 * <p>In relaxed mode (default), the instance initializer block's read of {@code this.zeta}
 * (where {@code zeta} is declared after the block) does NOT create a dependency edge. The fields
 * are sorted alphabetically ({@code x} before {@code zeta}) and the init block keeps its source
 * position, so the output is: init-block, {@code x}, {@code zeta}.
 *
 * <p>In strict mode ({@code relaxedForwardReferences: false}), the {@code this.zeta} read in the
 * init block creates a {@code zeta → init-block} dependency edge even though {@code zeta} is
 * declared after the block in source. The constraint forces {@code zeta} to appear before the init
 * block in the output: {@code x}, {@code zeta}, init-block.
 */
public class StrictInitBlockFieldRefSample {
    int x;
    int zeta = 5;

    {
        x = this.zeta + 1;
    }
}
