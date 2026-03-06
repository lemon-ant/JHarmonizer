package io.github.lemon_ant.jharmonizer.core.sorter.spoon.dependency_graph;

import static io.github.lemon_ant.jharmonizer.core.sorter.spoon.dependency_graph.OrderDependentFieldReferenceUtils.requireSourceStart;

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

    @NonNull
    @Override
    public Set<MemberDependencyArc> findDirectProviderEdges(
            @NonNull CtTypeMember dependentMember, boolean keepAccessorsTogether) {

        Optional<CtElement> dependentInitializationAstRoot =
                InitializationOrderDependencyUtils.resolveInitializationAstRoot(dependentMember);
        if (dependentInitializationAstRoot.isEmpty()) {
            return Set.of();
        }

        Set<CtField<?>> blankFinalFieldsReadByDependentMember =
                OrderDependentFieldReferenceUtils.findFieldsReadByMember(dependentMember, dependentInitializationAstRoot.get())
                        .stream()
                        .filter(InitializationOrderDependencyUtils::isBlankFinalField)
                        .collect(Collectors.toUnmodifiableSet());

        if (blankFinalFieldsReadByDependentMember.isEmpty()) {
            return Set.of();
        }

        int dependentSourceStart = requireSourceStart(dependentMember);

        return blankFinalFieldsReadByDependentMember.stream()
                .flatMap(blankFinalField -> InitializationOrderDependencyUtils.resolveProviderMembersForBlankFinalRead(
                        dependentMember, blankFinalField, dependentSourceStart)
                        .stream())
                .map(providerMember ->
                        new MemberDependencyArc(providerMember, MemberDependencyEdgeKind.DECLARATION_DEPENDENCY))
                .collect(Collectors.toUnmodifiableSet());
    }
}
