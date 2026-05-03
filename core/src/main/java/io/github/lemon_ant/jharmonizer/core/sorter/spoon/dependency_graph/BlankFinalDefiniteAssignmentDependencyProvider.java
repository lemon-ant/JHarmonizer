/*
 * SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.lemon_ant.jharmonizer.core.sorter.spoon.dependency_graph;

import static io.github.lemon_ant.jharmonizer.core.sorter.spoon.dependency_graph.DeclaringTypeFieldReferenceUtils.requireSrcStart;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.NonNull;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.declaration.CtField;
import spoon.reflect.declaration.CtTypeMember;

/**
 * Provides declaration-order dependencies required to keep reads of blank final fields after their assignments.
 *
 * <p>Conservative approach: if a dependent initialization member reads a blank final field, we add edges from all
 * potential assignment providers declared above the dependent member in the original source order.
 */
final class BlankFinalDefiniteAssignmentDependencyProvider implements MemberDependencyProvider {

    /**
     * Finds the direct provider edges.
     * @param dependentMember the dependent member
     * @param providerConfig the detector configuration
     * @return the matching direct provider edges
     */
    @NonNull
    @Override
    public Set<MemberDependencyArc> findDirectProviderEdges(
            @NonNull CtTypeMember dependentMember, @NonNull MemberDependencyProvider.ProviderConfig providerConfig) {

        Optional<CtElement> dependentInitializationAstRoot =
                InitializationOrderDependencyUtils.resolveInitializationAstRoot(dependentMember);
        if (dependentInitializationAstRoot.isEmpty()) {
            return Set.of();
        }

        Set<CtField<?>> blankFinalFieldsReadByDependentMember =
                DeclaringTypeFieldReferenceUtils.findFieldsReadByMember(
                                dependentMember, dependentInitializationAstRoot.get())
                        .stream()
                        .filter(InitializationOrderDependencyUtils::isBlankFinalField)
                        .collect(Collectors.toUnmodifiableSet());

        if (blankFinalFieldsReadByDependentMember.isEmpty()) {
            return Set.of();
        }

        int dependentSrcStart = requireSrcStart(dependentMember);

        return blankFinalFieldsReadByDependentMember.stream()
                .flatMap(blankFinalField -> InitializationOrderDependencyUtils.resolveProviderMembersForBlankFinalRead(
                        dependentMember,
                        blankFinalField,
                        dependentSrcStart,
                        providerConfig.isRelaxedForwardReferences())
                        .stream())
                .map(providerMember ->
                        new MemberDependencyArc(providerMember, MemberDependencyEdgeKind.DECLARATION_DEPENDENCY))
                .collect(Collectors.toUnmodifiableSet());
    }
}
