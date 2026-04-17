package io.github.lemon_ant.jharmonizer.core.sorter.spoon.dependency_graph;

import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.NonNull;
import spoon.reflect.code.CtFieldRead;
import spoon.reflect.code.CtLambda;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.declaration.CtEnumValue;
import spoon.reflect.declaration.CtField;
import spoon.reflect.declaration.CtType;
import spoon.reflect.declaration.CtTypeMember;
import spoon.reflect.declaration.ModifierKind;
import spoon.reflect.visitor.filter.TypeFilter;

/**
 * Provides declaration-order dependencies caused by cross-type constant back-references.
 *
 * <p>When a field {@code F} in type {@code T} accesses a field from a resolvable type {@code E1}
 * (present in the same compilation unit), and a transitive initializer chain from {@code E1}
 * eventually reads fields of {@code T}, then {@code F} depends on those fields of {@code T}.
 * Accessing the cross-type field triggers {@code E1}'s static initialization, which may trigger
 * further static initializations, eventually reading fields of {@code T}. If those fields are not
 * yet initialized when the chain runs, an {@link ExceptionInInitializerError} occurs at runtime.
 *
 * <p>This provider handles chains of arbitrary depth: {@code T.F → E1.SF1 → E2.SF2 → … → T.G}.
 * Cross-file type pairs are intentionally out of scope — a cross-file circular static initializer
 * dependency is a design problem that should be fixed by redesigning the application, not worked
 * around by a harmonization tool.
 */
final class CrossTypeConstantBackRefDependencyProvider implements MemberDependencyProvider {

    /**
     * Finds the direct provider edges.
     *
     * @param dependentMember the type member whose initialization-order dependencies are being
     *     analyzed
     * @param keepAccessorsTogether whether to treat getter/setter pairs as a unit when analyzing
     *     dependencies
     * @return the matching direct provider edges
     */
    @NonNull
    @Override
    public Set<@NonNull MemberDependencyArc> findDirectProviderEdges(
            @NonNull CtTypeMember dependentMember, boolean keepAccessorsTogether) {
        if (!(dependentMember instanceof CtField<?> dependentField)) {
            return Set.of();
        }

        if (!dependentField.hasModifier(ModifierKind.STATIC)) {
            return Set.of();
        }

        CtElement dependentInitializerAst = dependentField.getDefaultExpression();
        if (dependentInitializerAst == null) {
            return Set.of();
        }

        CtType<?> declaringType = DeclaringTypeFieldReferenceUtils.requireDeclaringType(dependentMember);
        Set<CtType<?>> visitedTypes = Collections.newSetFromMap(new IdentityHashMap<>());
        visitedTypes.add(declaringType);

        return collectProviderFieldsReachableViaBackRef(dependentInitializerAst, declaringType, visitedTypes)
                .map(providerField ->
                        new MemberDependencyArc(providerField, MemberDependencyEdgeKind.DECLARATION_DEPENDENCY))
                .collect(Collectors.toUnmodifiableSet());
    }

    @NonNull
    private static Stream<CtField<?>> collectProviderFieldsReachableViaBackRef(
            CtElement expr, CtType<?> declaringType, Set<CtType<?>> visitedTypes) {
        List<CtField<?>> crossTypeFieldCandidates = expr.getElements(new TypeFilter<>(CtFieldRead.class)).stream()
                .filter(fieldRead -> isDirectlyEvaluatedDuringInit(expr, fieldRead))
                .flatMap(
                        fieldRead -> Optional.ofNullable(fieldRead.getVariable().getDeclaration()).stream())
                .filter(fieldDecl -> fieldDecl instanceof CtField<?>)
                .<CtField<?>>map(fieldDecl -> fieldDecl)
                .toList();

        Set<CtField<?>> providerFields = new HashSet<>();
        for (CtField<?> crossTypeField : crossTypeFieldCandidates) {
            CtType<?> crossType = crossTypeField.getDeclaringType();
            if (crossType == null || visitedTypes.contains(crossType)) {
                continue;
            }

            if (!crossTypeField.hasModifier(ModifierKind.STATIC)) {
                continue;
            }

            if (InitializationOrderDependencyUtils.isStaticCompileTimeConstantVariable(crossTypeField)) {
                continue;
            }

            CtElement crossTypeInitExpr = crossTypeField.getDefaultExpression();
            if (crossTypeInitExpr == null) {
                continue;
            }

            crossTypeInitExpr.getElements(new TypeFilter<>(CtFieldRead.class)).stream()
                    .filter(backRefRead -> isDirectlyEvaluatedDuringInit(crossTypeInitExpr, backRefRead))
                    .flatMap(backRefRead ->
                            Optional.ofNullable(backRefRead.getVariable().getDeclaration()).stream())
                    .filter(field -> !(field instanceof CtEnumValue<?>))
                    .filter(field -> field instanceof CtField<?>)
                    .<CtField<?>>map(field -> field)
                    .filter(field -> isFieldDeclaredInType(field, declaringType))
                    .filter(field -> field.hasModifier(ModifierKind.STATIC))
                    .filter(field -> !InitializationOrderDependencyUtils.isStaticCompileTimeConstantVariable(field))
                    .forEach(providerFields::add);

            visitedTypes.add(crossType);
            collectProviderFieldsReachableViaBackRef(crossTypeInitExpr, declaringType, visitedTypes)
                    .forEach(providerFields::add);
        }

        return providerFields.stream();
    }

    /**
     * Returns {@code true} when {@code element} is directly evaluated during class initialization
     * relative to {@code initAstRoot}, meaning there is no lambda body or nested-type body between
     * {@code element} and {@code initAstRoot} in the parent chain.
     *
     * <p>Field reads inside lambda bodies are not evaluated when the lambda is created during
     * {@code <clinit>}; they run only when the lambda is eventually called. Reads inside anonymous or
     * inner class method bodies are similarly deferred and do not participate in {@code <clinit>}
     * ordering.
     */
    @SuppressWarnings("PMD.CompareObjectsWithEquals")
    private static boolean isDirectlyEvaluatedDuringInit(CtElement initAstRoot, CtElement element) {
        if (element == initAstRoot) {
            return true;
        }
        CtElement currentParent = element.getParent();
        while (currentParent != null) {
            if (currentParent instanceof CtLambda<?>) {
                return false;
            }
            if (currentParent instanceof CtType<?>) {
                return false;
            }
            if (currentParent == initAstRoot) {
                return true;
            }
            currentParent = currentParent.getParent();
        }
        return false;
    }

    @SuppressWarnings("PMD.CompareObjectsWithEquals")
    private static boolean isFieldDeclaredInType(CtField<?> field, CtType<?> declaringType) {
        return field.getDeclaringType() == declaringType;
    }
}
