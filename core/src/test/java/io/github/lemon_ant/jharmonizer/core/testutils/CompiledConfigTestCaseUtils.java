// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
package io.github.lemon_ant.jharmonizer.core.testutils;

// @jharmonizer:fully-off
// jharmonizer v1.0.1 incorrectly reorders dependent static fields in test classes;
// remove this directive once jharmonizer is upgraded to a version that respects field initialization order.
import io.github.lemon_ant.jharmonizer.core.config.compiled.CompiledConfig;
import io.github.lemon_ant.jharmonizer.core.config.compiled.CompiledMemberGroup;
import io.github.lemon_ant.jharmonizer.core.config.compiled.Unified2CompiledModelCompiler;
import io.github.lemon_ant.jharmonizer.core.config.input.jharmonizer.JHarmonizerConfigurationManager;
import io.github.lemon_ant.jharmonizer.core.config.unified.UnifiedConfig;
import java.net.URL;
import lombok.NonNull;
import lombok.experimental.UtilityClass;

@UtilityClass
public class CompiledConfigTestCaseUtils {

    public static CompiledMemberGroup compileSingleRootMemberGroupFromJHarmonizerConfigResource(
            @NonNull URL configResource) {
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
