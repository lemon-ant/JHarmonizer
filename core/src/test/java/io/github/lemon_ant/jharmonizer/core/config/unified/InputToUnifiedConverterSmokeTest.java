package io.github.lemon_ant.jharmonizer.core.config.unified;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.lemon_ant.jharmonizer.core.config.input.jharmonizer.JHarmonizerConfig;
import io.github.lemon_ant.jharmonizer.core.config.input.jharmonizer.JHarmonizerInputToUnifiedConverter;
import io.github.lemon_ant.jharmonizer.core.config.input.jharmonizer.MemberGroup;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * Smoke test: verifies that converter parses basic tokens into Unified model
 * without side-effects. Focuses on token normalization and name/annotation tokens.
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
                .build();

        JHarmonizerConfig cfg = JHarmonizerConfig.builder()
                .typeMembersOrdering(List.of(root))
                .build();

        UnifiedConfig unified = new JHarmonizerInputToUnifiedConverter().convert(cfg);

        assertThat(unified.getRootMemberGroups()).hasSize(1);
        assertThat(unified.getRootMemberGroups().get(0).getSelectors().getIncludes()).hasSize(2);
    }
}
