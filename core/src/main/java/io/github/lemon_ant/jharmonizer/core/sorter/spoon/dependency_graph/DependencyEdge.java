package io.github.lemon_ant.jharmonizer.core.sorter.spoon.dependency_graph;

import lombok.NonNull;
import lombok.Value;
import spoon.reflect.declaration.CtTypeMember;

@Value
class DependencyEdge {
    @NonNull
    CtTypeMember dependentMember;

    @NonNull
    MemberDependencyEdgeKind edgeKind;
}
