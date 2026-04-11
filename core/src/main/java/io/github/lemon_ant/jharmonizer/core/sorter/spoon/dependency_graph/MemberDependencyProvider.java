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
     * @param keepAccessorsTogether whether accessor bundles should be preserved
     * @param relaxedForwardReferences whether forward references to fields declared later in source order
     *     are ignored for dependency resolution
     * @return the direct provider edges for the member
     */
    @NonNull
    Set<@NonNull MemberDependencyArc> findDirectProviderEdges(
            @NonNull CtTypeMember dependentMember, boolean keepAccessorsTogether, boolean relaxedForwardReferences);
}
