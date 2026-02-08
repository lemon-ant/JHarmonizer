package io.github.lemon_ant.jharmonizer.core.sorter.spoon;

import io.github.lemon_ant.jharmonizer.core.config.compiled.CompiledConfig;
import io.github.lemon_ant.jharmonizer.core.config.compiled.CompiledMemberGroup;
import io.github.lemon_ant.jharmonizer.core.config.unified.MemberDescriptor;
import io.github.lemon_ant.jharmonizer.core.sorter.spoon.dependency_graph.MemberDependencyGraph;
import io.github.lemon_ant.jharmonizer.core.sorter.spoon.dependency_graph.MemberDependencyGraphBuilder;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import spoon.reflect.declaration.CtCompilationUnit;
import spoon.reflect.declaration.CtType;
import spoon.reflect.declaration.CtTypeMember;

/**
 * Root coordinator for sorting:
 * - (later) reorders top-level types in the compilation unit,
 * - recursively processes nested types (depth-first),
 * - applies member sorting to each type via
 */
@AllArgsConstructor
public class SpoonSorter {

    @NonNull
    CompiledConfig compiledConfig;

    /**
     * Flattens blocks into a single member list preserving the block order.
     * Group boundary markers are expected to be applied before flattening.
     */
    private static List<CtTypeMember> flattenMembers(List<MemberGroupBlock> memberGroupBlocks) {
        return memberGroupBlocks.stream()
                .flatMap(memberGroupBlock -> memberGroupBlock.getTypeMembers().stream())
                .toList();
    }

    /**
     * Entry point used by Sorter: sorts all types (top-level + nested) in the compilation unit.
     */
    public void sortCompilationUnitRecursively(@NonNull CtCompilationUnit compilationUnit) {

        // TODO Implement top-level types ordering:
        // reorderTopLevelTypes(compilationUnit, compiledConfig);

        compilationUnit.getDeclaredTypes().forEach(this::sortTypeRecursively);
    }

    /**
     * Depth-first recursion:
     * 1) process nested types,
     * 2) sort current type members,
     * 3) apply group boundary markers and flatten.
     * <p>
     * This order keeps the logic deterministic and ensures nested types are already "clean"
     * when the outer type is printed.
     */
    private void sortTypeRecursively(CtType<?> currentType) {
        MemberDescriptor topLevelTypeDescriptor = SpoonMemberDescriptorFactory.describeMember(currentType);

        CompiledMemberGroup rootMemberGroup = compiledConfig
                .matchRootGroup(topLevelTypeDescriptor)
                .orElseThrow(() ->
                        new IllegalStateException("No matching root member group for top-level type: qualifiedName="
                                + currentType.getQualifiedName()
                                + ", descriptor=" + topLevelTypeDescriptor));
        currentType.getNestedTypes().forEach(this::sortTypeRecursively);

        Map<CtTypeMember, MemberDescriptor> typeMember2Descriptor =
                SpoonMemberDescriptorFactory.describeMembers(currentType);

        Map<CtTypeMember, CompiledMemberGroup> naturalGroupByMember =
                NaturalMemberGroupResolver.resolveNaturalGroups(rootMemberGroup, typeMember2Descriptor);

        MemberDependencyGraph memberDependencyGraph =
                MemberDependencyGraphBuilder.buildDependencyGraph(naturalGroupByMember);

        Map<CtTypeMember, CompiledMemberGroup> effectiveGroupByMember =
                EffectiveMemberGroupResolver.resolveEffectiveGroups(naturalGroupByMember, memberDependencyGraph);

        List<MemberGroupBlock> memberGroupBlocks =
                TypeMemberGrouper.groupMembersByEffectiveGroups(effectiveGroupByMember);

        List<MemberGroupBlock> orderedMemberGroupBlocks =
                GroupMembersOrderer.orderMembersInsideGroups(memberGroupBlocks, memberDependencyGraph);

        GroupBoundaryMarker.markGroupBoundaries(orderedMemberGroupBlocks);

        List<CtTypeMember> flattenedSortedMembers = flattenMembers(orderedMemberGroupBlocks);

        // Apply to Spoon model.
        currentType.setTypeMembers(flattenedSortedMembers);
    }
}
