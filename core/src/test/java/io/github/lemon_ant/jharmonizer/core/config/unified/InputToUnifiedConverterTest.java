package io.github.lemon_ant.jharmonizer.core.config.unified;

import static io.github.lemon_ant.jharmonizer.core.config.input.jharmonizer.CreationHelper.createHeaderLine;
import static io.github.lemon_ant.jharmonizer.core.config.input.jharmonizer.CreationHelper.createTopLevelTypesOrdering;
import static io.github.lemon_ant.jharmonizer.core.config.input.jharmonizer.CreationHelper.createTypeGroup;
import static org.assertj.core.api.Assertions.assertThat;

import io.github.lemon_ant.jharmonizer.core.config.input.jharmonizer.FormatterStyle;
import io.github.lemon_ant.jharmonizer.core.config.input.jharmonizer.JHarmonizerConfig;
import io.github.lemon_ant.jharmonizer.core.config.input.jharmonizer.JHarmonizerInputToUnifiedConverter;
import io.github.lemon_ant.jharmonizer.core.config.input.jharmonizer.MemberGroup;
import io.github.lemon_ant.jharmonizer.core.config.input.jharmonizer.SortKey;
import io.github.lemon_ant.jharmonizer.core.config.input.jharmonizer.TopLevelTypesOrdering;
import io.github.lemon_ant.jharmonizer.core.config.input.jharmonizer.TypeKind;
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

        TopLevelTypesOrdering topLevel = createTopLevelTypesOrdering(
                false,
                List.of(createTypeGroup(Set.of(TypeKind.CLASS))), // type-groups (can be empty for this smoke test)
                List.of(SortKey.ALPHA) // sort-keys (can be empty for this smoke test)
                );

        JHarmonizerConfig jHarmonizerConfig = JHarmonizerConfig.builder()
                .memberGroups(List.of(root))
                .topLevelTypesOrdering(topLevel)
                .formatterStyle(FormatterStyle.PALANTIR)
                .headerLine(createHeaderLine('-', 5))
                .build();

        UnifiedConfig unified = JHarmonizerInputToUnifiedConverter.convert2Unified(jHarmonizerConfig);

        assertThat(unified.getRootMemberGroups()).hasSize(1);
        assertThat(unified.getRootMemberGroups().getFirst().getSelectorBlock().getIncludes())
                .hasSize(2);
    }
}
