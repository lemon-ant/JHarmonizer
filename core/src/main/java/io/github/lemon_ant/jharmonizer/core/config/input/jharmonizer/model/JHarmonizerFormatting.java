package io.github.lemon_ant.jharmonizer.core.config.input.jharmonizer.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.NonNull;
import lombok.Value;

@Value
public class JHarmonizerFormatting {

    // TODO Exclude to a dedicated class
    boolean fixImports;

    @NonNull
    // TODO Exclude to a dedicated class
    FormatterStyle formatterStyle;

    public JHarmonizerFormatting(
            @JsonProperty(value = "fix-imports", required = true) boolean fixImports,
            @NonNull @JsonProperty(value = "formatter-style", required = true) FormatterStyle formatterStyle) {
        this.fixImports = fixImports;
        this.formatterStyle = formatterStyle;
    }
}
