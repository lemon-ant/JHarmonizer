package io.github.antonlem.jharmonizer.config;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Value;

@Value
public class ConfigRoot {

    @JsonProperty("java-file")
    JavaFileEntry javaFile;
}
