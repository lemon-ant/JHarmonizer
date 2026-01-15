package io.github.lemon_ant.jharmonizer.core.sorter.spoon;

import io.github.lemon_ant.jharmonizer.core.config.compiled.CompiledConfig;
import io.github.lemon_ant.jharmonizer.core.config.compiled.CompiledMemberGroup;
import io.github.lemon_ant.jharmonizer.core.config.unified.MemberDescriptor;
import io.github.lemon_ant.jharmonizer.core.sorter.spoon.dependency_graph.MemberDependencyGraph;
import io.github.lemon_ant.jharmonizer.core.sorter.spoon.dependency_graph.MemberDependencyGraphBuilder;
import java.util.List;
import java.util.Map;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import spoon.reflect.declaration.CtCompilationUnit;
import spoon.reflect.declaration.CtType;
import spoon.reflect.declaration.CtTypeMember;

/**
 * Root coordinator for sorting:
 * - (later) reorders top-level types in the compilation unit,
 * - recursively processes nested types (depth-first),
 * - applies member sorting to each type via
 */
@RequiredArgsConstructor
public class SpoonSorter {

    /**
     * Entry point used by Sorter: sorts all types (top-level + nested) in the compilation unit.
     */
    public static void sortCompilationUnitRecursively(
            @NonNull CompiledConfig compiledConfig, @NonNull CtCompilationUnit compilationUnit) {

        // TODO Implement top-level types ordering:
        // reorderTopLevelTypes(compilationUnit, compiledConfig);

        compilationUnit.getDeclaredTypes().forEach(declaredTopLevelType -> {
            MemberDescriptor topLevelTypeDescriptor = SpoonMemberDescriptorFactory.describeMember(declaredTopLevelType);

            CompiledMemberGroup rootMemberGroup = compiledConfig
                    .matchRootGroup(topLevelTypeDescriptor)
                    .orElseThrow(() ->
                            new IllegalStateException("No matching root member group for top-level type: qualifiedName="
                                    + declaredTopLevelType.getQualifiedName()
                                    + ", descriptor=" + topLevelTypeDescriptor));

            sortTypeRecursively(rootMemberGroup, declaredTopLevelType);
        });
    }

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
     * Depth-first recursion:
     * 1) process nested types,
     * 2) sort current type members,
     * 3) apply group boundary markers and flatten.
     *
     * This order keeps the logic deterministic and ensures nested types are already "clean"
     * when the outer type is printed.
     */
    private static void sortTypeRecursively(CompiledMemberGroup rootMemberGroup, CtType<?> currentType) {
        currentType.getNestedTypes().forEach(nestedType -> sortTypeRecursively(rootMemberGroup, nestedType));

        Map<CtTypeMember, MemberDescriptor> typeMember2Descriptor =
                SpoonMemberDescriptorFactory.describeMembers(currentType);

        Map<CtTypeMember, CompiledMemberGroup> naturalGroupByMember =
                NaturalMemberGroupResolver.resolveNaturalGroups(rootMemberGroup, typeMember2Descriptor);

        MemberDependencyGraph memberDependencyGraph =
                MemberDependencyGraphBuilder.buildDependencyGraph(currentType, naturalGroupByMember);

        Map<CtTypeMember, CompiledMemberGroup> effectiveGroupByMember =
                EffectiveMemberGroupResolver.resolveEffectiveGroups(naturalGroupByMember, memberDependencyGraph);

        List<MemberGroupBlock> memberGroupBlocks =
                TypeMemberGrouper.groupMembersByEffectiveGroups(effectiveGroupByMember);

        // TODO Check it
        List<MemberGroupBlock> orderedMemberGroupBlocks =
                GroupMembersOrderer.orderMembersInsideGroups(memberGroupBlocks, memberDependencyGraph);

        // TODO Check it
        GroupBoundaryMarker.markGroupBoundaries(orderedMemberGroupBlocks);

        List<CtTypeMember> flattenedSortedMembers = flattenMembers(orderedMemberGroupBlocks);

        // Apply to Spoon model.
        currentType.setTypeMembers(flattenedSortedMembers);
    }
}
