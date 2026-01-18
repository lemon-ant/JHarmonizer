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
    public Set<@NonNull DependencyEdge> findDirectEdges(
            @NonNull CtTypeMember providerMember, boolean keepAccessorsTogether) {

        if (!keepAccessorsTogether || !(providerMember instanceof CtMethod<?> providerMethod)) {
            return Set.of();
        }

        return SpoonJavaBeansAccessorUtils.findPairedAccessorMethods(providerMethod).stream()
                .map(pairedAccessorMethod ->
                        new DependencyEdge(pairedAccessorMethod, MemberDependencyEdgeKind.ACCESSOR_BUNDLE))
                .collect(Collectors.toUnmodifiableSet());
    }
}
