package io.github.lemon_ant.jharmonizer.core.sorter.spoon.dependency_graph;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import lombok.NonNull;
import lombok.experimental.UtilityClass;
import spoon.reflect.code.CtExecutableReferenceExpression;
import spoon.reflect.code.CtFieldAccess;
import spoon.reflect.code.CtFieldRead;
import spoon.reflect.code.CtFieldWrite;
import spoon.reflect.code.CtLambda;
import spoon.reflect.cu.SourcePosition;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.declaration.CtField;
import spoon.reflect.declaration.CtType;
import spoon.reflect.declaration.CtTypeMember;
import spoon.reflect.reference.CtFieldReference;
import spoon.reflect.visitor.filter.TypeFilter;

@UtilityClass
// TODO Rename it
final class OrderDependentFieldReferenceUtils {

    static CtType<?> requireDeclaringType(@NonNull CtTypeMember typeMember) {
        CtType<?> declaringType = typeMember.getDeclaringType();
        if (declaringType != null) {
            return declaringType;
        }

        SourcePosition memberPosition = typeMember.getPosition();

        throw new IllegalStateException(
                "Expected type member to have declaring type (member must come from CtType.getTypeMembers()). "
                        + "typeMember=" + typeMember.getShortRepresentation()
                        + ", position=" + memberPosition);
    }

    /**
     * TODO: Reduce over-conservative ordering constraints for initializer dependencies.
     * <p>
     * Current approach treats field accesses found within initializer-like AST roots as potentially order-sensitive and
     * creates {@code DECLARATION_DEPENDENCY} edges. We already apply a source-order filter and only keep edges where the
     * provider member is declared above the dependent member in the original source (strict: missing/invalid positions
     * are treated as an error).
     * <p>
     * This is safe but may still create unnecessary edges and reduce sorting freedom (including occasional artificial
     * cycles) because some field accesses are "lazy" and do not execute during initialization.
     * <p>
     * Follow-up ideas (JLS-driven):
     * <ul>
     *   <li>Model "illegal forward reference" more precisely:
     *     <ul>
     *       <li>Focus on simple-name accesses (unqualified), not qualified ones ({@code this.field} / {@code TypeName.field}).</li>
     *       <li>Apply only in initializer contexts (field initializers, init blocks, enum constant initializers,
     *           annotation default values).</li>
     *     </ul>
     *   </li>
     *   <li>Ignore lazy/external execution contexts when collecting field accesses (to avoid false edges):
     *     <ul>
     *       <li>Do not traverse into lambdas and method references.</li>
     *       <li>Do not traverse into anonymous/local/nested type bodies declared inside initializers.</li>
     *     </ul>
     *   </li>
     *   <li>Consider exceptions for compile-time constants:
     *     <ul>
     *       <li>References to constant variables may not need ordering constraints.</li>
     *     </ul>
     *   </li>
     *   <li>Consider write-vs-read semantics:
     *     <ul>
     *       <li>Writes/assignments (LHS) should not be treated as reads that require provider-before-dependent edges.</li>
     *     </ul>
     *   </li>
     * </ul>
     * <p>
     * Goal: fewer artificial edges (still correct), better flexibility for grouping/sorting.
     */
    static Set<CtField<?>> findReferencedFields(
            @NonNull CtTypeMember dependentMember, @NonNull CtElement dependentAstRoot) {
        CtType<?> declaringType = requireDeclaringType(dependentMember);
        int dependentSourceStart = requireSourceStart(dependentMember);

        return collectReferencedDeclaringTypeFields(
                dependentAstRoot,
                declaringType,
                CtFieldAccess.class,
                // TODO Reconsider to use shouldCreateDeclarationDependencyEdge for each case
                referencedField -> shouldCreateDeclarationDependencyEdge(referencedField, dependentSourceStart));
    }

    static Set<CtField<?>> findReadFields(@NonNull CtTypeMember dependentMember, @NonNull CtElement astRoot) {
        CtType<?> declaringType = requireDeclaringType(dependentMember);
        return collectReferencedDeclaringTypeFields(astRoot, declaringType, CtFieldRead.class, referencedField -> true);
    }

    static Set<CtField<?>> findWrittenFields(@NonNull CtTypeMember dependentMember, @NonNull CtElement astRoot) {
        CtType<?> declaringType = requireDeclaringType(dependentMember);
        return collectReferencedDeclaringTypeFields(
                astRoot, declaringType, CtFieldWrite.class, referencedField -> true);
    }

    // TODO Rename it
    private static boolean shouldCreateDeclarationDependencyEdge(
            CtTypeMember providerMember, int dependentSourceStart) {
        int providerSourceStart = requireSourceStart(providerMember);
        return providerSourceStart < dependentSourceStart;
    }

    static int requireSourceStart(CtTypeMember typeMember) {
        SourcePosition memberPosition = typeMember.getPosition();
        if (memberPosition != null && memberPosition.isValidPosition()) {
            return memberPosition.getSourceStart();
        }

        throw new IllegalStateException(
                "Expected type member to have a valid SourcePosition (member must come from parsed source and "
                        + "CtType.getTypeMembers()). "
                        + "typeMember=" + typeMember.getShortRepresentation()
                        + ", position=" + memberPosition);
    }

    @SuppressWarnings("PMD.CompareObjectsWithEquals")
    private static <T extends CtFieldAccess<?>> Set<CtField<?>> collectReferencedDeclaringTypeFields(
            CtElement dependentAstRoot,
            CtType<?> declaringType,
            Class<T> fieldAccessClass,
            Predicate<CtField<?>> additionalFieldFilter) {
        TypeFilter<T> fieldAccessTypeFilter = new TypeFilter<>(fieldAccessClass);
        List<T> dependentAstRootElements = dependentAstRoot.getElements(fieldAccessTypeFilter);
        return dependentAstRootElements.stream()
                .filter(fieldAccess -> !isInsideLazyContext(declaringType, dependentAstRoot, fieldAccess))
                .map(CtFieldAccess::getVariable)
                .map(CtFieldReference::getDeclaration)
                .filter(Objects::nonNull)
                .filter(referencedField -> referencedField.getDeclaringType() == declaringType)
                .filter(additionalFieldFilter)
                .collect(Collectors.toUnmodifiableSet());
    }

    @SuppressWarnings("PMD.CompareObjectsWithEquals")
    private static boolean isInsideLazyContext(CtType<?> declaringType, CtElement astRoot, CtElement element) {
        CtElement currentParent = element.getParent();

        while (currentParent != null) {
            if (currentParent instanceof CtLambda<?>) {
                return true;
            }

            if (currentParent instanceof CtExecutableReferenceExpression<?, ?>) {
                return true;
            }

            if (currentParent instanceof CtType<?> parentType && parentType != declaringType) {
                return true;
            }

            if (currentParent == astRoot) {
                return false;
            }

            currentParent = currentParent.getParent();
        }

        return false;
    }
}
