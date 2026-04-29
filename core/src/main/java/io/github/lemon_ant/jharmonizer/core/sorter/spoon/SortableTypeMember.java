// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
package io.github.lemon_ant.jharmonizer.core.sorter.spoon;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.Value;
import spoon.reflect.declaration.CtTypeMember;

/**
 * A sortable wrapper around a Spoon {@code CtTypeMember} that carries the member's own
 * {@link OrderingKey} together with two levels of <em>representative</em> ordering keys.
 *
 * <p>Two representative levels are needed to model JavaBeans accessor super-cluster ordering:
 * <ul>
 *   <li>{@link #superClusterRepresentativeKey} — used when the member's accessor super-cluster as
 *       a whole must be compared against a non-accessor. For non-accessor members this is the
 *       member's own key (self-reference). For every accessor member of a multi-member super-cluster
 *       this is a shared reference to the super-cluster's <em>top accessor method</em>'s own key,
 *       so the super-cluster sorts as if it sat at its top method's source position / visibility /
 *       method-signature alpha key.</li>
 *   <li>{@link #propertyClusterRepresentativeKey} — used to order accessor property clusters
 *       relative to one another inside the super-cluster. For non-accessor members this is the
 *       member's own key (self-reference). For accessor members this is a synthetic
 *       {@link OrderingKey} whose {@code alphaKey} is the JavaBeans property name and whose
 *       {@code srcStart} / {@code visibilityRank} come from that property cluster's top member.
 *       Members of the same property cluster share the same instance.</li>
 * </ul>
 *
 * <p>The {@link SortableTypeMember} comparator built by
 * {@link ComparatorUtils#buildSortableTypeMemberComparator(java.util.List)} dispatches in this
 * order:
 * <ol>
 *   <li>If the two members' super-cluster representatives differ by reference, sort decision is
 *       made on the super-cluster representatives (handles accessor-vs-non-accessor pairs and
 *       uses real method-signature alpha keys).</li>
 *   <li>Otherwise, if the two property-cluster representatives differ by reference, sort decision
 *       is made on the property-cluster representatives (handles accessor pairs of different
 *       property clusters; ALPHA naturally sorts by property name).</li>
 *   <li>Otherwise both members belong to the same property cluster and the decision is made on
 *       their own keys.</li>
 * </ol>
 */
@Value
@AllArgsConstructor(access = AccessLevel.PACKAGE)
class SortableTypeMember {

    @NonNull
    CtTypeMember typeMember;

    @NonNull
    OrderingKey ownKey;

    @NonNull
    OrderingKey propertyClusterRepresentativeKey;

    @NonNull
    OrderingKey superClusterRepresentativeKey;

    @Override
    public String toString() {
        return "member=" + describeTypeMember(typeMember)
                + ", ownKey=" + ownKey
                + ", propertyClusterRepresentativeKey=" + propertyClusterRepresentativeKey
                + ", superClusterRepresentativeKey=" + superClusterRepresentativeKey;
    }

    @NonNull
    private static String describeTypeMember(CtTypeMember typeMember) {
        return typeMember.getClass().getSimpleName() + "@" + System.identityHashCode(typeMember);
    }

    /**
     * Immutable sort key carrying the source-level attributes a comparator may use to order
     * type members or accessor clusters: source position, alpha key, alpha sorting rank and
     * visibility rank.
     *
     * <p>Two flavours of {@link OrderingKey} coexist by intent — the comparator does not need
     * to distinguish them and treats every {@link OrderingKey} the same way:
     * <ul>
     *   <li>An <em>own</em> key derived directly from a single
     *       {@link spoon.reflect.declaration.CtTypeMember} (its source position, its
     *       method/field signature alpha key, etc.).</li>
     *   <li>A <em>representative</em> key for an accessor cluster: a synthetic instance whose
     *       {@code alphaKey} is the JavaBeans property name and whose {@code srcStart} /
     *       {@code visibilityRank} are taken from the cluster's top member. This makes
     *       cluster-vs-cluster comparisons sort by property name, while cluster-vs-anything-else
     *       comparisons use the top member's source position and visibility.</li>
     * </ul>
     *
     * <p>Each member that does not belong to a multi-member accessor cluster uses its own key as
     * its representative (same instance), so cluster handling is uniform: the
     * {@link SortableTypeMember} comparator simply compares representative references and falls
     * back to own keys when the representatives are the same instance.
     */
    @Value
    @AllArgsConstructor(access = AccessLevel.PACKAGE)
    static class OrderingKey {

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
}
