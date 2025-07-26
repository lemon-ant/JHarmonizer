package io.github.antonlem.jharmonizer.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Value;

@Value
public class ConfigRoot {

    TopLevelTypesOrdering topLevelTypesOrdering;

    ConfigRoot(@JsonProperty("top-level-types-ordering") TopLevelTypesOrdering topLevelTypesOrdering) {
        this.topLevelTypesOrdering = topLevelTypesOrdering;
    }
}
