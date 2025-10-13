package io.github.lemon_ant.jharmonizer.core.config.unified;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.lemon_ant.jharmonizer.core.config.input.jharmonizer.JHarmonizerConfig;
import io.github.lemon_ant.jharmonizer.core.config.input.jharmonizer.JHarmonizerInputToUnifiedConverter;
import io.github.lemon_ant.jharmonizer.core.config.input.jharmonizer.MemberGroup;
import io.github.lemon_ant.jharmonizer.core.config.input.jharmonizer.SortKey;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * Smoke test aligned with the current API:
 * - uses static JHarmonizerInputToUnifiedConverter.convert2Unified(cfg)
 * - initializes all @NonNull fields of MemberGroup to avoid NPE
 * - asserts include-lines size via getSelectorBlock().getIncludes()
 */
class InputToUnifiedConverterSmokeTest {

    @Test
    void converter_shouldParseNameAndAnnotationTokens() {
        MemberGroup root = MemberGroup.builder()
            .name("Root")
            .includes(Set.of(
                Set.of("method", "=toString"),
                Set.of("method", "@~Size|Length")
            ))
            .sortKeys(List.of(SortKey.VISIBILITY_ASC, SortKey.ALPHA))
            .keepAccessorsTogether(false)
            .build();

        JHarmonizerConfig cfg = JHarmonizerConfig.builder()
            .typeMembersOrdering(List.of(root))
            .build();

        UnifiedConfig unified = JHarmonizerInputToUnifiedConverter.convert2Unified(cfg);

        assertThat(unified.getRootMemberGroups()).hasSize(1);
        assertThat(unified.getRootMemberGroups().getFirst().getSelectorBlock().getIncludes()).hasSize(2);
    }
}
