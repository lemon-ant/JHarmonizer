// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
package io.github.lemon_ant.jharmonizer.core.sorter.spoon;

import edu.umd.cs.findbugs.annotations.Nullable;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;

/**
 * Extended ordering key that inherits {@link MemberOrderingKey}'s member-own attributes and
 * additionally carries JavaBeans accessor cluster information.
 *
 * <p>When {@code keepAccessorsTogether} is enabled and a member is a recognized accessor,
 * {@link #propertyName} is non-null and the cluster-representative attributes
 * ({@link #clusterSrcStart}, {@link #clusterVisibilityRank}) reflect the <em>top member</em>
 * of the property cluster (the one that sorts first under the member-only comparator). For
 * all other members the cluster attributes equal the member's own attributes.
 *
 * <p>The cluster-aware comparator ({@link ComparatorUtils#buildOrderingComparator(java.util.List)})
 * substitutes cluster attributes for own attributes only when comparing two accessors of
 * different property clusters ({@link ComparatorUtils#isCrossCluster}).
 */
@Getter
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
class ClusteredOrderingKey extends MemberOrderingKey {

    /**
     * The JavaBeans property name identifying this member's accessor cluster, or {@code null}
     * when the member is not a recognized accessor or accessor clustering is disabled. Two
     * members share a cluster iff both have a non-null property name and the names are equal.
     * The cluster-aware comparator uses this name as the ALPHA key when comparing accessors
     * of different property clusters.
     */
    @Nullable
    private final String propertyName;

    /**
     * Source-start position of the cluster's top member; equals {@link #getSrcStart()} when
     * this member is not part of a multi-member accessor cluster. Used by the PRESERVE rule
     * when the cluster-aware comparator detects a cross-cluster accessor pair.
     */
    private final int clusterSrcStart;

    /**
     * Visibility rank of the cluster's top member; equals {@link #getVisibilityRank()} when
     * this member is not part of a multi-member accessor cluster. Used by the
     * {@code VISIBILITY_*} rules when the cluster-aware comparator detects a cross-cluster
     * accessor pair.
     */
    private final int clusterVisibilityRank;

    /**
     * Creates a new clustered ordering key.
     *
     * @param srcStart source-start position of this member
     * @param alphaKey alphabetical sort key for this member
     * @param alphaSortingRank rank used before the alpha key in ALPHA comparisons
     * @param visibilityRank visibility rank for this member
     * @param propertyName JavaBeans property name identifying this member's accessor cluster,
     *     or {@code null} when the member is not part of a cluster
     * @param clusterSrcStart source-start position of the cluster's top member
     * @param clusterVisibilityRank visibility rank of the cluster's top member
     */
    ClusteredOrderingKey(
            int srcStart,
            @NonNull String alphaKey,
            int alphaSortingRank,
            int visibilityRank,
            @Nullable String propertyName,
            int clusterSrcStart,
            int clusterVisibilityRank) {
        super(srcStart, alphaKey, alphaSortingRank, visibilityRank);
        this.propertyName = propertyName;
        this.clusterSrcStart = clusterSrcStart;
        this.clusterVisibilityRank = clusterVisibilityRank;
    }
}
