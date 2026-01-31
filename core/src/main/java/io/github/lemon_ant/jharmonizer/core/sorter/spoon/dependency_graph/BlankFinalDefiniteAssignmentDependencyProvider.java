package io.github.lemon_ant.jharmonizer.core.sorter.spoon.dependency_graph;

import static io.github.lemon_ant.jharmonizer.core.sorter.spoon.dependency_graph.OrderDependentFieldReferenceUtils.requireDeclaringType;
import static io.github.lemon_ant.jharmonizer.core.sorter.spoon.dependency_graph.OrderDependentFieldReferenceUtils.requireSourceStart;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.NonNull;
import spoon.reflect.declaration.CtAnonymousExecutable;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.declaration.CtField;
import spoon.reflect.declaration.CtType;
import spoon.reflect.declaration.CtTypeMember;
import spoon.reflect.declaration.ModifierKind;

/**
 * Provides declaration-order dependencies required to keep reads of blank final fields after their assignments.
 *
 * <p>Java requires blank final fields to be definitely assigned before they are read during instance/static
 * initialization. Because field initializers and initializer blocks execute in source order, reordering these
 * members may introduce "variable might not have been initialized" compilation errors.
 */
final class BlankFinalDefiniteAssignmentDependencyProvider implements MemberDependencyProvider {

    @NonNull
    @Override
    public Set<MemberDependencyArc> findDirectProviderEdges(
            @NonNull CtTypeMember dependentMember, boolean keepAccessorsTogether) {

        Optional<CtElement> dependentInitializationAstRoot = extractInitializationAstRoot(dependentMember);
        if (dependentInitializationAstRoot.isEmpty()) {
            return Set.of();
        }

        Set<CtField<?>> blankFinalFieldsReadByDependentMember =
                OrderDependentFieldReferenceUtils.findReadFields(dependentMember, dependentInitializationAstRoot.get())
                        .stream()
                        .filter(BlankFinalDefiniteAssignmentDependencyProvider::isBlankFinalField)
                        .collect(Collectors.toUnmodifiableSet());

        if (blankFinalFieldsReadByDependentMember.isEmpty()) {
            return Set.of();
        }

        int dependentSourceStart = requireSourceStart(dependentMember);

        return blankFinalFieldsReadByDependentMember.stream()
                .flatMap(blankFinalField ->
                        findProviderMembersForBlankFinalRead(dependentMember, blankFinalField, dependentSourceStart)
                                .stream())
                .map(providerMember ->
                        new MemberDependencyArc(providerMember, MemberDependencyEdgeKind.DECLARATION_DEPENDENCY))
                .collect(Collectors.toUnmodifiableSet());
    }

    @SuppressWarnings("PMD.CompareObjectsWithEquals")
    private static Set<CtTypeMember> findProviderMembersForBlankFinalRead(
            CtTypeMember dependentMember, CtField<?> blankFinalField, int dependentSourceStart) {
        CtType<?> declaringType = requireDeclaringType(dependentMember);
        List<CtTypeMember> typeMembersInDeclaringType = declaringType.getTypeMembers();
        boolean blankFinalFieldIsStatic = blankFinalField.getModifiers().contains(ModifierKind.STATIC);

        List<CtTypeMember> candidateProviderMembers = typeMembersInDeclaringType.stream()
                .filter(typeMember -> typeMember != dependentMember)
                .filter(BlankFinalDefiniteAssignmentDependencyProvider::isInitializationMember)
                .filter(typeMember -> isStaticInitializationMember(typeMember) == blankFinalFieldIsStatic)
                .toList();

        return findClosestAssignmentProviderMember(candidateProviderMembers, blankFinalField, dependentSourceStart)
                .stream()
                .collect(Collectors.toUnmodifiableSet());
    }

    private static Optional<CtTypeMember> findClosestAssignmentProviderMember(
            List<CtTypeMember> candidateProviderMembers, CtField<?> blankFinalField, int dependentSourceStart) {
        CtTypeMember closestProviderMember = null;
        int closestProviderSourceStart = Integer.MIN_VALUE;

        for (CtTypeMember candidateProviderMember : candidateProviderMembers) {
            int candidateProviderSourceStart = requireSourceStart(candidateProviderMember);
            if (candidateProviderSourceStart >= dependentSourceStart) {
                continue;
            }

            if (!assignsField(candidateProviderMember, blankFinalField)) {
                continue;
            }

            if (candidateProviderSourceStart > closestProviderSourceStart) {
                closestProviderMember = candidateProviderMember;
                closestProviderSourceStart = candidateProviderSourceStart;
            }
        }

        return Optional.ofNullable(closestProviderMember);
    }

    private static boolean assignsField(CtTypeMember typeMember, CtField<?> blankFinalField) {
        Optional<CtElement> initializationAstRoot = extractInitializationAstRoot(typeMember);
        return initializationAstRoot
                .filter(ctElement -> OrderDependentFieldReferenceUtils.findWrittenFields(typeMember, ctElement)
                        .contains(blankFinalField))
                .isPresent();
    }

    private static Optional<CtElement> extractInitializationAstRoot(CtTypeMember typeMember) {
        if (typeMember instanceof CtField<?> fieldWithPotentialInitializer) {
            return Optional.ofNullable(fieldWithPotentialInitializer.getDefaultExpression());
        }

        if (typeMember instanceof CtAnonymousExecutable initializerBlock) {
            return Optional.ofNullable(initializerBlock.getBody());
        }

        return Optional.empty();
    }

    private static boolean isBlankFinalField(CtField<?> field) {
        return field.getModifiers().contains(ModifierKind.FINAL) && field.getDefaultExpression() == null;
    }

    private static boolean isInitializationMember(CtTypeMember typeMember) {
        return typeMember instanceof CtField<?> || typeMember instanceof CtAnonymousExecutable;
    }

    private static boolean isStaticInitializationMember(CtTypeMember typeMember) {
        return (typeMember instanceof CtField<?> fieldMember
                        && fieldMember.getModifiers().contains(ModifierKind.STATIC))
                || (typeMember instanceof CtAnonymousExecutable initializerBlock
                        && initializerBlock.getModifiers().contains(ModifierKind.STATIC));
    }
}
