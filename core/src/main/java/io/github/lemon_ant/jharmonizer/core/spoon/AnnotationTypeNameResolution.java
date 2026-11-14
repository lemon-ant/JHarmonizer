// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
package io.github.lemon_ant.jharmonizer.core.spoon;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;
import lombok.Value;

/**
 * Carries either a confirmed canonical name or an unconfirmed name for conservative grouping.
 * Source declarations, single-type imports, or runtime java.lang declarations in module context can establish identity.
 * An unconfirmed name retains the original spelling or adds supported package context. It does not authorize source
 * metadata lookup. Lookup blockers and insufficient evidence retain the written name with confirmation disabled.
 * The resolver selects the name and its evidence; constructors retain explicit state or adapt that evidence.
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
     * @param canonicalTypeNameLookupResult successful lookup whose selected declaration/import was validated
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
     * Retains a fallback name without confirming its canonical identity or whether its declaration is external.
     * @param annotationTypeName original written name or name inferred from context
     */
    AnnotationTypeNameResolution(@NonNull String annotationTypeName) {
        this(annotationTypeName, false);
    }
}
