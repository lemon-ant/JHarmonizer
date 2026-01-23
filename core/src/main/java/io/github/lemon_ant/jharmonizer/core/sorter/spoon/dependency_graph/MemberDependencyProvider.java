package io.github.lemon_ant.jharmonizer.core.sorter.spoon.dependency_graph;

import java.util.Set;
import lombok.NonNull;
import spoon.reflect.declaration.CtTypeMember;

/**
 * Contributes dependency edges to the graph.
 */
@SuppressWarnings("PMD.ImplicitFunctionalInterface")
interface MemberDependencyProvider {

    /**
     * Returns direct dependency edges for the given dependent member.
     *
     * <p>Each returned edge is expected to be directed from provider to dependent.
     * Edge kind specifies whether the edge represents declaration dependency or just accessor bundling.
     */
    @NonNull
    Set<@NonNull DependencyEdge> findDirectEdges(@NonNull CtTypeMember providerMember, boolean keepAccessorsTogether);
}
