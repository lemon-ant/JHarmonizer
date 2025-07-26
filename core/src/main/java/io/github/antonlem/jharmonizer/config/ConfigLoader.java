package io.github.antonlem.jharmonizer.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import java.io.File;
import java.io.IOException;
import lombok.experimental.UtilityClass;

@UtilityClass
public class ConfigLoader {
    private static final ObjectMapper YAML_MAPPER =
            new ObjectMapper(new YAMLFactory()).configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    public static ConfigRoot loadFrom(File yamlFile) throws IOException {
        return YAML_MAPPER.readValue(yamlFile, ConfigRoot.class);
    }
}
