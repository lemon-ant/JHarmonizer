package io.github.lemon_ant.jharmonizer.core.sorter.spoon.dependency_graph;

import io.github.lemon_ant.jharmonizer.core.config.compiled.CompiledMemberGroup;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.NonNull;
import lombok.experimental.UtilityClass;
import spoon.reflect.declaration.CtType;
import spoon.reflect.declaration.CtTypeMember;

/**
 * Builds a directed dependency graph between members of a single {@link CtType}.
 */
@UtilityClass
// TODO Перепроверить
public class MemberDependencyGraphBuilder {

    @NonNull
    private static final Collection<@NonNull MemberDependencyProvider> memberDependencyProviders = List.of(
            new AccessorPairDependencyProvider(),
            new FieldInitializerDependencyProvider(),
            new InitializerBlockDependencyProvider());

    @NonNull
    public static MemberDependencyGraph buildDependencyGraph(
            @NonNull CtType<?> type, @NonNull Map<CtTypeMember, CompiledMemberGroup> typeMember2CompiledMemberGroup) {

        MemberDependencyGraph memberDependencyGraph = new MemberDependencyGraph();

        type.getTypeMembers().stream()
                .flatMap(dependentMember -> {
                    CompiledMemberGroup dependentNaturalGroup =
                            resolveNaturalGroupOrThrow(dependentMember, typeMember2CompiledMemberGroup);

                    boolean keepAccessorsTogether = dependentNaturalGroup.isKeepAccessorsTogether();

                    return memberDependencyProviders.stream()
                            .flatMap(memberDependencyProvider ->
                                    memberDependencyProvider
                                            .findDirectEdges(dependentMember, keepAccessorsTogether)
                                            .stream());
                })
                .forEach(memberDependencyGraph::addEdge);

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
