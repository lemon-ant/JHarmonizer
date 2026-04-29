// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
package io.github.lemon_ant.jharmonizer.core.sorter.spoon;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.Value;

/**
 * Immutable sort key carrying the source-level attributes a comparator may use to order
 * type members or accessor clusters: source position, alpha key, alpha sorting rank and
 * visibility rank.
 *
 * <p>Two flavours of {@link OrderingKey} coexist by intent — the comparator does not need
 * to distinguish them and treats every {@link OrderingKey} the same way:
 * <ul>
 *   <li>An <em>own</em> key derived directly from a single {@link spoon.reflect.declaration.CtTypeMember}
 *       (its source position, its method/field signature alpha key, etc.).</li>
 *   <li>A <em>representative</em> key for an accessor cluster: a synthetic instance whose
 *       {@code alphaKey} is the JavaBeans property name and whose {@code srcStart} /
 *       {@code visibilityRank} are taken from the cluster's top member. This makes
 *       cluster-vs-cluster comparisons sort by property name, while cluster-vs-anything-else
 *       comparisons use the top member's source position and visibility.</li>
 * </ul>
 *
 * <p>Each member that does not belong to a multi-member accessor cluster uses its own key as
 * its representative (same instance), so cluster handling is uniform: the {@link SortableTypeMember}
 * comparator simply compares representative references and falls back to own keys when the
 * representatives are the same instance.
 */
@Value
@AllArgsConstructor(access = AccessLevel.PACKAGE)
class OrderingKey {

    /** Source-start position used by the PRESERVE rule. */
    int srcStart;

    /** Alphabetical sort key used by the ALPHA rule. For cluster representatives this is the property name. */
    @NonNull
    String alphaKey;

    /**
     * Rank applied before the alpha key in ALPHA comparisons. Non-zero only for
     * {@code CtAnonymousExecutable} (initializer blocks), which receive rank {@code 1} so
     * they sort after all regular named members regardless of their source position.
     */
    int alphaSortingRank;

    /** Visibility rank used by the {@code VISIBILITY_ASC} / {@code VISIBILITY_DESC} rules. */
    int visibilityRank;
}
