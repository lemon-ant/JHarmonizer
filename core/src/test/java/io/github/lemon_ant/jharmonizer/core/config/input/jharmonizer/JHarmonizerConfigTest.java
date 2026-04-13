package io.github.lemon_ant.jharmonizer.core.config.input.jharmonizer;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.lemon_ant.jharmonizer.core.config.input.jharmonizer.model.JHarmonizerConfig;
import org.junit.jupiter.api.Test;

class JHarmonizerConfigTest {

    private static final JHarmonizerConfig DEFAULT_CONFIG = JHarmonizerConfigLoader.loadDefault();

    @Test
    void equals_sameInstance_returnsTrue() {
        // When / Then
        assertThat(DEFAULT_CONFIG).isEqualTo(DEFAULT_CONFIG);
    }

    @Test
    void equals_nonJHarmonizerConfigObject_returnsFalse() {
        // When / Then
        assertThat(DEFAULT_CONFIG).isNotEqualTo("not a config");
    }

    @Test
    void equals_twoLoadedDefaultConfigs_returnsTrue() {
        // Given
        JHarmonizerConfig first = JHarmonizerConfigLoader.loadDefault();
        JHarmonizerConfig second = JHarmonizerConfigLoader.loadDefault();

        // When / Then
        assertThat(first).isEqualTo(second);
    }

    @Test
    void hashCode_twoLoadedDefaultConfigs_produceSameHashCode() {
        // Given
        JHarmonizerConfig first = JHarmonizerConfigLoader.loadDefault();
        JHarmonizerConfig second = JHarmonizerConfigLoader.loadDefault();

        // When / Then
        assertThat(first.hashCode()).isEqualTo(second.hashCode());
    }

    @Test
    void loadDefault_returnedConfig_hasMemberGroupsDefined() {
        // When / Then
        assertThat(DEFAULT_CONFIG.getMemberGroups()).isNotEmpty();
    }

    @Test
    void loadDefault_returnedConfig_hasFormattingDefined() {
        // When / Then
        assertThat(DEFAULT_CONFIG.getFormatting()).isNotNull();
    }

    @Test
    void loadDefault_returnedConfig_hasHeaderLineDefined() {
        // When / Then
        assertThat(DEFAULT_CONFIG.getHeaderLine()).isNotNull();
    }
}
