package io.github.lemon_ant.jharmonizer.core.sorter.spoon.dependency_graph;

import static io.github.lemon_ant.jharmonizer.core.sorter.spoon.dependency_graph.OrderDependentFieldReferenceUtils.requireSourceStart;

import java.util.Set;
import java.util.stream.Collectors;
import lombok.NonNull;
import spoon.reflect.declaration.CtField;
import spoon.reflect.declaration.CtTypeMember;

/**
 * Provides declaration dependencies created by explicit {@code this.<field>} references in field initializers.
 */
final class ExplicitThisInitializerFieldDependencyProvider implements MemberDependencyProvider {

    @NonNull
    @Override
    public Set<@NonNull MemberDependencyArc> findDirectProviderEdges(
            @NonNull CtTypeMember dependentMember, boolean keepAccessorsTogether) {
        if (!(dependentMember instanceof CtField<?> referencedField)) {
            return Set.of();
        }

        return findEarlierFieldsWithExplicitThisReferenceTo(referencedField).stream()
                .map(providerMember ->
                        new MemberDependencyArc(providerMember, MemberDependencyEdgeKind.DECLARATION_DEPENDENCY))
                .collect(Collectors.toUnmodifiableSet());
    }

    private static Set<CtTypeMember> findEarlierFieldsWithExplicitThisReferenceTo(@NonNull CtField<?> referencedField) {
        int referencedFieldSourceStart = requireSourceStart(referencedField);

        return referencedField.getDeclaringType().getTypeMembers().stream()
                .filter(typeMember -> typeMember instanceof CtField<?>)
                .filter(typeMember -> requireSourceStart(typeMember) < referencedFieldSourceStart)
                .map(typeMember -> (CtField<?>) typeMember)
                .filter(field -> field.getDefaultExpression() != null)
                .filter(field -> OrderDependentFieldReferenceUtils.hasExplicitThisReferenceTo(field, referencedField))
                .map(field -> (CtTypeMember) field)
                .collect(Collectors.toUnmodifiableSet());
    }
}
