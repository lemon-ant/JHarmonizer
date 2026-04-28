package io.github.lemon_ant.jharmonizer.core.sorter.spoon;

import edu.umd.cs.findbugs.annotations.Nullable;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.Value;
import spoon.reflect.declaration.CtTypeMember;

/**
 * A sortable wrapper around a Spoon {@code CtTypeMember} that caches the ordering key
 * used to compare and sort members within a member group.
 */
@Value
@AllArgsConstructor(access = AccessLevel.PACKAGE)
class SortableTypeMember {

    @NonNull
    CtTypeMember typeMember;

    @NonNull
    OrderingKey orderingKey;

    @Override
    public String toString() {
        return "member=" + describeTypeMember(typeMember) + ", orderingKey=" + orderingKey;
    }

    @NonNull
    private static String describeTypeMember(CtTypeMember typeMember) {
        return typeMember.getClass().getSimpleName() + "@" + System.identityHashCode(typeMember);
    }

    /**
     * An immutable key used to compare {@link SortableTypeMember} instances.
     *
     * <p>Each accessor method that participates in the JavaBeans accessor super-cluster (when
     * {@code keepAccessorsTogether} is enabled) carries a non-null {@link #propertyName} that
     * identifies its property cluster, plus two "cluster" representative attributes derived
     * from the cluster's <em>top member</em> (the one that sorts first inside the cluster under
     * the configured ordering rules applied to member-own keys):
     * {@link #clusterSrcStart} and {@link #clusterVisibilityRank}.
     * For non-accessors and for accessors when clustering is disabled, {@link #propertyName} is
     * {@code null} and the cluster attributes equal the member's own attributes.
     *
     * <p>The comparator built by {@link ComparatorUtils#buildOrderingComparator(List)} substitutes
     * cluster attributes for the member's own attributes <em>only</em> when both compared
     * {@link OrderingKey}s are accessors of <em>different</em> property clusters; in that case the
     * ALPHA rule additionally compares {@link #propertyName} instead of the full method-signature
     * {@link #alphaKey}. In every other case (same-cluster, or at least one non-accessor) the
     * member's own attributes are used. This guarantees a totally-ordered, transitive comparator
     * and ensures non-accessor methods can never sort between two property clusters of the
     * accessor super-cluster.
     */
    @Value
    @AllArgsConstructor(access = AccessLevel.PACKAGE)
    static class OrderingKey {

        int srcStart;

        @NonNull
        String alphaKey;

        /**
         * Rank applied first in the ALPHA rule, before the alpha key.
         * Non-zero only for {@code CtAnonymousExecutable} (initializer blocks), which receive rank
         * {@code 1} so that they sort after all regular named members regardless of their position
         * in the source.
         */
        int alphaSortingRank;

        int visibilityRank;

        /**
         * The JavaBeans property name identifying this member's accessor cluster, or {@code null}
         * when the member is not a recognized accessor or accessor clustering is disabled. Two
         * members share a cluster iff both have a non-null {@link #propertyName} and the names
         * are equal. The cluster-aware comparator uses {@link #propertyName} as the ALPHA key
         * when comparing accessors of <em>different</em> property clusters.
         */
        @Nullable
        String propertyName;

        /**
         * Source-start position of the cluster's top member; equals {@link #srcStart} when this
         * member is not part of a multi-member accessor cluster. Used by the PRESERVE rule when
         * the cluster-aware comparator detects a cross-cluster accessor pair.
         */
        int clusterSrcStart;

        /**
         * Visibility rank of the cluster's top member; equals {@link #visibilityRank} when this
         * member is not part of a multi-member accessor cluster. Used by the
         * {@code VISIBILITY_*} rules when the cluster-aware comparator detects a cross-cluster
         * accessor pair.
         */
        int clusterVisibilityRank;
    }
}
