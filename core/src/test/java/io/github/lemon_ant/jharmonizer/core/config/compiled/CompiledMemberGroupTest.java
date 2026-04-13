package io.github.lemon_ant.jharmonizer.core.config.compiled;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.lemon_ant.jharmonizer.core.config.unified.MemberAccess;
import io.github.lemon_ant.jharmonizer.core.config.unified.MemberDescriptor;
import io.github.lemon_ant.jharmonizer.core.config.unified.MemberKind;
import io.github.lemon_ant.jharmonizer.core.config.unified.UnifiedSeparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class CompiledMemberGroupTest {

    @Test
    void build_nameMissing_preservesNullName() {
        // When
        CompiledMemberGroup compiledMemberGroup = CompiledMemberGroup.builder()
                .compiledSubGroups(List.of())
                .keepAccessorsTogether(false)
                .orderIndex(0)
                .selectorBlock(new CompiledMemberGroupSelectorBlock(List.of(), List.of()))
                .separator(UnifiedSeparator.NONE)
                .orderingRule(OrderingRule.PRESERVE)
                .build();

        // Then
        assertThat(compiledMemberGroup.getName()).isNull();
    }

    @Nested
    class ClassifyRecursively {

        @Test
        void classifyRecursively_selectorDoesNotMatch_returnsEmpty() {
            // Given
            CompiledMemberGroupSelectorBlock noMatchBlock =
                    new CompiledMemberGroupSelectorBlock(List.of(descriptor -> false), List.of());
            CompiledMemberGroup compiledMemberGroup = buildGroup(noMatchBlock, List.of());
            MemberDescriptor fieldDescriptor = buildFieldDescriptor("someField", MemberAccess.PUBLIC);

            // When
            Optional<CompiledMemberGroup> classifyResult = compiledMemberGroup.classifyRecursively(fieldDescriptor);

            // Then
            assertThat(classifyResult).isEmpty();
        }

        @Test
        void classifyRecursively_selectorMatchesNoChildren_returnsParent() {
            // Given
            CompiledMemberGroupSelectorBlock matchAllBlock = new CompiledMemberGroupSelectorBlock(List.of(), List.of());
            CompiledMemberGroup compiledMemberGroup = buildGroup(matchAllBlock, List.of());
            MemberDescriptor fieldDescriptor = buildFieldDescriptor("someField", MemberAccess.PUBLIC);

            // When
            Optional<CompiledMemberGroup> classifyResult = compiledMemberGroup.classifyRecursively(fieldDescriptor);

            // Then
            assertThat(classifyResult).isPresent();
            assertThat(classifyResult.get()).isSameAs(compiledMemberGroup);
        }

        @Test
        void classifyRecursively_selectorMatchesWithMatchingChild_returnsChild() {
            // Given
            CompiledMemberGroupSelectorBlock matchAllBlock = new CompiledMemberGroupSelectorBlock(List.of(), List.of());
            CompiledMemberGroup childGroup = buildGroup(matchAllBlock, List.of());
            CompiledMemberGroup parentGroup = buildGroup(matchAllBlock, List.of(childGroup));
            MemberDescriptor fieldDescriptor = buildFieldDescriptor("someField", MemberAccess.PUBLIC);

            // When
            Optional<CompiledMemberGroup> classifyResult = parentGroup.classifyRecursively(fieldDescriptor);

            // Then
            assertThat(classifyResult).isPresent();
            assertThat(classifyResult.get()).isSameAs(childGroup);
        }

        @Test
        void classifyRecursively_selectorMatchesWithNonMatchingChild_returnsParent() {
            // Given
            CompiledMemberGroupSelectorBlock matchAllBlock = new CompiledMemberGroupSelectorBlock(List.of(), List.of());
            CompiledMemberGroupSelectorBlock noMatchBlock =
                    new CompiledMemberGroupSelectorBlock(List.of(descriptor -> false), List.of());
            CompiledMemberGroup childGroup = buildGroup(noMatchBlock, List.of());
            CompiledMemberGroup parentGroup = buildGroup(matchAllBlock, List.of(childGroup));
            MemberDescriptor fieldDescriptor = buildFieldDescriptor("someField", MemberAccess.PUBLIC);

            // When
            Optional<CompiledMemberGroup> classifyResult = parentGroup.classifyRecursively(fieldDescriptor);

            // Then
            assertThat(classifyResult).isPresent();
            assertThat(classifyResult.get()).isSameAs(parentGroup);
        }
    }

    @Nested
    class Equality {

        @Test
        void equals_sameInstance_returnsTrue() {
            // Given
            CompiledMemberGroup compiledMemberGroup = buildDefaultGroup();

            // When / Then
            assertThat(compiledMemberGroup).isEqualTo(compiledMemberGroup);
        }

        @Test
        void equals_nonCompiledMemberGroupObject_returnsFalse() {
            // Given
            CompiledMemberGroup compiledMemberGroup = buildDefaultGroup();

            // When / Then
            assertThat(compiledMemberGroup).isNotEqualTo("not a group");
        }

        @Test
        void equals_differentOrderIndex_returnsFalse() {
            // Given
            CompiledMemberGroup compiledMemberGroup = buildGroupWithOrderIndex(0);
            CompiledMemberGroup differentOrderGroup = buildGroupWithOrderIndex(1);

            // When / Then
            assertThat(compiledMemberGroup).isNotEqualTo(differentOrderGroup);
        }

        @Test
        void equals_differentKeepAccessorsTogether_returnsFalse() {
            // Given
            CompiledMemberGroup groupWithAccessors = buildGroupWithKeepAccessors(true);
            CompiledMemberGroup groupWithoutAccessors = buildGroupWithKeepAccessors(false);

            // When / Then
            assertThat(groupWithAccessors).isNotEqualTo(groupWithoutAccessors);
        }

        @Test
        void equals_identicalGroups_returnsTrue() {
            // Given
            CompiledMemberGroup firstGroup = buildDefaultGroup();
            CompiledMemberGroup secondGroup = buildDefaultGroup();

            // When / Then
            assertThat(firstGroup).isEqualTo(secondGroup);
        }

        @Test
        void hashCode_identicalGroups_produceSameValue() {
            // Given
            CompiledMemberGroup firstGroup = buildDefaultGroup();
            CompiledMemberGroup secondGroup = buildDefaultGroup();

            // When / Then
            assertThat(firstGroup.hashCode()).isEqualTo(secondGroup.hashCode());
        }
    }

    private static CompiledMemberGroup buildDefaultGroup() {
        return buildGroup(new CompiledMemberGroupSelectorBlock(List.of(), List.of()), List.of());
    }

    private static CompiledMemberGroup buildGroupWithOrderIndex(int orderIndex) {
        return CompiledMemberGroup.builder()
                .compiledSubGroups(List.of())
                .keepAccessorsTogether(false)
                .orderIndex(orderIndex)
                .selectorBlock(new CompiledMemberGroupSelectorBlock(List.of(), List.of()))
                .separator(UnifiedSeparator.NONE)
                .orderingRule(OrderingRule.PRESERVE)
                .build();
    }

    private static CompiledMemberGroup buildGroupWithKeepAccessors(boolean keepAccessors) {
        return CompiledMemberGroup.builder()
                .compiledSubGroups(List.of())
                .keepAccessorsTogether(keepAccessors)
                .orderIndex(0)
                .selectorBlock(new CompiledMemberGroupSelectorBlock(List.of(), List.of()))
                .separator(UnifiedSeparator.NONE)
                .orderingRule(OrderingRule.PRESERVE)
                .build();
    }

    private static CompiledMemberGroup buildGroup(
            CompiledMemberGroupSelectorBlock selectorBlock, List<CompiledMemberGroup> subGroups) {
        return CompiledMemberGroup.builder()
                .compiledSubGroups(subGroups)
                .keepAccessorsTogether(false)
                .orderIndex(0)
                .selectorBlock(selectorBlock)
                .separator(UnifiedSeparator.NONE)
                .orderingRule(OrderingRule.PRESERVE)
                .build();
    }

    private static MemberDescriptor buildFieldDescriptor(String name, MemberAccess memberAccess) {
        return MemberDescriptor.builder()
                .name(name)
                .memberKind(MemberKind.FIELD)
                .memberAccess(memberAccess)
                .declarationModifiers(Set.of())
                .build();
    }
}
