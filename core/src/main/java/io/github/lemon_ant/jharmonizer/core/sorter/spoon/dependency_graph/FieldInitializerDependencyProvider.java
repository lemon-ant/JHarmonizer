package io.github.lemon_ant.jharmonizer.core.sorter.spoon.dependency_graph;

import static io.github.lemon_ant.jharmonizer.core.sorter.spoon.dependency_graph.OrderDependentFieldReferenceUtils.requireSourceStart;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.NonNull;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.declaration.CtField;
import spoon.reflect.declaration.CtTypeMember;

/**
 * Provides declaration dependency edges caused by field initializers.
 *
 * <p>If fieldB initializer references fieldA, then fieldA -> fieldB.
 */
final class FieldInitializerDependencyProvider extends AbstractReferencedFieldsDeclarationDependencyProvider {

    @NonNull
    @Override
    public Set<@NonNull MemberDependencyArc> findDirectProviderEdges(
            @NonNull CtTypeMember dependentMember, boolean keepAccessorsTogether) {
        if (!(dependentMember instanceof CtField<?> dependentField)) {
            return Set.of();
        }

        Optional<CtElement> dependentAstRoot = resolveDependentAstRoot(dependentMember);
        if (dependentAstRoot.isEmpty()) {
            return Set.of();
        }

        Set<CtTypeMember> providers = new HashSet<>(
                OrderDependentFieldReferenceUtils.findReferencedFields(dependentMember, dependentAstRoot.get()));
        providers.addAll(findEarlierThisQualifiedReferrers(dependentField));

        return providers.stream()
                .map(providerMember ->
                        new MemberDependencyArc(providerMember, MemberDependencyEdgeKind.DECLARATION_DEPENDENCY))
                .collect(Collectors.toUnmodifiableSet());
    }

    @NonNull
    @Override
    protected Optional<CtElement> resolveDependentAstRoot(@NonNull CtTypeMember dependentMember) {
        if (!(dependentMember instanceof CtField<?> dependentField)) {
            return Optional.empty();
        }

        return Optional.ofNullable(dependentField.getDefaultExpression());
    }

    private static Set<CtTypeMember> findEarlierThisQualifiedReferrers(CtField<?> dependentField) {
        int dependentFieldSourceStart = requireSourceStart(dependentField);

        return dependentField.getDeclaringType().getTypeMembers().stream()
                .filter(typeMember -> typeMember instanceof CtField<?>)
                .filter(typeMember -> requireSourceStart(typeMember) < dependentFieldSourceStart)
                .map(typeMember -> (CtField<?>) typeMember)
                .filter(field -> field.getDefaultExpression() != null)
                .filter(field -> OrderDependentFieldReferenceUtils.findExplicitThisReferencedFields(
                                field, field.getDefaultExpression())
                        .contains(dependentField))
                .map(field -> (CtTypeMember) field)
                .collect(Collectors.toUnmodifiableSet());
    }
}
