package io.github.lemon_ant.jharmonizer.core.sorter.spoon.dependency_graph;

import static io.github.lemon_ant.jharmonizer.core.sorter.spoon.SpoonTypeMemberUtils.streamExplicitSourceTypeMembers;
import static io.github.lemon_ant.jharmonizer.core.sorter.spoon.dependency_graph.DeclaringTypeFieldReferenceUtils.requireDeclaringType;
import static io.github.lemon_ant.jharmonizer.core.sorter.spoon.dependency_graph.DeclaringTypeFieldReferenceUtils.requireSourceStart;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Value;
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

    private static boolean matchesInitializationMemberStaticness(
            @NonNull CtTypeMember typeMember, boolean requiredStaticness) {
        return (typeMember instanceof CtField<?> || typeMember instanceof CtAnonymousExecutable)
                && typeMember.getModifiers().contains(ModifierKind.STATIC) == requiredStaticness;
    }

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

    static boolean isBlankFinalField(@NonNull CtField<?> field) {
        return field.getModifiers().contains(ModifierKind.FINAL) && field.getDefaultExpression() == null;
    }

    /**
     * Returns all candidate provider members that potentially assign the given blank final field before it is read.
     *
     * <p>Conservative approach: return all initialization members (fields / init blocks) declared above the dependent
     * member in the original source order, that write to the blank final field. This reduces the risk of reordering
     * causing "variable might not have been initialized" compilation errors.
     */
    @NonNull
    static Set<CtTypeMember> resolveProviderMembersForBlankFinalRead(
            @NonNull CtTypeMember dependentMember, @NonNull CtField<?> blankFinalField, int dependentSourceStart) {

        CtType<?> declaringType = requireDeclaringType(dependentMember);
        boolean blankFinalFieldIsStatic = blankFinalField.getModifiers().contains(ModifierKind.STATIC);

        return streamExplicitSourceTypeMembers(declaringType)
                .filter(typeMember -> typeMember != dependentMember)
                .filter(typeMember -> matchesInitializationMemberStaticness(typeMember, blankFinalFieldIsStatic))
                .map(candidateProviderMember ->
                        new ProviderCandidate(candidateProviderMember, requireSourceStart(candidateProviderMember)))
                .filter(candidate -> candidate.getSourceStart() < dependentSourceStart)
                .map(ProviderCandidate::getProviderMember)
                .filter(providerMember -> assignsField(providerMember, blankFinalField))
                .collect(Collectors.toUnmodifiableSet());
    }

    private static boolean assignsField(
            @NonNull CtTypeMember candidateProviderMember, @NonNull CtField<?> blankFinalField) {
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

        int sourceStart;
    }
}
