package io.github.lemon_ant.jharmonizer.core.sorter.spoon.dependency_graph;

import io.github.lemon_ant.jharmonizer.core.config.compiled.CompiledMemberGroup;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import lombok.NonNull;
import lombok.experimental.UtilityClass;
import spoon.reflect.declaration.CtType;
import spoon.reflect.declaration.CtTypeMember;

/**
 * Builds a directed dependency graph between members of a single {@link CtType}.
 */
@UtilityClass
public class MemberDependencyGraphBuilder {

    private static final Set<MemberDependencyEdgeKind> ACCESSOR_BUNDLE_ONLY =
            EnumSet.of(MemberDependencyEdgeKind.ACCESSOR_BUNDLE);
    private static final Set<MemberDependencyEdgeKind> DECLARATION_DEPENDENCY_ONLY =
            EnumSet.of(MemberDependencyEdgeKind.DECLARATION_DEPENDENCY);

    private static final Collection<@NonNull MemberDependencyProvider> memberDependencyProviders = List.of(
            new AccessorPairDependencyProvider(),
            new FieldInitializerDependencyProvider(),
            new InitializerBlockDependencyProvider());

    @NonNull
    public static MemberDependencyGraph buildDependencyGraph(
            @NonNull CtType<?> type, @NonNull Map<CtTypeMember, CompiledMemberGroup> typeMember2NaturalMemberGroup) {

        MemberDependencyGraph memberDependencyGraph = new MemberDependencyGraph();

        type.getTypeMembers().forEach(providerMember -> {
            CompiledMemberGroup providerNaturalGroup =
                    resolveNaturalGroupOrThrow(providerMember, typeMember2NaturalMemberGroup);

            boolean keepAccessorsTogether = providerNaturalGroup.isKeepAccessorsTogether();

            memberDependencyProviders.stream()
                    .flatMap(memberDependencyProvider ->
                            memberDependencyProvider.findDirectEdges(providerMember, keepAccessorsTogether).stream())
                    .forEach(directEdge -> memberDependencyGraph.addEdge(
                            providerMember, directEdge.getDependentMember(), directEdge.getEdgeKind()));
        });

        return memberDependencyGraph;
    }

    private static CompiledMemberGroup resolveNaturalGroupOrThrow(
            CtTypeMember dependentMember, Map<CtTypeMember, CompiledMemberGroup> typeMember2CompiledMemberGroup) {

        return Optional.ofNullable(typeMember2CompiledMemberGroup.get(dependentMember))
                .orElseThrow(() -> new IllegalStateException("Natural group was not resolved for type member. "
                        + "Expected typeMember2CompiledMemberGroup to contain all CtType.getTypeMembers(). "
                        + "Missing member: "
                        + dependentMember));
    }
}
