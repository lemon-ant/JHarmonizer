// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
package io.github.lemon_ant.jharmonizer.core.config.input.jharmonizer.converter;

import static io.github.lemon_ant.jharmonizer.core.config.input.jharmonizer.model.JHarmonizerConfigCreator.createFormatting;
import static io.github.lemon_ant.jharmonizer.core.config.input.jharmonizer.model.JHarmonizerConfigCreator.createHeaderLine;
import static io.github.lemon_ant.jharmonizer.core.config.input.jharmonizer.model.JHarmonizerConfigCreator.createTopLevelTypesOrdering;
import static io.github.lemon_ant.jharmonizer.core.config.input.jharmonizer.model.JHarmonizerConfigCreator.createTypeGroup;
import static org.assertj.core.api.Assertions.assertThat;

import io.github.lemon_ant.jharmonizer.core.config.input.jharmonizer.model.FormatterStyle;
import io.github.lemon_ant.jharmonizer.core.config.input.jharmonizer.model.JHarmonizerConfig;
import io.github.lemon_ant.jharmonizer.core.config.input.jharmonizer.model.JHarmonizerMemberGroup;
import io.github.lemon_ant.jharmonizer.core.config.input.jharmonizer.model.JHarmonizerOrderingRule;
import io.github.lemon_ant.jharmonizer.core.config.input.jharmonizer.model.JHarmonizerTopLevelTypesOrdering;
import io.github.lemon_ant.jharmonizer.core.config.input.jharmonizer.model.JHarmonizerTypeKind;
import io.github.lemon_ant.jharmonizer.core.config.unified.UnifiedConfig;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * Smoke test aligned with the current API:
 * - uses static JHarmonizer2UnifiedConverter.convert2Unified(cfg)
 * - initializes all @NonNull fields of JHarmonizerMemberGroup to avoid NPE
 * - asserts include-lines size via getSelectorBlock().getIncludes()
 */
class JHarmonizer2UnifiedConverterTest {

    @Test
    void convert2Unified_validMinimalJHarmonizerModel_returnExpectedUnifiedModel() {
        JHarmonizerMemberGroup root = JHarmonizerMemberGroup.builder()
                .name("Root")
                .includes(Set.of(Set.of("method", "=toString"), Set.of("method", "@~Size|Length")))
                .orderingRules(List.of(JHarmonizerOrderingRule.VISIBILITY_ASC, JHarmonizerOrderingRule.ALPHA))
                .keepAccessorsTogether(false)
                .build();

        JHarmonizerTopLevelTypesOrdering topLevel = createTopLevelTypesOrdering(
                false,
                List.of(createTypeGroup(
                        Set.of(JHarmonizerTypeKind.CLASS))), // type-groups (can be empty for this smoke test)
                List.of(JHarmonizerOrderingRule.ALPHA) // ordering-rules (can be empty for this smoke test)
                );

        JHarmonizerConfig jHarmonizerConfig = new JHarmonizerConfig(
                topLevel,
                createFormatting(true, FormatterStyle.PALANTIR),
                true,
                true,
                createHeaderLine('-', 5),
                List.of(root));

        UnifiedConfig unified = JHarmonizer2UnifiedConverter.convert2Unified(jHarmonizerConfig);

        assertThat(unified.getRootMemberGroups()).hasSize(1);
        assertThat(unified.getRootMemberGroups().getFirst().getSelectorBlock().getIncludes())
                .hasSize(2);
    }
}
