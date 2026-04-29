package io.github.lemon_ant.jharmonizer.core.config.compiled;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.lemon_ant.jharmonizer.core.config.unified.UnifiedMemberGroup;
import io.github.lemon_ant.jharmonizer.core.config.unified.UnifiedMemberGroupSelectorBlock;
import io.github.lemon_ant.jharmonizer.core.config.unified.UnifiedOrderingRule;
import io.github.lemon_ant.jharmonizer.core.config.unified.UnifiedSeparator;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;
import lombok.NonNull;
import org.junit.jupiter.api.Test;

class MemberGroupCompilerTest {

    @Test
    void compileTopLevelGroups_treeAndForest_postOrderIndexesAreStableAndContiguous() {
        // Given
        UnifiedMemberGroup leafB = createGroup("B", null, List.of(), List.of(UnifiedOrderingRule.PRESERVE));
        UnifiedMemberGroup leafD = createGroup("D", null, List.of(), List.of(UnifiedOrderingRule.PRESERVE));
        UnifiedMemberGroup nodeC = createGroup("C", null, List.of(leafD), List.of(UnifiedOrderingRule.PRESERVE));
        UnifiedMemberGroup rootA = createGroup("A", null, List.of(leafB, nodeC), List.of(UnifiedOrderingRule.PRESERVE));
        UnifiedMemberGroup rootE = createGroup("E", null, List.of(), List.of(UnifiedOrderingRule.PRESERVE));

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
                createGroup("InheritedChild", null, List.of(), List.of(UnifiedOrderingRule.ALPHA));
        UnifiedMemberGroup overriddenChild =
                createGroup("OverriddenChild", false, List.of(), List.of(UnifiedOrderingRule.ALPHA));
        UnifiedMemberGroup inheritedUnderOverride =
                createGroup("InheritedUnderOverride", null, List.of(), List.of(UnifiedOrderingRule.ALPHA));
        UnifiedMemberGroup overrideContainer = createGroup(
                "OverrideContainer",
                false,
                List.of(overriddenChild, inheritedUnderOverride),
                List.of(UnifiedOrderingRule.ALPHA));
        UnifiedMemberGroup root = createGroup(
                "Root", true, List.of(inheritedChild, overrideContainer), List.of(UnifiedOrderingRule.ALPHA));

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
    void compileTopLevelGroups_orderingRules_areMappedOneToOneAndOrderIsPreserved() {
        // Given
        UnifiedMemberGroup root = createGroup(
                "Root",
                null,
                List.of(),
                List.of(UnifiedOrderingRule.VISIBILITY_DESC, UnifiedOrderingRule.ALPHA, UnifiedOrderingRule.PRESERVE));

        // When
        CompiledMemberGroup compiledRoot =
                MemberGroupCompiler.compileTopLevelGroups(List.of(root)).getFirst();

        // Then
        assertThat(compiledRoot.getOrderingRules())
                .containsExactly(OrderingRule.VISIBILITY_DESC, OrderingRule.ALPHA, OrderingRule.PRESERVE);
    }

    @Test
    void compileTopLevelGroups_separator_isInheritedAndCanBeOverridden() {
        // Given
        UnifiedMemberGroup inheritedChild = createGroup("InheritedChild", null, null, null, List.of(), null);
        UnifiedMemberGroup overriddenChild = createGroup(
                "OverriddenChild", null, null, UnifiedSeparator.HEADER, List.of(), List.of(UnifiedOrderingRule.ALPHA));
        UnifiedMemberGroup root = createGroup(
                "Root",
                null,
                null,
                UnifiedSeparator.NEW_LINE,
                List.of(inheritedChild, overriddenChild),
                List.of(UnifiedOrderingRule.ALPHA));

        // When
        CompiledMemberGroup compiledRoot =
                MemberGroupCompiler.compileTopLevelGroups(List.of(root)).getFirst();

        // Then
        CompiledMemberGroup compiledInheritedChild =
                compiledRoot.getCompiledSubGroups().getFirst();
        CompiledMemberGroup compiledOverriddenChild =
                compiledRoot.getCompiledSubGroups().get(1);
        assertThat(compiledRoot.getSeparator()).isEqualTo(UnifiedSeparator.NEW_LINE);
        assertThat(compiledInheritedChild.getSeparator()).isEqualTo(UnifiedSeparator.NEW_LINE);
        assertThat(compiledOverriddenChild.getSeparator()).isEqualTo(UnifiedSeparator.HEADER);
    }

    @Test
    void compileTopLevelGroups_orderingRules_areInheritedAndCanBeOverridden() {
        // Given
        List<UnifiedOrderingRule> parentOrderingRules = List.of(UnifiedOrderingRule.ALPHA);
        List<UnifiedOrderingRule> childOrderingRules = List.of(UnifiedOrderingRule.PRESERVE);
        UnifiedMemberGroup inheritedChild = createGroup("InheritedChild", null, null, null, List.of(), null);
        UnifiedMemberGroup overriddenChild =
                createGroup("OverriddenChild", null, null, null, List.of(), childOrderingRules);
        UnifiedMemberGroup root = createGroup(
                "Root",
                null,
                null,
                UnifiedSeparator.NONE,
                List.of(inheritedChild, overriddenChild),
                parentOrderingRules);

        // When
        CompiledMemberGroup compiledRoot =
                MemberGroupCompiler.compileTopLevelGroups(List.of(root)).getFirst();

        // Then
        CompiledMemberGroup compiledInheritedChild =
                compiledRoot.getCompiledSubGroups().getFirst();
        CompiledMemberGroup compiledOverriddenChild =
                compiledRoot.getCompiledSubGroups().get(1);
        assertThat(compiledRoot.getOrderingRules()).containsExactly(OrderingRule.ALPHA);
        assertThat(compiledInheritedChild.getOrderingRules()).containsExactly(OrderingRule.ALPHA);
        assertThat(compiledOverriddenChild.getOrderingRules()).containsExactly(OrderingRule.PRESERVE);
    }

    @Test
    void compileTopLevelGroups_orderingRules_emptyList_isAllowed() {
        // Given
        UnifiedMemberGroup child = createGroup("Child", null, null, null, List.of(), null);
        UnifiedMemberGroup root = createGroup("Root", null, null, UnifiedSeparator.NONE, List.of(child), List.of());

        // When
        CompiledMemberGroup compiledRoot =
                MemberGroupCompiler.compileTopLevelGroups(List.of(root)).getFirst();

        // Then
        CompiledMemberGroup compiledChild = compiledRoot.getCompiledSubGroups().getFirst();
        assertThat(compiledRoot.getOrderingRules()).isEmpty();
        assertThat(compiledChild.getOrderingRules()).isEmpty();
    }

    @Test
    void compileTopLevelGroups_relaxedForwardReferences_isInheritedAndCanBeOverridden() {
        // Given
        UnifiedMemberGroup inheritedChild =
                createGroup("InheritedChild", null, null, List.of(), List.of(UnifiedOrderingRule.ALPHA));
        UnifiedMemberGroup overriddenChild =
                createGroup("OverriddenChild", null, true, List.of(), List.of(UnifiedOrderingRule.ALPHA));
        UnifiedMemberGroup inheritedUnderOverride =
                createGroup("InheritedUnderOverride", null, null, List.of(), List.of(UnifiedOrderingRule.ALPHA));
        UnifiedMemberGroup overrideContainer = createGroup(
                "OverrideContainer",
                null,
                true,
                List.of(overriddenChild, inheritedUnderOverride),
                List.of(UnifiedOrderingRule.ALPHA));
        UnifiedMemberGroup root = createGroup(
                "Root", null, false, List.of(inheritedChild, overrideContainer), List.of(UnifiedOrderingRule.ALPHA));

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
        assertThat(compiledRoot.isRelaxedForwardReferences()).isFalse();
        assertThat(compiledInheritedChild.isRelaxedForwardReferences()).isFalse();
        assertThat(compiledOverrideContainer.isRelaxedForwardReferences()).isTrue();
        assertThat(compiledOverriddenChild.isRelaxedForwardReferences()).isTrue();
        assertThat(compiledInheritedUnderOverride.isRelaxedForwardReferences()).isTrue();
    }

    @Test
    void compileTopLevelGroups_relaxedForwardReferences_defaultsToTrue() {
        // Given
        UnifiedMemberGroup child = createGroup("Child", null, null, List.of(), List.of(UnifiedOrderingRule.ALPHA));
        UnifiedMemberGroup root = createGroup("Root", null, null, List.of(child), List.of(UnifiedOrderingRule.ALPHA));

        // When
        CompiledMemberGroup compiledRoot =
                MemberGroupCompiler.compileTopLevelGroups(List.of(root)).getFirst();

        // Then
        CompiledMemberGroup compiledChild = compiledRoot.getCompiledSubGroups().getFirst();
        assertThat(compiledRoot.isRelaxedForwardReferences()).isTrue();
        assertThat(compiledChild.isRelaxedForwardReferences()).isTrue();
    }

    @NonNull
    private static UnifiedMemberGroup createGroup(
            String groupName,
            Boolean keepAccessorsTogether,
            List<UnifiedMemberGroup> memberSubGroups,
            List<UnifiedOrderingRule> orderingRules) {
        return createGroup(
                groupName, keepAccessorsTogether, null, UnifiedSeparator.NONE, memberSubGroups, orderingRules);
    }

    @NonNull
    private static UnifiedMemberGroup createGroup(
            String groupName,
            Boolean keepAccessorsTogether,
            Boolean relaxedForwardReferences,
            List<UnifiedMemberGroup> memberSubGroups,
            List<UnifiedOrderingRule> orderingRules) {
        return createGroup(
                groupName,
                keepAccessorsTogether,
                relaxedForwardReferences,
                UnifiedSeparator.NONE,
                memberSubGroups,
                orderingRules);
    }

    @NonNull
    private static UnifiedMemberGroup createGroup(
            String groupName,
            Boolean keepAccessorsTogether,
            Boolean relaxedForwardReferences,
            UnifiedSeparator separator,
            List<UnifiedMemberGroup> memberSubGroups,
            List<UnifiedOrderingRule> orderingRules) {
        UnifiedMemberGroupSelectorBlock selectorBlock =
                UnifiedMemberGroupSelectorBlock.builder().build();
        return UnifiedMemberGroup.builder()
                .groupName(groupName)
                .keepAccessorsTogether(keepAccessorsTogether)
                .relaxedForwardReferences(relaxedForwardReferences)
                .memberSubGroups(memberSubGroups)
                .selectorBlock(selectorBlock)
                .separator(separator)
                .orderingRules(orderingRules)
                .build();
    }

    @NonNull
    private static List<CompiledMemberGroup> collectAllGroups(List<CompiledMemberGroup> rootGroups) {
        Deque<CompiledMemberGroup> queue = new ArrayDeque<>(rootGroups);
        List<CompiledMemberGroup> collectedGroups = new ArrayList<>();
        while (!queue.isEmpty()) {
            CompiledMemberGroup currentGroup = queue.removeFirst();
            collectedGroups.add(currentGroup);
            currentGroup.getCompiledSubGroups().forEach(queue::addLast);
        }
        return collectedGroups;
    }
}
