package io.github.lemon_ant.jharmonizer.core.testutils;

import io.github.lemon_ant.jharmonizer.core.config.compiled.CompiledConfig;
import io.github.lemon_ant.jharmonizer.core.config.compiled.CompiledMemberGroup;
import io.github.lemon_ant.jharmonizer.core.config.compiled.Unified2CompiledModelCompiler;
import io.github.lemon_ant.jharmonizer.core.config.input.jharmonizer.JHarmonizerConfigurationManager;
import io.github.lemon_ant.jharmonizer.core.config.unified.UnifiedConfig;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import lombok.NonNull;
import lombok.experimental.UtilityClass;

@UtilityClass
public class CompiledConfigTestCaseUtils {

    public static CompiledMemberGroup compileSingleRootMemberGroupFromJHarmonizerConfigResource(
            @NonNull URL configResource) {
        UnifiedConfig unifiedConfig = invokeParseUnifiedConfigFromClasspathResource(configResource);
        CompiledConfig compiledConfig = Unified2CompiledModelCompiler.compile(unifiedConfig);

        if (compiledConfig.getRootMemberGroups().size() != 1) {
            throw new IllegalStateException("Expected exactly one root member group in fixture config, but found: %s"
                    .formatted(compiledConfig.getRootMemberGroups().size()));
        }

        return compiledConfig.getRootMemberGroups().getFirst();
    }

    @NonNull
    private static UnifiedConfig invokeParseUnifiedConfigFromClasspathResource(URL configResource) {
        Method parseUnifiedConfigMethod = resolveParseUnifiedConfigMethod();
        try {
            return (UnifiedConfig) parseUnifiedConfigMethod.invoke(null, configResource);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(
                    "Cannot access parseUnifiedConfigFromClasspathResource via reflection", e);
        } catch (InvocationTargetException e) {
            Throwable targetException = e.getTargetException();
            throw new IllegalStateException(
                    "Failed to invoke parseUnifiedConfigFromClasspathResource via reflection", targetException);
        }
    }

    @NonNull
    private static Method resolveParseUnifiedConfigMethod() {
        try {
            Method parseUnifiedConfigMethod = JHarmonizerConfigurationManager.class.getDeclaredMethod(
                    "parseUnifiedConfigFromClasspathResource", URL.class);
            parseUnifiedConfigMethod.setAccessible(true);
            return parseUnifiedConfigMethod;
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException(
                    "Cannot resolve parseUnifiedConfigFromClasspathResource on JHarmonizerConfigurationManager", e);
        }
    }
}
