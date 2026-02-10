package io.github.lemon_ant.jharmonizer.core.testutils;

import static java.util.Objects.requireNonNull;

import io.github.lemon_ant.jharmonizer.core.config.compiled.CompiledConfig;
import io.github.lemon_ant.jharmonizer.core.config.compiled.CompiledMemberGroup;
import io.github.lemon_ant.jharmonizer.core.config.compiled.Unified2CompiledModelCompiler;
import io.github.lemon_ant.jharmonizer.core.config.input.jharmonizer.JHarmonizerConfigurationManager;
import io.github.lemon_ant.jharmonizer.core.config.unified.UnifiedConfig;
import java.net.URL;
import lombok.experimental.UtilityClass;

@UtilityClass
public class CompiledConfigTestCaseUtils {

    public static CompiledMemberGroup compileSingleRootMemberGroupFromJHarmonizerConfigResource(URL configResource) {
        requireNonNull(configResource, "configResource cannot be null");

        UnifiedConfig unifiedConfig =
                JHarmonizerConfigurationManager.parseUnifiedConfigFromClasspathResource(configResource);
        CompiledConfig compiledConfig = Unified2CompiledModelCompiler.compile(unifiedConfig);

        if (compiledConfig.getRootMemberGroups().size() != 1) {
            throw new IllegalStateException("Expected exactly one root member group in fixture config, but found: %s"
                    .formatted(compiledConfig.getRootMemberGroups().size()));
        }

        return compiledConfig.getRootMemberGroups().getFirst();
    }
}
