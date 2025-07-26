package io.github.antonlem.jharmonizer.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import java.io.File;
import java.io.IOException;
import lombok.experimental.UtilityClass;

@UtilityClass
public class ConfigLoader {
    public static JavaFileEntry loadFrom(File yamlFile) throws IOException {
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        return mapper.readValue(yamlFile, JavaFileEntry.class);
    }
}
