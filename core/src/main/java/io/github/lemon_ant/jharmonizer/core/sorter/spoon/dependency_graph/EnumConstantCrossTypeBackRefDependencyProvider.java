package io.github.lemon_ant.jharmonizer.core.sorter.spoon.dependency_graph;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.NonNull;
import spoon.reflect.code.CtFieldRead;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.declaration.CtEnumValue;
import spoon.reflect.declaration.CtField;
import spoon.reflect.declaration.CtType;
import spoon.reflect.declaration.CtTypeMember;
import spoon.reflect.visitor.filter.TypeFilter;

/**
 * Provides declaration-order dependencies caused by cross-type enum constant back-references.
 *
 * <p>When a field {@code F} in type {@code T} accesses an enum constant from a resolvable type
 * {@code E} (present in the same compilation unit), and {@code E}'s enum constant initializer reads
 * fields of {@code T}, then {@code F} depends on those fields: accessing the enum constant triggers
 * {@code E}'s static initialization, which reads those fields of {@code T}. If those fields are not
 * yet initialized when {@code F} runs, an {@link ExceptionInInitializerError} occurs at runtime.
 */
final class EnumConstantCrossTypeBackRefDependencyProvider implements MemberDependencyProvider {

    /**
     * Finds the direct provider edges.
     *
     * @param dependentMember the dependent member
     * @param keepAccessorsTogether the keep accessors together
     * @return the matching direct provider edges
     */
    @NonNull
    @Override
    public Set<@NonNull MemberDependencyArc> findDirectProviderEdges(
            @NonNull CtTypeMember dependentMember, boolean keepAccessorsTogether) {
        if (!(dependentMember instanceof CtField<?> dependentField)) {
            return Set.of();
        }

        CtElement dependentInitializerAst = dependentField.getDefaultExpression();
        if (dependentInitializerAst == null) {
            return Set.of();
        }

        CtType<?> declaringType = DeclaringTypeFieldReferenceUtils.requireDeclaringType(dependentMember);
        return dependentInitializerAst.getElements(new TypeFilter<>(CtFieldRead.class)).stream()
                .flatMap(
                        fieldRead -> Optional.ofNullable(fieldRead.getVariable().getDeclaration()).stream())
                .filter(fieldDecl -> fieldDecl instanceof CtEnumValue<?>)
                .map(fieldDecl -> (CtEnumValue<?>) fieldDecl)
                .filter(enumValue -> isFromResolvableDifferentType(enumValue, declaringType))
                .map(CtEnumValue::getDefaultExpression)
                .filter(enumInitExpr -> enumInitExpr != null)
                .flatMap(enumInitExpr -> findDeclaringTypeFieldsReadIn(enumInitExpr, declaringType))
                .map(providerField ->
                        new MemberDependencyArc(providerField, MemberDependencyEdgeKind.DECLARATION_DEPENDENCY))
                .collect(Collectors.toUnmodifiableSet());
    }

    @SuppressWarnings("PMD.CompareObjectsWithEquals")
    private static boolean isFromResolvableDifferentType(CtEnumValue<?> enumValue, CtType<?> declaringType) {
        CtType<?> enumType = enumValue.getDeclaringType();
        return enumType != null && enumType != declaringType;
    }

    @NonNull
    private static Stream<CtField<?>> findDeclaringTypeFieldsReadIn(CtElement enumInitExpr, CtType<?> declaringType) {
        return enumInitExpr.getElements(new TypeFilter<>(CtFieldRead.class)).stream()
                .flatMap(
                        fieldRead -> Optional.ofNullable(fieldRead.getVariable().getDeclaration()).stream())
                .filter(fieldDecl -> !(fieldDecl instanceof CtEnumValue<?>))
                .filter(fieldDecl -> fieldDecl instanceof CtField<?>)
                .<CtField<?>>map(fieldDecl -> (CtField<?>) fieldDecl)
                .filter(field -> isFieldDeclaredInType(field, declaringType));
    }

    @SuppressWarnings("PMD.CompareObjectsWithEquals")
    private static boolean isFieldDeclaredInType(CtField<?> field, CtType<?> declaringType) {
        return field.getDeclaringType() == declaringType;
    }
}
