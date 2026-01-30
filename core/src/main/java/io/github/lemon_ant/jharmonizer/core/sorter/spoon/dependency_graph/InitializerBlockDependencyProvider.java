package io.github.lemon_ant.jharmonizer.core.sorter.spoon.dependency_graph;

import static io.github.lemon_ant.jharmonizer.core.sorter.spoon.dependency_graph.OrderDependentFieldReferenceUtils.requireDeclaringType;

import java.util.Set;
import java.util.stream.Collectors;
import lombok.NonNull;
import spoon.reflect.declaration.CtAnonymousExecutable;
import spoon.reflect.declaration.CtType;
import spoon.reflect.declaration.CtTypeMember;

/**
 * Provides declaration dependency edges caused by initializer blocks (static / instance).
 *
 * <p>If init-block references fieldA, then fieldA -> init-block.
 */
final class InitializerBlockDependencyProvider implements MemberDependencyProvider {

    @NonNull
    @Override
    public Set<@NonNull MemberDependencyArc> findDirectProviderEdges(
            @NonNull CtTypeMember dependentMember, boolean keepAccessorsTogether) {
        if (!(dependentMember instanceof CtAnonymousExecutable dependentInitializerBlock)) {
            return Set.of();
        }

        if (dependentInitializerBlock.getBody() == null) {
            return Set.of();
        }

        CtType<?> declaringType = requireDeclaringType(dependentInitializerBlock);

        return OrderDependentFieldReferenceUtils.findReferencedFields(
                        dependentInitializerBlock.getBody(), declaringType)
                .stream()
                .map(referencedField ->
                        new MemberDependencyArc(referencedField, MemberDependencyEdgeKind.DECLARATION_DEPENDENCY))
                .collect(Collectors.toUnmodifiableSet());
    }
}
