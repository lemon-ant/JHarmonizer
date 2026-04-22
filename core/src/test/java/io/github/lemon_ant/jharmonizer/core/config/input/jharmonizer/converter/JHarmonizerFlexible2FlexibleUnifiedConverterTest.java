package io.github.lemon_ant.jharmonizer.core.config.input.jharmonizer.converter;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.lemon_ant.jharmonizer.core.config.input.jharmonizer.model.FormatterStyle;
import io.github.lemon_ant.jharmonizer.core.config.input.jharmonizer.model.JHarmonizerFlexibleConfig;
import io.github.lemon_ant.jharmonizer.core.config.input.jharmonizer.model.JHarmonizerFlexibleFormatting;
import io.github.lemon_ant.jharmonizer.core.config.unified.FlexibleUnifiedConfig;
import io.github.lemon_ant.jharmonizer.core.config.unified.UnifiedFormatterStyle;
import org.junit.jupiter.api.Test;

class JHarmonizerFlexible2FlexibleUnifiedConverterTest {

    @Test
    void convert2FlexibleUnified_memberGroupsMissing_keepsRootMemberGroupsAbsent() {
        // Given
        JHarmonizerFlexibleConfig vendorConfig = new JHarmonizerFlexibleConfig(null, null, true, null, null, null);

        // When
        FlexibleUnifiedConfig unifiedConfig =
                JHarmonizerFlexible2FlexibleUnifiedConverter.convert2FlexibleUnified(vendorConfig);

        // Then
        assertThat(unifiedConfig.getRootMemberGroups()).isEmpty();
    }

    @Test
    void convert2FlexibleUnified_partialFormatting_convertsOnlySpecifiedFields() {
        // Given
        JHarmonizerFlexibleFormatting flexibleFormatting =
                new JHarmonizerFlexibleFormatting(true, null, null, null, null);
        JHarmonizerFlexibleConfig vendorConfig =
                new JHarmonizerFlexibleConfig(null, flexibleFormatting, null, null, null, null);

        // When
        FlexibleUnifiedConfig unifiedConfig =
                JHarmonizerFlexible2FlexibleUnifiedConverter.convert2FlexibleUnified(vendorConfig);

        // Then
        assertThat(unifiedConfig.getFormatting()).isPresent();
        assertThat(unifiedConfig.getFormatting().get().getFixImports()).contains(true);
        assertThat(unifiedConfig.getFormatting().get().getFormatterStyle()).isEmpty();
        assertThat(unifiedConfig.getFormatting().get().getBlankLineBetweenFields())
                .isEmpty();
    }

    @Test
    void convert2FlexibleUnified_fullPartialFormattingWithStyle_convertsFormatterStyle() {
        // Given
        JHarmonizerFlexibleFormatting flexibleFormatting =
                new JHarmonizerFlexibleFormatting(null, FormatterStyle.PALANTIR, null, null, null);
        JHarmonizerFlexibleConfig vendorConfig =
                new JHarmonizerFlexibleConfig(null, flexibleFormatting, null, null, null, null);

        // When
        FlexibleUnifiedConfig unifiedConfig =
                JHarmonizerFlexible2FlexibleUnifiedConverter.convert2FlexibleUnified(vendorConfig);

        // Then
        assertThat(unifiedConfig.getFormatting()).isPresent();
        assertThat(unifiedConfig.getFormatting().get().getFormatterStyle()).contains(UnifiedFormatterStyle.PALANTIR);
        assertThat(unifiedConfig.getFormatting().get().getFixImports()).isEmpty();
    }
}
