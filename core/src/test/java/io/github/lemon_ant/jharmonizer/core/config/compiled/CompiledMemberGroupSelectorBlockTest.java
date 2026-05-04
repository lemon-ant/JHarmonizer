// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
package io.github.lemon_ant.jharmonizer.core.config.compiled;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.lemon_ant.jharmonizer.core.config.unified.DeclarationModifier;
import io.github.lemon_ant.jharmonizer.core.config.unified.MemberAccess;
import io.github.lemon_ant.jharmonizer.core.config.unified.MemberDescriptor;
import io.github.lemon_ant.jharmonizer.core.config.unified.MemberKind;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import lombok.NonNull;
import org.junit.jupiter.api.Test;

class CompiledMemberGroupSelectorBlockTest {

    @Test
    void match_includesAndExcludesEmpty_returnTrue() {
        // Given
        CompiledMemberGroupSelectorBlock selectorBlock = new CompiledMemberGroupSelectorBlock(List.of(), List.of());
        MemberDescriptor anyFieldDescriptor = createFieldDescriptor("anyField", MemberAccess.PUBLIC);

        // When
        boolean matches = selectorBlock.match(anyFieldDescriptor);

        // Then
        assertThat(matches).isTrue();
    }

    @Test
    void match_includesEmptyAndExcludesMatch_returnFalse() {
        // Given
        CompiledMemberGroupSelectorBlock selectorBlock = new CompiledMemberGroupSelectorBlock(
                List.of(), List.of(descriptor -> descriptor.getMemberKind() == MemberKind.FIELD));
        MemberDescriptor fieldDescriptor = createFieldDescriptor("someField", MemberAccess.PUBLIC);

        // When
        boolean matches = selectorBlock.match(fieldDescriptor);

        // Then
        assertThat(matches).isFalse();
    }

    @Test
    void match_includesMatchAndExcludesEmpty_returnTrue() {
        // Given
        CompiledMemberGroupSelectorBlock selectorBlock = new CompiledMemberGroupSelectorBlock(
                List.of(descriptor -> descriptor.getMemberKind() == MemberKind.FIELD), List.of());
        MemberDescriptor fieldDescriptor = createFieldDescriptor("someField", MemberAccess.PUBLIC);

        // When
        boolean matches = selectorBlock.match(fieldDescriptor);

        // Then
        assertThat(matches).isTrue();
    }

    @Test
    void match_includesDoNotMatchAndExcludesEmpty_returnFalse() {
        // Given
        CompiledMemberGroupSelectorBlock selectorBlock = new CompiledMemberGroupSelectorBlock(
                List.of(descriptor -> descriptor.getMemberKind() == MemberKind.METHOD), List.of());
        MemberDescriptor fieldDescriptor = createFieldDescriptor("someField", MemberAccess.PUBLIC);

        // When
        boolean matches = selectorBlock.match(fieldDescriptor);

        // Then
        assertThat(matches).isFalse();
    }

    @Test
    void match_excludesTakePriorityOverIncludes_returnFalse() {
        // Given
        CompiledMemberGroupSelectorBlock selectorBlock = new CompiledMemberGroupSelectorBlock(
                List.of(descriptor -> descriptor.getMemberKind() == MemberKind.FIELD),
                List.of(descriptor -> descriptor.getDeclarationModifiers().contains(DeclarationModifier.STATIC)));
        MemberDescriptor staticFieldDescriptor =
                createFieldDescriptor("someStaticField", MemberAccess.PUBLIC, DeclarationModifier.STATIC);

        // When
        boolean matches = selectorBlock.match(staticFieldDescriptor);

        // Then
        assertThat(matches).isFalse();
    }

    @NonNull
    private static MemberDescriptor createFieldDescriptor(
            String fieldName, MemberAccess memberAccess, DeclarationModifier... declarationModifiers) {
        EnumSet<DeclarationModifier> declarationModifiersSet = declarationModifiers.length == 0
                ? EnumSet.noneOf(DeclarationModifier.class)
                : EnumSet.copyOf(Set.of(declarationModifiers));
        return MemberDescriptor.builder()
                .name(fieldName)
                .memberKind(MemberKind.FIELD)
                .memberAccess(memberAccess)
                .declarationModifiers(declarationModifiersSet)
                .build();
    }
}
