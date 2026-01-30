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

    @NonNull
    Set<@NonNull MemberDependencyArc> findDirectProviderEdges(
            @NonNull CtTypeMember dependentMember, boolean keepAccessorsTogether);
}
