package io.github.lemon_ant.jharmonizer.core.sorter.spoon.dependency_graph;

import java.util.Set;
import lombok.NonNull;
import spoon.reflect.declaration.CtTypeMember;

/**
 * Contributes dependency edges to the graph.
 *
 * <p>Contract: for the given dependent member, returns direct provider edges.
 * The graph builder will add them as {@code provider -> dependent}.
 */
@SuppressWarnings("PMD.ImplicitFunctionalInterface")
interface MemberDependencyProvider {

    /**
     * Finds direct provider edges for the dependent member.
     *
     * @param dependentMember the dependent member to inspect
     * @param config the detector configuration controlling accessor bundling and forward-reference strictness
     * @return the direct provider edges for the member
     */
    @NonNull
    Set<@NonNull MemberDependencyArc> findDirectProviderEdges(
            @NonNull CtTypeMember dependentMember, @NonNull DependencyDetectorConfig config);
}
