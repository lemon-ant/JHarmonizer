// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
package io.github.lemon_ant.jharmonizer.core.sorter.spoon;

import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;
import lombok.experimental.FieldDefaults;

/**
 * Extended ordering key that inherits {@link MemberOrderingKey}'s member-own attributes and
 * additionally carries JavaBeans accessor cluster information for a recognized accessor member.
 *
 * <p>A {@link ClusteredOrderingKey} is only created for members that are recognized as JavaBeans
 * accessors when {@code keepAccessorsTogether} is enabled. Non-accessor members and accessors
 * when clustering is disabled keep a plain {@link MemberOrderingKey}.
 *
 * <p>{@link #propertyName} identifies the property cluster this accessor belongs to.
 * The cluster-representative attributes ({@link #clusterSrcStart},
 * {@link #clusterVisibilityRank}) reflect the <em>top member</em> of the property cluster
 * (the one that sorts first under the member-only comparator). Two accessors share a cluster
 * iff their {@link #propertyName}s are equal.
 *
 * <p>The cluster-aware comparator ({@link ComparatorUtils#buildClusteredOrderingComparator(java.util.List)})
 * substitutes cluster attributes for own attributes only when comparing two accessors of
 * different property clusters ({@link ComparatorUtils#isCrossCluster}).
 */
@Getter
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
class ClusteredOrderingKey extends MemberOrderingKey {

    /**
     * The JavaBeans property name identifying this member's accessor cluster. Two members share
     * a cluster iff their property names are equal. The cluster-aware comparator uses this name
     * as the ALPHA key when comparing accessors of different property clusters.
     */
    @NonNull
    String propertyName;

    /**
     * Source-start position of the cluster's top member. Used by the PRESERVE rule when the
     * cluster-aware comparator detects a cross-cluster accessor pair.
     */
    int clusterSrcStart;

    /**
     * Visibility rank of the cluster's top member. Used by the {@code VISIBILITY_*} rules when
     * the cluster-aware comparator detects a cross-cluster accessor pair.
     */
    int clusterVisibilityRank;

    /**
     * Creates a new clustered ordering key for a recognized accessor member.
     *
     * @param srcStart source-start position of this member
     * @param alphaKey alphabetical sort key for this member
     * @param alphaSortingRank rank used before the alpha key in ALPHA comparisons
     * @param visibilityRank visibility rank for this member
     * @param propertyName JavaBeans property name identifying this member's accessor cluster
     * @param clusterSrcStart source-start position of the cluster's top member
     * @param clusterVisibilityRank visibility rank of the cluster's top member
     */
    ClusteredOrderingKey(
            int srcStart,
            @NonNull String alphaKey,
            int alphaSortingRank,
            int visibilityRank,
            @NonNull String propertyName,
            int clusterSrcStart,
            int clusterVisibilityRank) {
        super(srcStart, alphaKey, alphaSortingRank, visibilityRank);
        this.propertyName = propertyName;
        this.clusterSrcStart = clusterSrcStart;
        this.clusterVisibilityRank = clusterVisibilityRank;
    }
}
