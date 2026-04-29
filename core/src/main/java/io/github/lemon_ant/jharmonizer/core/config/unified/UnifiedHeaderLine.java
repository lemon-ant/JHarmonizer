package io.github.lemon_ant.jharmonizer.core.config.unified;

import lombok.Value;

/**
 * Unified descriptor for the header-line separator rendered between member groups.
 * Specifies the character repeated across the separator line and the left-padding width.
 */
@Value
public class UnifiedHeaderLine {
    char character;
    int leftPadding;
}
