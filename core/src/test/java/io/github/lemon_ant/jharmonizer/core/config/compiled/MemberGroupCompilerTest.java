package io.github.lemon_ant.jharmonizer.core.config.compiled;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.lemon_ant.jharmonizer.core.config.unified.UnifiedMemberGroup;
import io.github.lemon_ant.jharmonizer.core.config.unified.UnifiedMemberGroupSelectorBlock;
import io.github.lemon_ant.jharmonizer.core.config.unified.UnifiedSeparator;
import io.github.lemon_ant.jharmonizer.core.config.unified.UnifiedSortKey;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

class MemberGroupCompilerTest {

    private static UnifiedMemberGroup createGroup(
            String groupName,
            Boolean keepAccessorsTogether,
            List<UnifiedMemberGroup> memberSubGroups,
            List<UnifiedSortKey> sortKeys) {
        UnifiedMemberGroupSelectorBlock selectorBlock =
                UnifiedMemberGroupSelectorBlock.builder().build();
        return UnifiedMemberGroup.builder()
                .groupName(groupName)
                .keepAccessorsTogether(keepAccessorsTogether)
                .memberSubGroups(memberSubGroups)
                .selectorBlock(selectorBlock)
                .separator(UnifiedSeparator.NONE)
                .sortKeys(sortKeys)
                .build();
    }

    private static List<CompiledMemberGroup> collectAllGroups(List<CompiledMemberGroup> rootGroups) {
        Deque<CompiledMemberGroup> queue = new ArrayDeque<>(rootGroups);
        List<CompiledMemberGroup> collectedGroups = new java.util.ArrayList<>();
        while (!queue.isEmpty()) {
            CompiledMemberGroup currentGroup = queue.removeFirst();
            collectedGroups.add(currentGroup);
            currentGroup.getCompiledSubGroups().forEach(queue::addLast);
        }
        return collectedGroups;
    }

    @Test
    void compileTopLevelGroups_treeAndForest_postOrderIndexesAreStableAndContiguous() {
        // Given
        UnifiedMemberGroup leafB = createGroup("B", null, List.of(), List.of(UnifiedSortKey.PRESERVE));
        UnifiedMemberGroup leafD = createGroup("D", null, List.of(), List.of(UnifiedSortKey.PRESERVE));
        UnifiedMemberGroup nodeC = createGroup("C", null, List.of(leafD), List.of(UnifiedSortKey.PRESERVE));
        UnifiedMemberGroup rootA = createGroup("A", null, List.of(leafB, nodeC), List.of(UnifiedSortKey.PRESERVE));
        UnifiedMemberGroup rootE = createGroup("E", null, List.of(), List.of(UnifiedSortKey.PRESERVE));

        // When
        List<CompiledMemberGroup> compiledRoots = MemberGroupCompiler.compileTopLevelGroups(List.of(rootA, rootE));

        // Then
        CompiledMemberGroup compiledA = compiledRoots.getFirst();
        CompiledMemberGroup compiledE = compiledRoots.get(1);
        CompiledMemberGroup compiledB = compiledA.getCompiledSubGroups().getFirst();
        CompiledMemberGroup compiledC = compiledA.getCompiledSubGroups().get(1);
        CompiledMemberGroup compiledD = compiledC.getCompiledSubGroups().getFirst();

        assertThat(compiledB.getOrderIndex()).isEqualTo(0);
        assertThat(compiledD.getOrderIndex()).isEqualTo(1);
        assertThat(compiledC.getOrderIndex()).isEqualTo(2);
        assertThat(compiledA.getOrderIndex()).isEqualTo(3);
        assertThat(compiledE.getOrderIndex()).isEqualTo(4);

        List<CompiledMemberGroup> allCompiledGroups = collectAllGroups(compiledRoots);

        Set<Integer> uniqueOrderIndexes = new HashSet<>();
        allCompiledGroups.stream().map(CompiledMemberGroup::getOrderIndex).forEach(uniqueOrderIndexes::add);

        assertThat(uniqueOrderIndexes).hasSize(allCompiledGroups.size());

        List<Integer> expectedIndexes =
                IntStream.range(0, allCompiledGroups.size()).boxed().toList();
        assertThat(uniqueOrderIndexes).containsExactlyInAnyOrderElementsOf(expectedIndexes);

        // Each parent must have a greater index than any of its descendants.
        assertThat(compiledA.getOrderIndex()).isGreaterThan(compiledB.getOrderIndex());
        assertThat(compiledA.getOrderIndex()).isGreaterThan(compiledC.getOrderIndex());
        assertThat(compiledA.getOrderIndex()).isGreaterThan(compiledD.getOrderIndex());
        assertThat(compiledC.getOrderIndex()).isGreaterThan(compiledD.getOrderIndex());
    }

    @Test
    void compileTopLevelGroups_keepAccessorsTogether_isInheritedAndCanBeOverridden() {
        // Given
        UnifiedMemberGroup inheritedChild =
                createGroup("InheritedChild", null, List.of(), List.of(UnifiedSortKey.ALPHA));
        UnifiedMemberGroup overriddenChild =
                createGroup("OverriddenChild", false, List.of(), List.of(UnifiedSortKey.ALPHA));
        UnifiedMemberGroup inheritedUnderOverride =
                createGroup("InheritedUnderOverride", null, List.of(), List.of(UnifiedSortKey.ALPHA));
        UnifiedMemberGroup overrideContainer = createGroup(
                "OverrideContainer",
                false,
                List.of(overriddenChild, inheritedUnderOverride),
                List.of(UnifiedSortKey.ALPHA));
        UnifiedMemberGroup root =
                createGroup("Root", true, List.of(inheritedChild, overrideContainer), List.of(UnifiedSortKey.ALPHA));

        // When
        List<CompiledMemberGroup> compiledRoots = MemberGroupCompiler.compileTopLevelGroups(List.of(root));
        CompiledMemberGroup compiledRoot = compiledRoots.getFirst();

        // Then
        CompiledMemberGroup compiledInheritedChild =
                compiledRoot.getCompiledSubGroups().getFirst();
        CompiledMemberGroup compiledOverrideContainer =
                compiledRoot.getCompiledSubGroups().get(1);
        CompiledMemberGroup compiledOverriddenChild =
                compiledOverrideContainer.getCompiledSubGroups().getFirst();
        CompiledMemberGroup compiledInheritedUnderOverride =
                compiledOverrideContainer.getCompiledSubGroups().get(1);

        assertThat(compiledRoot.isKeepAccessorsTogether()).isTrue();
        assertThat(compiledInheritedChild.isKeepAccessorsTogether()).isTrue();
        assertThat(compiledOverrideContainer.isKeepAccessorsTogether()).isFalse();
        assertThat(compiledOverriddenChild.isKeepAccessorsTogether()).isFalse();
        assertThat(compiledInheritedUnderOverride.isKeepAccessorsTogether()).isFalse();
    }

    @Test
    void compileTopLevelGroups_sortKeys_areMappedOneToOneAndOrderIsPreserved() {
        // Given
        UnifiedMemberGroup root = createGroup(
                "Root",
                null,
                List.of(),
                List.of(UnifiedSortKey.VISIBILITY_DESC, UnifiedSortKey.ALPHA, UnifiedSortKey.PRESERVE));

        // When
        CompiledMemberGroup compiledRoot =
                MemberGroupCompiler.compileTopLevelGroups(List.of(root)).getFirst();

        // Then
        assertThat(compiledRoot.getSortKeys())
                .containsExactly(SortKey.VISIBILITY_DESC, SortKey.ALPHA, SortKey.PRESERVE);
    }
}
