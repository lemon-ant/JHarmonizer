package io.github.lemon_ant.jharmonizer.core.config.input.jharmonizer.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.NonNull;
import lombok.Value;

/**
 * Formatting section of a JHarmonizer YAML config, controlling import fixing and the formatter style.
 */
@Value
public class JHarmonizerFormatting {

    // TODO Exclude to a dedicated class
    boolean fixImports;

    @NonNull
    // TODO Exclude to a dedicated class
    FormatterStyle formatterStyle;

    /**
     * Creates a new JHarmonizerFormatting.
     * @param fixImports the fix imports
     * @param formatterStyle the formatter style
     */
    public JHarmonizerFormatting(
            @JsonProperty(value = "fix-imports", required = true) boolean fixImports,
            @NonNull @JsonProperty(value = "formatter-style", required = true) FormatterStyle formatterStyle) {
        this.fixImports = fixImports;
        this.formatterStyle = formatterStyle;
    }
}
