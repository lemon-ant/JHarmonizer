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

    /**
     * Finds the direct provider edges.
     * @param dependentMember the dependent member
     * @param config the detector configuration
     * @return the matching direct provider edges
     */
    @NonNull
    @Override
    public Set<@NonNull MemberDependencyArc> findDirectProviderEdges(
            @NonNull CtTypeMember dependentMember, @NonNull MemberDependencyProvider.ProviderConfig config) {

        if (!config.isKeepAccessorsTogether() || !(dependentMember instanceof CtMethod<?> dependentMethod)) {
            return Set.of();
        }

        return SpoonJavaBeansAccessorUtils.findPairedAccessorMethods(dependentMethod).stream()
                .map(pairedAccessorMethod ->
                        new MemberDependencyArc(pairedAccessorMethod, MemberDependencyEdgeKind.ACCESSOR_BUNDLE))
                .collect(Collectors.toUnmodifiableSet());
    }
}
