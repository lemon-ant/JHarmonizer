// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
package io.github.lemon_ant.jharmonizer.core.translator.spoon.annotation;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;
import lombok.Value;

/**
 * Carries the selected annotation type name and its canonical-identity confirmation.
 * Repeatable metadata lookup requires a confirmed name. Unconfirmed names support conservative order preservation.
 */
@Value
@Getter(AccessLevel.PACKAGE)
@AllArgsConstructor(access = AccessLevel.PACKAGE)
class AnnotationTypeNameResolution {

    /** Confirmed canonical name or unconfirmed written/contextual name, without key prefixes or container substitution. */
    @NonNull
    String annotationTypeName;

    /** Whether source declarations, imports, or runtime module lookup established canonical identity. */
    boolean canonicalTypeNameConfirmed;

    /**
     * Retains the canonical name from a successful declaration/import lookup.
     * @param canonicalTypeNameLookupResult successful declaration or import lookup
     * @throws IllegalArgumentException if the lookup did not establish a name
     */
    AnnotationTypeNameResolution(@NonNull CanonicalAnnotationTypeNameLookupResult canonicalTypeNameLookupResult) {
        String canonicalAnnotationTypeName = canonicalTypeNameLookupResult.getCanonicalAnnotationTypeName();
        if (canonicalAnnotationTypeName == null) {
            throw new IllegalArgumentException("Canonical annotation lookup did not establish a name");
        }
        annotationTypeName = canonicalAnnotationTypeName;
        canonicalTypeNameConfirmed = true;
    }

    /**
     * Retains an unconfirmed fallback name.
     * @param annotationTypeName original written name or name inferred from context
     */
    AnnotationTypeNameResolution(@NonNull String annotationTypeName) {
        this(annotationTypeName, false);
    }
}
