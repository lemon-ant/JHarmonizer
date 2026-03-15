package io.github.lemon_ant.jharmonizer.core.config.input.jharmonizer;

import io.github.lemon_ant.jharmonizer.core.config.input.jharmonizer.converter.JHarmonizer2UnifiedConverter;
import io.github.lemon_ant.jharmonizer.core.config.input.jharmonizer.model.JHarmonizerConfig;
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

    public static UnifiedConfig parseUnifiedConfigFromFile(@NonNull Path configFilePath) {
        JHarmonizerConfig loadedConfig = JHarmonizerConfigLoader.loadFrom(configFilePath.toFile());
        return JHarmonizer2UnifiedConverter.convert2Unified(loadedConfig);
    }
}
