package io.github.lemon_ant.jharmonizer.core.config.input.jharmonizer;

import static io.github.lemon_ant.jharmonizer.core.testutils.TestCaseResourceUtils.TEST_CASES_DIR;
import static org.assertj.core.api.Assertions.assertThat;

import io.github.lemon_ant.jharmonizer.core.config.unified.FlexibleUnifiedConfig;
import io.github.lemon_ant.jharmonizer.core.config.unified.UnifiedConfig;
import io.github.lemon_ant.jharmonizer.core.testutils.TestCaseResourceUtils;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class JHarmonizerConfigurationManagerTest {

    private static final URL SIMPLE_WORKING_CONFIG_URL = TestCaseResourceUtils.requireClasspathResourceUrl(
            "/" + TEST_CASES_DIR + "/core/config/input/jharmonizer/simplest-working-config.yml");

    @Test
    void parseUnifiedDefaultConfig_returnsNonNullUnifiedConfig() {
        // When
        UnifiedConfig unifiedConfig = JHarmonizerConfigurationManager.parseUnifiedDefaultConfig();

        // Then
        assertThat(unifiedConfig).isNotNull();
        assertThat(unifiedConfig.getRootMemberGroups()).isNotEmpty();
    }

    @Test
    void parseUnifiedConfigFromClasspathResource_validResource_returnsNonNullConfig() {
        // When
        UnifiedConfig unifiedConfig =
                JHarmonizerConfigurationManager.parseUnifiedConfigFromClasspathResource(SIMPLE_WORKING_CONFIG_URL);

        // Then
        assertThat(unifiedConfig).isNotNull();
    }

    @Test
    void parseFlexibleUnifiedConfigFromClasspathResource_validResource_returnsNonNullConfig() {
        // When
        FlexibleUnifiedConfig flexibleUnifiedConfig =
                JHarmonizerConfigurationManager.parseFlexibleUnifiedConfigFromClasspathResource(
                        SIMPLE_WORKING_CONFIG_URL);

        // Then
        assertThat(flexibleUnifiedConfig).isNotNull();
    }

    @Test
    void parseFlexibleUnifiedConfigFromFile_validFile_returnsNonNullConfig(@TempDir Path tempDir)
            throws java.io.IOException {
        // Given
        Path configFile = tempDir.resolve("config.yml");
        Files.copy(SIMPLE_WORKING_CONFIG_URL.openStream(), configFile);

        // When
        FlexibleUnifiedConfig flexibleUnifiedConfig =
                JHarmonizerConfigurationManager.parseFlexibleUnifiedConfigFromFile(configFile);

        // Then
        assertThat(flexibleUnifiedConfig).isNotNull();
    }
}
