package io.github.lemon_ant.jharmonizer.core.config.unified;

/**
 * Unified ordering rule applied when sorting members inside a group.
 * Serves as a stable intermediate representation between the input YAML model
 * and the compiled model used during sorting.
 */
public enum UnifiedOrderingRule {
    ALPHA,
    PRESERVE,
    VISIBILITY_ASC,
    VISIBILITY_DESC,
}
