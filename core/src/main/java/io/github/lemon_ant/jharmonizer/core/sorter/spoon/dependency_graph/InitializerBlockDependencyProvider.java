package io.github.lemon_ant.jharmonizer.core.sorter.spoon.dependency_graph;

import java.util.Set;
import lombok.NonNull;
import spoon.reflect.declaration.CtTypeMember;

/**
 * Placeholder provider for initializer blocks (static / instance).
 * <p>
 * Implementation note:
 * Spoon typically models initializer blocks as CtAnonymousExecutable, which should be mapped to:
 * - MemberKind.STATIC_INIT_BLOCK
 * - MemberKind.INSTANCE_INIT_BLOCK
 * <p>
 * This provider should add edges from referenced fields to the initializer block.
 */
final class InitializerBlockDependencyProvider implements MemberDependencyProvider {

    @Override
    @NonNull
    public Set<@NonNull MemberDependencyEdge> findDirectEdges(
            @NonNull CtTypeMember dependentMember, boolean keepAccessorsTogether) {

        // TODO Implement once CtAnonymousExecutable is integrated into MemberDescriptorFactory.
        return Set.of();
    }
}
