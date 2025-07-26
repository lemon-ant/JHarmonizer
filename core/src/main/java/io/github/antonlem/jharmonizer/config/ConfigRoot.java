package io.github.antonlem.jharmonizer.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Value;

@Value
public class ConfigRoot {

    TopLevelTypesOrdering topLevelTypesOrdering;
    boolean fixImports;
    FormatterStyle formatterStyle;

    public ConfigRoot(
            @JsonProperty("top-level-types-ordering") TopLevelTypesOrdering topLevelTypesOrdering,
            @JsonProperty(value = "fix-imports", required = true) boolean fixImports,
            @JsonProperty(value = "formatter-style", required = true) FormatterStyle formatterStyle) {
        this.topLevelTypesOrdering = topLevelTypesOrdering;
        this.fixImports = fixImports;
        this.formatterStyle = formatterStyle;
    }
}
