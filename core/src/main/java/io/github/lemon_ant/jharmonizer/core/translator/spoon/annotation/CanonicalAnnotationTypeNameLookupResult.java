// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
package io.github.lemon_ant.jharmonizer.core.translator.spoon.annotation;

import static io.github.lemon_ant.jharmonizer.core.translator.spoon.annotation.SpoonImportUtils.extractCanonicalSingleImportTypeName;
import static io.github.lemon_ant.jharmonizer.core.translator.spoon.annotation.SpoonTypeNameUtils.formatCanonicalTypeName;
import static io.github.lemon_ant.jharmonizer.core.translator.spoon.annotation.SpoonTypeNameUtils.requireAnnotationTypeDeclaration;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import spoon.reflect.declaration.CtImport;
import spoon.reflect.declaration.CtType;
import spoon.reflect.reference.CtTypeReference;

/**
 * Outcome of a canonical annotation type-name lookup. Source successes require an annotation declaration.
 * Single imports validate available declarations; a missing declaration leaves only canonical import evidence.
 * A non-terminal lookup allows the next stage; a terminal lookup stops even when its type name is unknown.
 * For example, an unavailable superclass must block imports that might name a shadowed annotation.
 */
@Getter(AccessLevel.PACKAGE)
@Value
class CanonicalAnnotationTypeNameLookupResult {

    @NonNull
    static final CanonicalAnnotationTypeNameLookupResult NOT_FOUND = new CanonicalAnnotationTypeNameLookupResult(false);

    @NonNull
    static final CanonicalAnnotationTypeNameLookupResult UNRESOLVED = new CanonicalAnnotationTypeNameLookupResult(true);

    @Nullable
    String canonicalAnnotationTypeName;

    boolean terminal;

    /**
     * Derives the name from one definite type import, checking its declaration when available.
     * An unresolved ordinary import has no declaration: its canonical spelling is the remaining evidence.
     * @param typeImport selected single type import
     * @throws IllegalArgumentException if the import does not establish a single type name
     * @throws IllegalStateException if an available imported declaration is not an annotation type
     */
    CanonicalAnnotationTypeNameLookupResult(@NonNull CtImport typeImport) {
        String importedTypeNameCandidate = extractCanonicalSingleImportTypeName(typeImport);
        if (importedTypeNameCandidate == null) {
            throw new IllegalArgumentException("Expected a single type import");
        }
        // A resolved JDK import can have getDeclaration()==null while its classpath declaration is available.
        // Inspect that declaration's kind as well; this does not enable external repeatable-metadata lookup.
        CtType<?> importedTypeDeclaration = typeImport.getReference() instanceof CtTypeReference<?> importedType
                ? importedType.getTypeDeclaration()
                : null;
        canonicalAnnotationTypeName = importedTypeDeclaration == null
                ? importedTypeNameCandidate
                : formatCanonicalTypeName(requireAnnotationTypeDeclaration(importedTypeDeclaration)
                        .getReference());
        terminal = true;
    }

    /**
     * Derives the canonical name from a selected declaration after verifying that it is an annotation type.
     * @param typeDeclaration selected type declaration
     * @throws IllegalStateException if the declaration is not an annotation type
     */
    CanonicalAnnotationTypeNameLookupResult(@NonNull CtType<?> typeDeclaration) {
        canonicalAnnotationTypeName = formatCanonicalTypeName(
                requireAnnotationTypeDeclaration(typeDeclaration).getReference());
        terminal = true;
    }

    /** Creates a status without a successful type name. */
    @SuppressWarnings("PMD.NullAssignment")
    private CanonicalAnnotationTypeNameLookupResult(boolean terminal) {
        canonicalAnnotationTypeName = null;
        this.terminal = terminal;
    }
}
