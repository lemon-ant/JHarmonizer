// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
package io.github.lemon_ant.jharmonizer.core.spoon;

import static io.github.lemon_ant.jharmonizer.core.spoon.SpoonTypeUtils.formatCanonicalTypeName;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.stream.Stream;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.Value;
import lombok.experimental.UtilityClass;
import org.jspecify.annotations.Nullable;
import spoon.reflect.declaration.CtCompilationUnit;
import spoon.reflect.declaration.CtRecord;
import spoon.reflect.declaration.CtType;
import spoon.reflect.declaration.ModifierKind;
import spoon.reflect.reference.CtTypeReference;

/** Looks up inherited member types through superclass and superinterface declarations available in one source file. */
@UtilityClass
class SpoonTypeInheritanceUtils {
    private static final int MAX_UNAMBIGUOUS_DECLARATIONS = 1;

    /**
     * Finds a uniquely inherited member type using superclass and superinterface declarations in one source file.
     * Stops each branch at a direct declaration, including private types that are not inherited.
     * Missing declarations block only branches that must be searched for the requested member name.
     * @param compilationUnit original Spoon model of the source file containing the lookup scope
     * @param ownerType type whose inherited members are searched
     * @param memberTypeName simple name of the inherited member type
     * @return found declaration, absence, or an unresolved result for needed unavailable or ambiguous parents
     */
    @NonNull
    @SuppressWarnings("PMD.UseConcurrentHashMap")
    static InheritedMemberTypeLookupResult findInheritedMemberTypeDeclaration(
            @NonNull CtCompilationUnit compilationUnit, @NonNull CtType<?> ownerType, @NonNull String memberTypeName) {
        // All traversal state belongs to this synchronous lookup and is discarded on return.
        Queue<CtTypeReference<?>> parentTypesToVisit = new ArrayDeque<>();
        streamParentTypeReferences(ownerType).forEach(parentTypesToVisit::add);
        Set<String> visitedTypeNames = new HashSet<>();
        visitedTypeNames.add(ownerType.getQualifiedName());
        Map<String, CtType<?>> inheritedDeclarationsByCanonicalName = new HashMap<>();
        while (!parentTypesToVisit.isEmpty()) {
            CtType<?> parentDeclaration = findSrcParentTypeDeclaration(compilationUnit, parentTypesToVisit.remove());
            if (parentDeclaration == null) {
                return InheritedMemberTypeLookupResult.UNRESOLVED;
            }
            CtType<?> declaredMemberType = parentDeclaration.getNestedType(memberTypeName);
            if (declaredMemberType != null) {
                if (!declaredMemberType.hasModifier(ModifierKind.PRIVATE)) {
                    // CtType.equals compares structure; canonical names distinguish homonyms and deduplicate diamonds.
                    inheritedDeclarationsByCanonicalName.put(
                            formatCanonicalTypeName(declaredMemberType.getReference()), declaredMemberType);
                }
                // A private Parent.Inner is not inherited but still hides Grandparent.Inner (JLS 8.5).
                // Any direct declaration ends this branch; do not inspect its owner's ancestors.
                continue;
            }
            if (visitedTypeNames.add(parentDeclaration.getQualifiedName())) {
                streamParentTypeReferences(parentDeclaration).forEach(parentTypesToVisit::add);
            }
        }
        return computeInheritedMemberTypeLookupResult(inheritedDeclarationsByCanonicalName);
    }

    /** Converts canonical candidates into absence, one declaration, or ambiguity. */
    @NonNull
    private static InheritedMemberTypeLookupResult computeInheritedMemberTypeLookupResult(
            Map<String, CtType<?>> inheritedDeclarationsByCanonicalName) {
        if (inheritedDeclarationsByCanonicalName.size() > MAX_UNAMBIGUOUS_DECLARATIONS) {
            return InheritedMemberTypeLookupResult.UNRESOLVED;
        }
        return inheritedDeclarationsByCanonicalName.values().stream()
                .findFirst()
                .map(inheritedDeclaration -> new InheritedMemberTypeLookupResult(inheritedDeclaration, false))
                .orElse(InheritedMemberTypeLookupResult.NOT_FOUND);
    }

    /** Accepts only parents belonging to the same original Spoon source-file model instance. */
    @Nullable
    @SuppressWarnings("PMD.CompareObjectsWithEquals")
    private static CtType<?> findSrcParentTypeDeclaration(
            CtCompilationUnit compilationUnit, CtTypeReference<?> parentTypeReference) {
        CtType<?> parentDeclaration = parentTypeReference.getDeclaration();
        return parentDeclaration != null
                        && !parentDeclaration.isShadow()
                        && parentDeclaration.getPosition().getCompilationUnit() == compilationUnit
                ? parentDeclaration
                : null;
    }

    /** Enumerates parents for inherited member-type lookup, excluding fixed Object/Enum/Record superclasses. */
    @NonNull
    private static Stream<CtTypeReference<?>> streamParentTypeReferences(CtType<?> srcType) {
        Stream<CtTypeReference<?>> superclass = Stream.<CtTypeReference<?>>ofNullable(srcType.getSuperclass())
                // These fixed ancestors cannot contribute the annotation types being resolved.
                .filter(parentType -> !srcType.isEnum()
                        && !(srcType instanceof CtRecord)
                        && !Object.class.getName().equals(parentType.getQualifiedName()));
        return Stream.concat(srcType.getSuperInterfaces().stream(), superclass);
    }

    /** Outcome of a source inheritance lookup; absence is distinct from an unavailable or ambiguous branch. */
    @Value
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    static class InheritedMemberTypeLookupResult {

        @NonNull
        private static final InheritedMemberTypeLookupResult NOT_FOUND =
                new InheritedMemberTypeLookupResult(null, false);

        @NonNull
        private static final InheritedMemberTypeLookupResult UNRESOLVED =
                new InheritedMemberTypeLookupResult(null, true);

        @Nullable
        CtType<?> declaration;

        boolean unresolved;
    }
}
