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
     * Formats a named, non-array reference using dots between the declaring-type links recorded by Spoon.
     * For example, a nested {@code pkg.Outer$Tag} becomes {@code pkg.Outer.Tag}. A {@code $} within an identifier
     * stays intact, as in top-level {@code pkg.Dollar$Tag}. This formatting works with unavailable declarations.
     * @param typeReference named, non-array type reference to format
     * @return qualified name with dot-separated declaring types
     */
    @NonNull
    static String formatCanonicalTypeName(@NonNull CtTypeReference<?> typeReference) {
        CtTypeReference<?> declaringTypeReference = typeReference.getDeclaringType();
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
     * Matches complete leading name segments. For example, {@code Owner} matches {@code Owner.Tag}.
     * @param typeNameToCheck tokenized type name to inspect
     * @param expectedTypeNamePrefix complete name segments expected at the beginning
     * @return {@code true} for an exact match or a prefix followed by a dot
     */
    static boolean startsWithTypeName(@NonNull String typeNameToCheck, @NonNull String expectedTypeNamePrefix) {
        return typeNameToCheck.equals(expectedTypeNamePrefix)
                || typeNameToCheck.startsWith(expectedTypeNamePrefix + '.');
    }
}
