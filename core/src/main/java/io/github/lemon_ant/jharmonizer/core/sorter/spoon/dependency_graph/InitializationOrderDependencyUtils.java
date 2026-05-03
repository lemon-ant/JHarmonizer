/*
 * SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.lemon_ant.jharmonizer.core.sorter.spoon.dependency_graph;

import static io.github.lemon_ant.jharmonizer.core.sorter.spoon.SpoonTypeMemberUtils.streamExplicitSrcTypeMembers;
import static io.github.lemon_ant.jharmonizer.core.sorter.spoon.dependency_graph.DeclaringTypeFieldReferenceUtils.requireDeclaringType;
import static io.github.lemon_ant.jharmonizer.core.sorter.spoon.dependency_graph.DeclaringTypeFieldReferenceUtils.requireSrcStart;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Value;
import spoon.reflect.code.CtExpression;
import spoon.reflect.code.CtLiteral;
import spoon.reflect.declaration.CtAnonymousExecutable;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.declaration.CtField;
import spoon.reflect.declaration.CtType;
import spoon.reflect.declaration.CtTypeMember;
import spoon.reflect.declaration.ModifierKind;

/**
 * Utilities for order-dependent initialization semantics (field initializers, initializer blocks)
 * and blank final definite assignment constraints.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
final class InitializationOrderDependencyUtils {

    private static boolean matchesInitializationMemberStaticness(CtTypeMember typeMember, boolean requiredStaticness) {
        return (typeMember instanceof CtField<?> || typeMember instanceof CtAnonymousExecutable)
                && typeMember.getModifiers().contains(ModifierKind.STATIC) == requiredStaticness;
    }

    /**
     * Resolves the initialization ast root.
     * @param typeMember the type member
     * @return the initialization ast root
     */
    @NonNull
    static Optional<CtElement> resolveInitializationAstRoot(@NonNull CtTypeMember typeMember) {
        if (typeMember instanceof CtField<?> fieldWithPotentialInitializer) {
            return Optional.ofNullable(fieldWithPotentialInitializer.getDefaultExpression());
        }

        if (typeMember instanceof CtAnonymousExecutable initializerBlock) {
            return Optional.ofNullable(initializerBlock.getBody());
        }

        return Optional.empty();
    }

    /**
     * Returns whether is blank final field.
     * @param field the field
     * @return {@code true} if is blank final field; otherwise {@code false}
     */
    static boolean isBlankFinalField(@NonNull CtField<?> field) {
        return field.getModifiers().contains(ModifierKind.FINAL) && field.getDefaultExpression() == null;
    }

    /**
     * Returns whether is static compile time constant variable.
     * @param field the field
     * @return {@code true} if is static compile time constant variable; otherwise {@code false}
     */
    static boolean isStaticCompileTimeConstantVariable(@NonNull CtField<?> field) {
        if (!field.getModifiers().contains(ModifierKind.STATIC)) {
            return false;
        }

        if (!field.getModifiers().contains(ModifierKind.FINAL)) {
            return false;
        }

        CtExpression<?> defaultExpression = field.getDefaultExpression();
        if (defaultExpression == null) {
            return false;
        }

        String typeQualifiedName = field.getType().getQualifiedName();
        boolean isPrimitiveOrString = field.getType().isPrimitive() || "java.lang.String".equals(typeQualifiedName);
        return isPrimitiveOrString
                && DeclaringTypeFieldReferenceUtils.findPartiallyEvaluatedExpression(defaultExpression)
                        .map(foldedExpression -> foldedExpression instanceof CtLiteral<?>)
                        .orElse(false);
    }

    /**
     * Returns all candidate provider members that potentially assign the given blank final field before it is read.
     *
     * <p>When {@code relaxedForwardReferences} is {@code true} (default), uses a conservative approach: returns all
     * initialization members (fields / init blocks) declared above the dependent member in the original source order
     * that write to the blank final field. This reduces the risk of reordering causing "variable might not have been
     * initialized" compilation errors.
     *
     * <p>When {@code relaxedForwardReferences} is {@code false}, all initialization members that write to the blank
     * final field are returned regardless of their source position relative to the dependent member.
     *
     * @param dependentMember the member that reads the blank final field
     * @param blankFinalField the blank final field being read
     * @param dependentSrcStart the source start position of the dependent member
     * @param relaxedForwardReferences whether to skip providers declared after the dependent member
     * @return the candidate provider members
     */
    @NonNull
    static Set<CtTypeMember> resolveProviderMembersForBlankFinalRead(
            @NonNull CtTypeMember dependentMember,
            @NonNull CtField<?> blankFinalField,
            int dependentSrcStart,
            boolean relaxedForwardReferences) {

        CtType<?> declaringType = requireDeclaringType(dependentMember);
        boolean blankFinalFieldIsStatic = blankFinalField.getModifiers().contains(ModifierKind.STATIC);

        return streamExplicitSrcTypeMembers(declaringType)
                .filter(typeMember -> typeMember != dependentMember)
                .filter(typeMember -> matchesInitializationMemberStaticness(typeMember, blankFinalFieldIsStatic))
                .map(candidateProviderMember ->
                        new ProviderCandidate(candidateProviderMember, requireSrcStart(candidateProviderMember)))
                // In relaxed mode, only include providers declared before the dependent (position-guarded).
                // In strict mode (relaxedForwardReferences=false), include all candidates regardless of position.
                .filter(candidate -> !relaxedForwardReferences || candidate.getSrcStart() < dependentSrcStart)
                .map(ProviderCandidate::getProviderMember)
                .filter(providerMember -> isFieldWrittenByMember(providerMember, blankFinalField))
                .collect(Collectors.toUnmodifiableSet());
    }

    private static boolean isFieldWrittenByMember(CtTypeMember candidateProviderMember, CtField<?> blankFinalField) {
        return resolveInitializationAstRoot(candidateProviderMember)
                .map(astRoot ->
                        DeclaringTypeFieldReferenceUtils.findFieldsWrittenByMember(candidateProviderMember, astRoot))
                .map(writtenFields -> writtenFields.contains(blankFinalField))
                .orElse(false);
    }

    @Value
    private static class ProviderCandidate {
        @NonNull
        CtTypeMember providerMember;

        int srcStart;
    }
}
