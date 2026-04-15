package io.github.lemon_ant.jharmonizer.core.sorter.spoon.dependency_graph;

import static io.github.lemon_ant.jharmonizer.core.sorter.spoon.SpoonTypeMemberUtils.streamExplicitSrcTypeMembers;
import static io.github.lemon_ant.jharmonizer.core.sorter.spoon.dependency_graph.DeclaringTypeFieldReferenceUtils.requireDeclaringType;
import static io.github.lemon_ant.jharmonizer.core.sorter.spoon.dependency_graph.DeclaringTypeFieldReferenceUtils.requireSrcStart;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.NonNull;
import spoon.reflect.declaration.CtAnonymousExecutable;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.declaration.CtField;
import spoon.reflect.declaration.CtType;
import spoon.reflect.declaration.CtTypeMember;
import spoon.reflect.declaration.ModifierKind;

/**
 * Provides declaration dependency edges where a field or initializer block G reads a mutable field F
 * that may have been mutated (via method calls) by a prior initializer block B that also reads F.
 *
 * <p>This handles the gap where an initializer block calls methods on a previously-declared mutable
 * field (e.g. {@code HashMap.put()}), and a later field captures a snapshot of that field's contents.
 * Since method invocations on a field appear as reads in Spoon's AST (not writes), the standard
 * field-reference tracking does not detect the dependency. This provider conservatively adds it:
 * if both B and G read the same non-compile-time-constant field F, and B is declared before G in
 * source order, then G depends on B.
 *
 * <p>Blank-final assignment dependencies are already covered by
 * {@link BlankFinalDefiniteAssignmentDependencyProvider} and are excluded here to avoid duplicate edges.
 */
final class InitializerBlockMutableFieldReadDependencyProvider implements MemberDependencyProvider {

    /**
     * Finds the direct provider edges.
     * @param dependentMember the dependent member
     * @param keepAccessorsTogether the keep accessors together flag
     * @return the matching direct provider edges
     */
    @NonNull
    @Override
    public Set<MemberDependencyArc> findDirectProviderEdges(
            @NonNull CtTypeMember dependentMember, boolean keepAccessorsTogether) {

        Optional<CtElement> dependentInitializationAstRoot =
                InitializationOrderDependencyUtils.resolveInitializationAstRoot(dependentMember);
        if (dependentInitializationAstRoot.isEmpty()) {
            return Set.of();
        }

        Set<CtField<?>> mutableFieldsReadByDependent =
                DeclaringTypeFieldReferenceUtils.findFieldsReadByMember(
                                dependentMember, dependentInitializationAstRoot.get())
                        .stream()
                        .filter(field -> !InitializationOrderDependencyUtils.isStaticCompileTimeConstantVariable(field))
                        .filter(field -> !InitializationOrderDependencyUtils.isBlankFinalField(field))
                        .collect(Collectors.toUnmodifiableSet());

        if (mutableFieldsReadByDependent.isEmpty()) {
            return Set.of();
        }

        int dependentSrcStart = requireSrcStart(dependentMember);
        CtType<?> declaringType = requireDeclaringType(dependentMember);

        return mutableFieldsReadByDependent.stream()
                .flatMap(mutableField ->
                        streamInitializerBlocksBeforeMemberReadingField(declaringType, mutableField, dependentSrcStart))
                .map(initializerBlock ->
                        new MemberDependencyArc(initializerBlock, MemberDependencyEdgeKind.DECLARATION_DEPENDENCY))
                .collect(Collectors.toUnmodifiableSet());
    }

    @NonNull
    private static Stream<CtAnonymousExecutable> streamInitializerBlocksBeforeMemberReadingField(
            CtType<?> declaringType, CtField<?> field, int dependentSrcStart) {

        boolean fieldIsStatic = field.getModifiers().contains(ModifierKind.STATIC);

        return streamExplicitSrcTypeMembers(declaringType)
                .filter(member -> member instanceof CtAnonymousExecutable)
                .map(member -> (CtAnonymousExecutable) member)
                .filter(initializerBlock ->
                        initializerBlock.getModifiers().contains(ModifierKind.STATIC) == fieldIsStatic)
                .filter(initializerBlock -> requireSrcStart(initializerBlock) < dependentSrcStart)
                .filter(initializerBlock -> isFieldReadByInitializerBlock(initializerBlock, field));
    }

    private static boolean isFieldReadByInitializerBlock(CtAnonymousExecutable initializerBlock, CtField<?> field) {
        return Optional.ofNullable(initializerBlock.getBody())
                .map(body -> DeclaringTypeFieldReferenceUtils.findFieldsReadByMember(initializerBlock, body))
                .map(readFields -> readFields.contains(field))
                .orElse(false);
    }
}
