// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
package io.github.lemon_ant.jharmonizer.core.config.unified;

import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;
import lombok.Getter;
import lombok.NonNull;

/**
 * Unified declaration modifiers used in rules; applicability is defined via TargetCategory sets.
 * Pairwise conflicts are declared globally (category-agnostic) and checked.
 */
public enum DeclarationModifier {
    STATIC(EnumSet.of(TargetCategory.FIELD, TargetCategory.METHOD, TargetCategory.TYPE, TargetCategory.INIT_BLOCK)),
    FINAL(EnumSet.of(TargetCategory.FIELD, TargetCategory.METHOD, TargetCategory.TYPE)),
    ABSTRACT(EnumSet.of(TargetCategory.METHOD, TargetCategory.TYPE)),
    DEFAULT(EnumSet.of(TargetCategory.METHOD)), // interface default methods
    SYNCHRONIZED(EnumSet.of(TargetCategory.METHOD)),
    TRANSIENT(EnumSet.of(TargetCategory.FIELD)),
    VOLATILE(EnumSet.of(TargetCategory.FIELD)),
    NATIVE(EnumSet.of(TargetCategory.METHOD)),
    STRICTFP(EnumSet.of(TargetCategory.METHOD, TargetCategory.TYPE)),
    SEALED(EnumSet.of(TargetCategory.TYPE)),
    NON_SEALED(EnumSet.of(TargetCategory.TYPE)),
    ;

    @Getter
    private final Set<TargetCategory> applicableTargets;

    // Global symmetric conflicts (no TargetCategory scoping)
    @SuppressWarnings("PMD.UseEnumCollections")
    private final Set<DeclarationModifier> conflicts = new HashSet<>();

    static {
        // METHOD-level pairs (but safe globally because of applicability filtering):
        conflict(ABSTRACT, FINAL);
        conflict(ABSTRACT, NATIVE);
        conflict(ABSTRACT, SYNCHRONIZED);

        conflict(DEFAULT, ABSTRACT);
        conflict(DEFAULT, STATIC);
        conflict(DEFAULT, FINAL);
        conflict(DEFAULT, SYNCHRONIZED);
        conflict(DEFAULT, NATIVE);

        // TYPE-level pair (SEALED vs NON_SEALED) — applies only on types via applicability:
        conflict(SEALED, NON_SEALED);

        // No FIELD-only conflicts in the current minimal model.
    }

    /**
     * Global conflict check. Category is ignored intentionally:
     * applicability per category is validated separately before conflicts are checked.
     */
    boolean hasConflictWith(@NonNull DeclarationModifier other) {
        return this.conflicts.contains(other);
    }

    /**
     * Returns whether is applicable to.
     * @param targetCategory the target category
     * @return {@code true} if is applicable to; otherwise {@code false}
     */
    boolean isApplicableTo(@NonNull TargetCategory targetCategory) {
        return applicableTargets.contains(targetCategory);
    }

    private static void conflict(DeclarationModifier a, DeclarationModifier b) {
        a.conflicts.add(b);
        b.conflicts.add(a);
    }

    /**
     * Creates a new DeclarationModifier.
     * @param applicableTargets the applicable targets
     */
    DeclarationModifier(Set<TargetCategory> applicableTargets) {
        this.applicableTargets = Collections.unmodifiableSet(applicableTargets);
    }
}
