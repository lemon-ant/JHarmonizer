package io.github.antonlem.jharmonizer.core.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Value;

@Value
public class HeaderLine {

    char character;
    int leftPadding;

    HeaderLine(
            @JsonProperty(value = "character", required = true) char character,
            @JsonProperty(value = "left-padding", required = true) int leftPadding) {
        this.character = character;
        this.leftPadding = leftPadding;
    }
}
