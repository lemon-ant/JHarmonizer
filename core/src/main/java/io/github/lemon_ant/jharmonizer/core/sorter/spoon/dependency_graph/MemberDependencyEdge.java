package io.github.lemon_ant.jharmonizer.core.sorter.spoon.dependency_graph;

import lombok.NonNull;
import lombok.Value;
import spoon.reflect.declaration.CtTypeMember;

/**
 * A directed edge in the member dependency graph.
 *
 * <p>Interpretation depends on {@link MemberDependencyEdgeKind}:
 * <ul>
 *   <li>{@link MemberDependencyEdgeKind#DECLARATION_DEPENDENCY}: provider must not appear later than dependent.</li>
 *   <li>{@link MemberDependencyEdgeKind#ACCESSOR_BUNDLE}: keep accessors in the same effective group; ignore for ordering.</li>
 * </ul>
 */
@Value
class MemberDependencyEdge {
    @NonNull
    CtTypeMember providerMember;

    @NonNull
    CtTypeMember dependentMember;

    @NonNull
    MemberDependencyEdgeKind edgeKind;
}
