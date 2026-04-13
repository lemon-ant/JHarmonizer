package io.github.lemon_ant.jharmonizer.core.config.compiled;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.lemon_ant.jharmonizer.core.config.ConfigurationManager;
import io.github.lemon_ant.jharmonizer.core.config.unified.DeclarationModifier;
import io.github.lemon_ant.jharmonizer.core.config.unified.MemberAccess;
import io.github.lemon_ant.jharmonizer.core.config.unified.MemberDescriptor;
import io.github.lemon_ant.jharmonizer.core.config.unified.MemberKind;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

class CompiledConfigTest {

    private static final CompiledConfig DEFAULT_CONFIG = ConfigurationManager.loadDefaultConfig();

    @Test
    void matchRootGroup_fieldDescriptor_returnsNonEmpty() {
        // Given
        MemberDescriptor fieldDescriptor = MemberDescriptor.builder()
                .name("someField")
                .memberKind(MemberKind.FIELD)
                .memberAccess(MemberAccess.PRIVATE)
                .declarationModifiers(Set.of())
                .build();

        // When
        Optional<CompiledMemberGroup> matchedGroup = DEFAULT_CONFIG.matchRootGroup(fieldDescriptor);

        // Then
        assertThat(matchedGroup).isPresent();
    }

    @Test
    void matchRootGroup_methodDescriptor_returnsNonEmpty() {
        // Given
        MemberDescriptor methodDescriptor = MemberDescriptor.builder()
                .name("someMethod")
                .memberKind(MemberKind.METHOD)
                .memberAccess(MemberAccess.PUBLIC)
                .declarationModifiers(Set.of())
                .build();

        // When
        Optional<CompiledMemberGroup> matchedGroup = DEFAULT_CONFIG.matchRootGroup(methodDescriptor);

        // Then
        assertThat(matchedGroup).isPresent();
    }

    @Test
    void matchRootGroup_staticFieldDescriptor_returnsNonEmpty() {
        // Given
        MemberDescriptor staticFieldDescriptor = MemberDescriptor.builder()
                .name("STATIC_FIELD")
                .memberKind(MemberKind.FIELD)
                .memberAccess(MemberAccess.PUBLIC)
                .declarationModifiers(Set.of(DeclarationModifier.STATIC, DeclarationModifier.FINAL))
                .build();

        // When
        Optional<CompiledMemberGroup> matchedGroup = DEFAULT_CONFIG.matchRootGroup(staticFieldDescriptor);

        // Then
        assertThat(matchedGroup).isPresent();
    }
}
