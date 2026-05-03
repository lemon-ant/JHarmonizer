/*
 * SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 */
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
     * Resolves the initializer-block initialization AST that should be scanned for field references.
     * @param dependentMember the dependent member
     * @return the initializer-block body when the member is an initializer block
     */
    @NonNull
    @Override
    protected Optional<CtElement> resolveDependentInitializationAst(@NonNull CtTypeMember dependentMember) {
        if (!(dependentMember instanceof CtAnonymousExecutable dependentInitializerBlock)) {
            return Optional.empty();
        }

        return Optional.ofNullable(dependentInitializerBlock.getBody());
    }
}
