// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
package io.github.lemon_ant.jharmonizer.core.translator.spoon.annotation;

import lombok.NonNull;
import lombok.experimental.UtilityClass;
import spoon.reflect.declaration.CtAnnotationType;
import spoon.reflect.declaration.CtType;
import spoon.reflect.reference.CtTypeReference;

/** Formats type names, matches written prefixes, and validates selected annotation declarations during parsing. */
@UtilityClass
class SpoonTypeNameUtils {

    /**
     * Formats a named, non-array reference using dots between its recorded declaring types.
     * Owner references do not require available declarations; this method never calls {@code getDeclaration()}.
     * For example, {@code pkg.Outer$Tag} becomes {@code pkg.Outer.Tag}; a top-level {@code pkg.Dollar$Tag}
     * and the identifiers in {@code pkg.Dollar$Owner.Tag$Kind} retain their '$'.
     * Uses only the nesting recorded by Spoon; does not infer missing owners or resolve type aliases.
     * @param typeReference named, non-array type reference to format
     * @return qualified name with dot-separated declaring types
     */
    @NonNull
    static String formatCanonicalTypeName(@NonNull CtTypeReference<?> typeReference) {
        CtTypeReference<?> declaringTypeReference = typeReference.getDeclaringType();
        // No owner reference means getQualifiedName() adds no nesting separator; any '$' belongs to an identifier.
        // Replacing every '$' would corrupt legal names such as the top-level annotation Dollar$Tag.
        return declaringTypeReference == null
                ? typeReference.getQualifiedName()
                : formatCanonicalTypeName(declaringTypeReference) + '.' + typeReference.getSimpleName();
    }

    /**
     * Requires a selected declaration to be an annotation before using its identity or repeatable metadata.
     * @param typeDeclaration source or classpath declaration selected by lookup
     * @return declaration narrowed to an annotation type
     * @throws IllegalStateException if the selected declaration is another type
     */
    @NonNull
    static CtAnnotationType<?> requireAnnotationTypeDeclaration(@NonNull CtType<?> typeDeclaration) {
        if (typeDeclaration instanceof CtAnnotationType<?> annotationTypeDeclaration) {
            return annotationTypeDeclaration;
        }
        String declarationOrigin = typeDeclaration.isShadow() ? "external" : "source";
        throw new IllegalStateException("Expected an annotation declaration, but resolved " + declarationOrigin
                + " type " + formatCanonicalTypeName(typeDeclaration.getReference()));
    }

    /**
     * Matches a type-name prefix at complete name-segment boundaries, without resolving declarations or inheritance.
     * For prefix {@code Owner}, accepts {@code Owner} and {@code Owner.Tag}, but rejects {@code OwnerOther.Tag}.
     * @param typeNameToCheck tokenized type name to inspect
     * @param expectedTypeNamePrefix complete name segments expected at the beginning
     * @return {@code true} for an exact match or a prefix followed by a dot
     */
    static boolean startsWithTypeName(@NonNull String typeNameToCheck, @NonNull String expectedTypeNamePrefix) {
        return typeNameToCheck.equals(expectedTypeNamePrefix)
                || typeNameToCheck.startsWith(expectedTypeNamePrefix + '.');
    }
}
