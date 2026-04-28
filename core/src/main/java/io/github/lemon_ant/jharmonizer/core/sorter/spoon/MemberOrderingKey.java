// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
package io.github.lemon_ant.jharmonizer.core.sorter.spoon;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;

/**
 * Base ordering key carrying a {@link spoon.reflect.declaration.CtTypeMember}'s own
 * source-level attributes used for comparison: source position, alpha key, alpha sorting
 * rank, and visibility rank.
 *
 * <p>This class is the minimal, always-populated representation derived in phase 1 of key
 * creation. It is used directly where accessor clustering is not needed — for example when
 * ordering top-level types — and as the intermediate representation inside
 * {@link OrderingKeyFactory#deriveAll(java.util.List, boolean, java.util.List)} before
 * cluster top-member attributes are resolved.
 *
 * <p>Subclasses may extend this class to carry additional context; see
 * {@link ClusteredOrderingKey}.
 */
@Getter
@AllArgsConstructor(access = AccessLevel.PACKAGE)
@EqualsAndHashCode
@ToString
class MemberOrderingKey {

    /** Source-start position of this member in the original file. */
    private final int srcStart;

    /** Alphabetical sort key derived from the member's name and signature. */
    @NonNull
    private final String alphaKey;

    /**
     * Rank applied before the alpha key in ALPHA comparisons. Non-zero only for
     * {@code CtAnonymousExecutable} (initializer blocks), which receive rank {@code 1} so
     * they sort after all regular named members regardless of their source position.
     */
    private final int alphaSortingRank;

    /** Visibility rank used by {@code VISIBILITY_ASC} / {@code VISIBILITY_DESC} rules. */
    private final int visibilityRank;
}
