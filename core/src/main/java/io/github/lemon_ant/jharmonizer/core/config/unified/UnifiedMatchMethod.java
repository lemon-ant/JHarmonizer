package io.github.lemon_ant.jharmonizer.core.config.unified;

/**
 * String-matching strategy used when evaluating member-selector patterns.
 * {@code EXACT} requires an exact string match; {@code REGEX} treats the pattern as a regular expression.
 */
public enum UnifiedMatchMethod {
    EXACT,
    REGEX
}
