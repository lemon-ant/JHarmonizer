package io.github.antonlem.jharmonizer.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Value;

@Value
public class ConfigRoot {

    JavaFileEntry javaFile;

    ConfigRoot(@JsonProperty("java-file") JavaFileEntry javaFile) {

        this.javaFile = javaFile;
    }
}
