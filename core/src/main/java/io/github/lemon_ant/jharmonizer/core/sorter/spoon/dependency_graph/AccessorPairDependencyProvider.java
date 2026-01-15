package io.github.lemon_ant.jharmonizer.core.sorter.spoon.dependency_graph;

import java.util.Set;
import java.util.stream.Collectors;
import lombok.NonNull;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.declaration.CtTypeMember;

/**
 * Optional provider: keeps accessors together by adding {@link MemberDependencyEdgeKind#ACCESSOR_BUNDLE} edges.
 *
 * <p>These edges are intentionally not declaration-order constraints and must be ignored by topo-ordering.
 */
final class AccessorPairDependencyProvider implements MemberDependencyProvider {

    @NonNull
    @Override
    public Set<@NonNull MemberDependencyEdge> findDirectEdges(
            @NonNull CtTypeMember dependentMember, boolean keepAccessorsTogether) {

        if (!keepAccessorsTogether || !(dependentMember instanceof CtMethod<?> dependentMethod)) {
            return Set.of();
        }

        return SpoonJavaBeansAccessorUtils.findPairedAccessorMethods(dependentMethod).stream()
                .map(pairedAccessorMethod -> new MemberDependencyEdge(
                        pairedAccessorMethod, dependentMethod, MemberDependencyEdgeKind.ACCESSOR_BUNDLE))
                .collect(Collectors.toUnmodifiableSet());
    }
}
