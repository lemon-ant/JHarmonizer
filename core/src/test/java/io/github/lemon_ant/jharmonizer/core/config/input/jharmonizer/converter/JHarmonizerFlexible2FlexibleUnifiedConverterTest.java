package io.github.lemon_ant.jharmonizer.core.config.input.jharmonizer.converter;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.lemon_ant.jharmonizer.core.config.input.jharmonizer.model.JHarmonizerFlexibleConfig;
import io.github.lemon_ant.jharmonizer.core.config.unified.FlexibleUnifiedConfig;
import java.util.List;
import org.junit.jupiter.api.Test;

class JHarmonizerFlexible2FlexibleUnifiedConverterTest {

    @Test
    void convert2FlexibleUnified_memberGroupsMissing_keepsRootMemberGroupsEmpty() {
        // Given
        JHarmonizerFlexibleConfig vendorConfig = new JHarmonizerFlexibleConfig(null, null, null, null, null, null);

        // When
        FlexibleUnifiedConfig unifiedConfig =
                JHarmonizerFlexible2FlexibleUnifiedConverter.convert2FlexibleUnified(vendorConfig);

        // Then
        assertThat(unifiedConfig.getRootMemberGroups()).contains(List.of());
    }
}
