package io.github.lemon_ant.jharmonizer.core.config.input.jharmonizer;

import com.fasterxml.jackson.databind.JsonNode;
import io.github.lemon_ant.jharmonizer.core.config.input.jharmonizer.converter.JHarmonizer2FlexibleUnifiedConverter;
import io.github.lemon_ant.jharmonizer.core.config.input.jharmonizer.converter.JHarmonizer2UnifiedConverter;
import io.github.lemon_ant.jharmonizer.core.config.input.jharmonizer.model.JHarmonizerConfig;
import io.github.lemon_ant.jharmonizer.core.config.unified.FlexibleUnifiedConfig;
import io.github.lemon_ant.jharmonizer.core.config.unified.UnifiedConfig;
import java.net.URL;
import java.nio.file.Path;
import lombok.NonNull;
import lombok.experimental.UtilityClass;

// TODO Merge with JHarmonizerConfigLoader
@UtilityClass
public class JHarmonizerConfigurationManager {
    public static UnifiedConfig parseUnifiedDefaultConfig() {
        JHarmonizerConfig defaultJHarmonizerConfig = JHarmonizerConfigLoader.loadDefault();
        return JHarmonizer2UnifiedConverter.convert2Unified(defaultJHarmonizerConfig);
    }

    public static UnifiedConfig parseUnifiedConfigFromClasspathResource(URL classpathResource) {
        JHarmonizerConfig loadedConfig = JHarmonizerConfigLoader.loadFromClasspathResource(classpathResource);
        return JHarmonizer2UnifiedConverter.convert2Unified(loadedConfig);
    }

    public static FlexibleUnifiedConfig parseFlexibleUnifiedConfigFromClasspathResource(
            @NonNull URL classpathResource) {
        JsonNode configTree = JHarmonizerConfigLoader.loadTreeFromClasspathResource(classpathResource);
        return JHarmonizer2FlexibleUnifiedConverter.convert2Flexible(configTree);
    }

    public static FlexibleUnifiedConfig parseFlexibleUnifiedConfigFromFile(@NonNull Path configFilePath) {
        JsonNode configTree = JHarmonizerConfigLoader.loadTreeFrom(configFilePath.toFile());
        return JHarmonizer2FlexibleUnifiedConverter.convert2Flexible(configTree);
    }
}
