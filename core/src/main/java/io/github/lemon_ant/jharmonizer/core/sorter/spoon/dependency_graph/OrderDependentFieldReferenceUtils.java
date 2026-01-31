package io.github.lemon_ant.jharmonizer.core.sorter.spoon.dependency_graph;

import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import spoon.reflect.code.CtFieldAccess;
import spoon.reflect.code.CtFieldRead;
import spoon.reflect.code.CtFieldWrite;
import spoon.reflect.cu.SourcePosition;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.declaration.CtField;
import spoon.reflect.declaration.CtType;
import spoon.reflect.declaration.CtTypeMember;
import spoon.reflect.reference.CtFieldReference;
import spoon.reflect.visitor.filter.TypeFilter;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
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
    @SuppressWarnings("PMD.CompareObjectsWithEquals")
    static Set<CtField<?>> findReferencedFields(@NonNull CtTypeMember dependentMember, @NonNull CtElement astRoot) {

        CtType<?> declaringType = requireDeclaringType(dependentMember);
        int dependentSourceStart = requireSourceStart(dependentMember);

        @SuppressWarnings("unchecked")
        Class<CtFieldAccess<?>> fieldAccessClass = (Class<CtFieldAccess<?>>) (Class<?>) CtFieldAccess.class;
        TypeFilter<CtFieldAccess<?>> fieldAccessTypeFilter = new TypeFilter<>(fieldAccessClass);

        return astRoot.getElements(fieldAccessTypeFilter).stream()
                .map(CtFieldAccess::getVariable)
                .map(CtFieldReference::getDeclaration)
                .filter(Objects::nonNull)
                .filter(referencedField -> referencedField.getDeclaringType() == declaringType)
                .filter(referencedField -> shouldCreateDeclarationDependencyEdge(referencedField, dependentSourceStart))
                .collect(Collectors.toUnmodifiableSet());
    }

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
    static Set<CtField<?>> findReadFields(@NonNull CtTypeMember dependentMember, @NonNull CtElement astRoot) {
        CtType<?> declaringType = requireDeclaringType(dependentMember);
        @SuppressWarnings("unchecked")
        Class<CtFieldRead<?>> fieldReadClass = (Class<CtFieldRead<?>>) (Class<?>) CtFieldRead.class;
        TypeFilter<CtFieldRead<?>> fieldReadTypeFilter = new TypeFilter<>(fieldReadClass);
        return astRoot.getElements(fieldReadTypeFilter).stream()
                .map(CtFieldRead::getVariable)
                .map(CtFieldReference::getDeclaration)
                .filter(Objects::nonNull)
                .filter(referencedField -> referencedField.getDeclaringType() == declaringType)
                .collect(Collectors.toUnmodifiableSet());
    }

    @SuppressWarnings("PMD.CompareObjectsWithEquals")
    static Set<CtField<?>> findWrittenFields(@NonNull CtTypeMember dependentMember, @NonNull CtElement astRoot) {
        CtType<?> declaringType = requireDeclaringType(dependentMember);
        @SuppressWarnings("unchecked")
        Class<CtFieldWrite<?>> fieldWriteClass = (Class<CtFieldWrite<?>>) (Class<?>) CtFieldWrite.class;
        TypeFilter<CtFieldWrite<?>> fieldWriteTypeFilter = new TypeFilter<>(fieldWriteClass);
        return astRoot.getElements(fieldWriteTypeFilter).stream()
                .map(CtFieldWrite::getVariable)
                .map(CtFieldReference::getDeclaration)
                .filter(Objects::nonNull)
                .filter(referencedField -> referencedField.getDeclaringType() == declaringType)
                .collect(Collectors.toUnmodifiableSet());
    }
}
