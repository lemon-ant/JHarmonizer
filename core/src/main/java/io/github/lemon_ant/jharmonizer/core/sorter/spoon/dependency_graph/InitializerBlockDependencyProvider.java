package io.github.lemon_ant.jharmonizer.core.sorter.spoon.dependency_graph;

import java.util.Optional;
import lombok.NonNull;
import spoon.reflect.declaration.CtAnonymousExecutable;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.declaration.CtTypeMember;

/**
 * Provides declaration dependency edges caused by initializer blocks (static / instance).
 *
 * <p>If init-block references fieldA, then fieldA -> init-block.
 */
final class InitializerBlockDependencyProvider extends AbstractReferencedFieldsDeclarationDependencyProvider {

    /**
     * Resolves the dependent ast root.
     * @param dependentMember the dependent member
     * @return the dependent ast root
     */
    @NonNull
    @Override
    protected Optional<CtElement> resolveDependentAstRoot(@NonNull CtTypeMember dependentMember) {
        if (!(dependentMember instanceof CtAnonymousExecutable dependentInitializerBlock)) {
            return Optional.empty();
        }

        return Optional.ofNullable(dependentInitializerBlock.getBody());
    }
}
