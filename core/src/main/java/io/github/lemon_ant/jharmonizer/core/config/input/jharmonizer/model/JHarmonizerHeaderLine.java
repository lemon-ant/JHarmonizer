// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
package io.github.lemon_ant.jharmonizer.core.config.input.jharmonizer.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Value;

/**
 * Header-line descriptor from a JHarmonizer YAML config,
 * specifying the separator character and left-padding width used between member groups.
 */
@Value
public class JHarmonizerHeaderLine {

    char character;
    int leftPadding;

    /**
     * Creates a new JHarmonizerHeaderLine.
     * @param character the character
     * @param leftPadding the left padding
     */
    JHarmonizerHeaderLine(
            @JsonProperty(value = "character", required = true) char character,
            @JsonProperty(value = "left-padding", required = true) int leftPadding) {
        this.character = character;
        this.leftPadding = leftPadding;
    }
}
