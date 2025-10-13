package io.github.lemon_ant.jharmonizer.core.config.unified;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.lemon_ant.jharmonizer.core.config.input.jharmonizer.FormatterStyle;
import io.github.lemon_ant.jharmonizer.core.config.input.jharmonizer.HeaderLine;
import io.github.lemon_ant.jharmonizer.core.config.input.jharmonizer.JHarmonizerConfig;
import io.github.lemon_ant.jharmonizer.core.config.input.jharmonizer.JHarmonizerInputToUnifiedConverter;
import io.github.lemon_ant.jharmonizer.core.config.input.jharmonizer.MemberGroup;
import io.github.lemon_ant.jharmonizer.core.config.input.jharmonizer.SortKey;
import io.github.lemon_ant.jharmonizer.core.config.input.jharmonizer.TopLevelTypesOrdering;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * Smoke test aligned with the current API:
 * - uses static JHarmonizerInputToUnifiedConverter.convert2Unified(cfg)
 * - initializes all @NonNull fields of MemberGroup to avoid NPE
 * - asserts include-lines size via getSelectorBlock().getIncludes()
 */
class InputToUnifiedConverterTest {

    @Test
    void convert2Unified_validMinimalJHarmonizerModel_returnExpectedUnifiedModel() {
        MemberGroup root = MemberGroup.builder()
                .name("Root")
                .includes(Set.of(Set.of("method", "=toString"), Set.of("method", "@~Size|Length")))
                .sortKeys(List.of(SortKey.VISIBILITY_ASC, SortKey.ALPHA))
                .keepAccessorsTogether(false)
                .build();

        TopLevelTypesOrdering topLevel = new TopLevelTypesOrdering(
                false,
                List.of(), // type-groups (can be empty for this smoke test)
                List.of() // sort-keys (can be empty for this smoke test)
                );

        JHarmonizerConfig jHarmonizerConfig = JHarmonizerConfig.builder()
                .memberGroups(List.of(root))
                .topLevelTypesOrdering(topLevel)
                .formatterStyle(FormatterStyle.PALANTIR)
                .headerLine(new HeaderLine('-', 5))
                .build();

        UnifiedConfig unified = JHarmonizerInputToUnifiedConverter.convert2Unified(jHarmonizerConfig);

        assertThat(unified.getRootMemberGroups()).hasSize(1);
        assertThat(unified.getRootMemberGroups().getFirst().getSelectorBlock().getIncludes())
                .hasSize(2);
    }
}
