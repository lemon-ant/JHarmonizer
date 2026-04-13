package io.github.lemon_ant.jharmonizer.core.config.unified;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import org.junit.jupiter.api.Test;

class UnifiedMemberGroupSelectorBlockTest {

    @Test
    void equals_sameInstance_returnsTrue() {
        // Given
        UnifiedMemberGroupSelectorBlock selectorBlock = new UnifiedMemberGroupSelectorBlock(Set.of(), Set.of());

        // When / Then
        assertThat(selectorBlock).isEqualTo(selectorBlock);
    }

    @Test
    void equals_nonSelectorBlockObject_returnsFalse() {
        // Given
        UnifiedMemberGroupSelectorBlock selectorBlock = new UnifiedMemberGroupSelectorBlock(Set.of(), Set.of());

        // When / Then
        assertThat(selectorBlock).isNotEqualTo("not a selector block");
    }

    @Test
    void equals_differentIncludes_returnsFalse() {
        // Given
        UnifiedMemberGroupRuleLine ruleLine = UnifiedMemberGroupRuleLine.builder()
                .memberKind(MemberKind.FIELD)
                .build();
        UnifiedMemberGroupSelectorBlock withIncludes = new UnifiedMemberGroupSelectorBlock(Set.of(), Set.of(ruleLine));
        UnifiedMemberGroupSelectorBlock withoutIncludes = new UnifiedMemberGroupSelectorBlock(Set.of(), Set.of());

        // When / Then
        assertThat(withIncludes).isNotEqualTo(withoutIncludes);
    }

    @Test
    void equals_differentExcludes_returnsFalse() {
        // Given
        UnifiedMemberGroupRuleLine ruleLine = UnifiedMemberGroupRuleLine.builder()
                .memberKind(MemberKind.FIELD)
                .build();
        UnifiedMemberGroupSelectorBlock withExcludes = new UnifiedMemberGroupSelectorBlock(Set.of(ruleLine), Set.of());
        UnifiedMemberGroupSelectorBlock withoutExcludes = new UnifiedMemberGroupSelectorBlock(Set.of(), Set.of());

        // When / Then
        assertThat(withExcludes).isNotEqualTo(withoutExcludes);
    }

    @Test
    void equals_identicalBlocks_returnsTrue() {
        // Given
        UnifiedMemberGroupRuleLine ruleLine = UnifiedMemberGroupRuleLine.builder()
                .memberKind(MemberKind.FIELD)
                .build();
        UnifiedMemberGroupSelectorBlock firstSelectorBlock =
                new UnifiedMemberGroupSelectorBlock(Set.of(), Set.of(ruleLine));
        UnifiedMemberGroupSelectorBlock secondSelectorBlock =
                new UnifiedMemberGroupSelectorBlock(Set.of(), Set.of(ruleLine));

        // When / Then
        assertThat(firstSelectorBlock).isEqualTo(secondSelectorBlock);
    }

    @Test
    void hashCode_identicalBlocks_produceSameValue() {
        // Given
        UnifiedMemberGroupSelectorBlock firstSelectorBlock = new UnifiedMemberGroupSelectorBlock(Set.of(), Set.of());
        UnifiedMemberGroupSelectorBlock secondSelectorBlock = new UnifiedMemberGroupSelectorBlock(Set.of(), Set.of());

        // When / Then
        assertThat(firstSelectorBlock.hashCode()).isEqualTo(secondSelectorBlock.hashCode());
    }
}
