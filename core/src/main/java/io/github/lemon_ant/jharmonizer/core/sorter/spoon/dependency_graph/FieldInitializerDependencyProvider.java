package io.github.lemon_ant.jharmonizer.core.sorter.spoon.dependency_graph;

import static io.github.lemon_ant.jharmonizer.core.sorter.spoon.dependency_graph.OrderDependentFieldReferenceUtils.requireDeclaringType;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.stream.Collectors;
import lombok.NonNull;
import spoon.reflect.declaration.CtField;
import spoon.reflect.declaration.CtType;
import spoon.reflect.declaration.CtTypeMember;

final class FieldInitializerDependencyProvider implements MemberDependencyProvider {

    private final Map<CtType<?>, Map<CtTypeMember, Set<DependencyEdge>>> outgoingEdgesByProviderMemberByType =
            new WeakHashMap<>();

    @NonNull
    @Override
    public Set<DependencyEdge> findDirectEdges(@NonNull CtTypeMember providerMember, boolean keepAccessorsTogether) {
        if (!(providerMember instanceof CtField<?> providerField)) {
            return Set.of();
        }

        CtType<?> declaringType = requireDeclaringType(providerField);

        Map<CtTypeMember, Set<DependencyEdge>> outgoingEdgesByProviderMember =
                outgoingEdgesByProviderMemberByType.computeIfAbsent(
                        declaringType, FieldInitializerDependencyProvider::buildOutgoingEdgesByProviderMember);

        return outgoingEdgesByProviderMember.getOrDefault(providerMember, Set.of());
    }

    private static Map<CtTypeMember, Set<DependencyEdge>> buildOutgoingEdgesByProviderMember(
            @NonNull CtType<?> declaringType) {
        Map<CtTypeMember, Set<DependencyEdge>> mutableOutgoingEdgesByProviderMember = new HashMap<>();

        for (CtTypeMember typeMember : declaringType.getTypeMembers()) {
            if (!(typeMember instanceof CtField<?> dependentField)) {
                continue;
            }

            if (dependentField.getDefaultExpression() == null) {
                continue;
            }

            Set<CtField<?>> referencedFields = OrderDependentFieldReferenceUtils.findReferencedFields(
                    dependentField.getDefaultExpression(), declaringType);

            for (CtField<?> referencedField : referencedFields) {
                if (OrderDependentFieldReferenceUtils.shouldCreateDeclarationDependencyEdge(
                        referencedField, dependentField)) {
                    continue;
                }

                mutableOutgoingEdgesByProviderMember
                        .computeIfAbsent(referencedField, ignored -> new HashSet<>())
                        .add(new DependencyEdge(dependentField, MemberDependencyEdgeKind.DECLARATION_DEPENDENCY));
            }
        }

        return mutableOutgoingEdgesByProviderMember.entrySet().stream()
                .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, entry -> Set.copyOf(entry.getValue())));
    }
}
