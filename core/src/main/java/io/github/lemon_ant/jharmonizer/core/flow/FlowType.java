package io.github.lemon_ant.jharmonizer.core.flow;

/**
 * Identifies the available processing flow strategies.
 * Each constant corresponds to a distinct flow implementation:
 * {@code CHECK_ALL} reports all files that need restructuring,
 * {@code CHECK_FAIL_FAST} stops at the first violation,
 * and {@code RESTRUCTURE} rewrites files in-place.
 */
public enum FlowType {
    CHECK_ALL,
    CHECK_FAIL_FAST,
    RESTRUCTURE,
}
