package io.github.lemon_ant.jharmonizer.core.config.input.jharmonizer;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import lombok.NonNull;
import lombok.experimental.UtilityClass;

@UtilityClass
public class JHarmonizerConfigLoader {
    private static final ObjectMapper YAML_MAPPER = new ObjectMapper(new YAMLFactory())
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true);

    @NonNull
    public static JHarmonizerConfig loadFrom(@NonNull InputStream configInput) throws IOException {
        return YAML_MAPPER.readValue(configInput, JHarmonizerConfig.class);
    }

    @NonNull
    public static JHarmonizerConfig loadFrom(@NonNull File yamlFile) throws IOException {
        try (InputStream is = Files.newInputStream(yamlFile.toPath())) {
            return loadFrom(is);
        }
    }
}
