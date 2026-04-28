// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
package io.github.lemon_ant.jharmonizer.core.sorter.spoon;

import static io.github.lemon_ant.jharmonizer.core.sorter.spoon.SpoonTypeMemberUtils.deriveAlphaKey;
import static io.github.lemon_ant.jharmonizer.core.sorter.spoon.SpoonTypeMemberUtils.deriveAlphaSortingRank;
import static io.github.lemon_ant.jharmonizer.core.sorter.spoon.SpoonTypeMemberUtils.deriveSrcStart;
import static io.github.lemon_ant.jharmonizer.core.sorter.spoon.SpoonTypeMemberUtils.deriveVisibilityRank;
import static java.util.Objects.requireNonNull;

import io.github.lemon_ant.jharmonizer.core.sorter.spoon.dependency_graph.SpoonJavaBeansAccessorUtils;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;
import lombok.Value;
import spoon.reflect.declaration.CtMethod;
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
     * Finds the JavaBeans accessor property name for a member when it is a recognized accessor method.
     *
     * @param typeMember the type member to inspect
     * @return the accessor property name, or empty when the member is not a recognized accessor
     */
    @NonNull
    static Optional<String> findAccessorPropertyName(@NonNull CtTypeMember typeMember) {
        return typeMember instanceof CtMethod<?> method
                ? SpoonJavaBeansAccessorUtils.findAccessorPropertyName(method)
                : Optional.empty();
    }

    /**
     * An immutable key used to compare {@link SortableTypeMember} instances.
     */
    @Getter
    @EqualsAndHashCode
    @ToString
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    static class OrderingKey {

        /**
         * Creates a memoizing provider that maps each {@link CtTypeMember} to its {@link OrderingKey}.
         *
         * @return the ordering key provider function
         */
        @NonNull
        static Function<CtTypeMember, OrderingKey> createOrderingKeyProvider() {
            @SuppressWarnings("PMD.UseConcurrentHashMap")
            Map<CtTypeMember, OrderingKey> typeMember2OrderingKey = new HashMap<>();
            return typeMember -> typeMember2OrderingKey.computeIfAbsent(typeMember, OrderingKey::derive);
        }

        /**
         * Derives an {@link OrderingKey} for the given type member.
         *
         * @param typeMember the type member to derive a key for
         * @return the derived ordering key
         */
        @NonNull
        static OrderingKey derive(@NonNull CtTypeMember typeMember) {
            return new OrderingKey(
                    deriveSrcStart(typeMember),
                    deriveAlphaKey(typeMember),
                    deriveAlphaSortingRank(typeMember),
                    deriveVisibilityRank(typeMember));
        }

        final int srcStart;

        @NonNull
        final String alphaKey;

        /**
         * Rank applied first in ALPHA ordering, before the alpha key and cluster property name.
         * Non-zero only for {@code CtAnonymousExecutable} (initializer blocks), which receive rank
         * {@code 1} so that they sort after all regular named members regardless of their position
         * in the source.
         */
        final int alphaSortingRank;

        final int visibilityRank;

        /**
         * Returns a clustered copy represented by the given top accessor member.
         *
         * @param propertyRepresentativeOrderingKey the top method key for the accessor property cluster
         * @param superClusterRepresentativeOrderingKey the top method key for the whole accessor super-cluster
         * @param propertyName the property name used when comparing different accessor property clusters
         * @return the clustered ordering key
         */
        @NonNull
        AccessorClusterOrderingKey resolveWithAccessorClusterRepresentative(
                @NonNull OrderingKey propertyRepresentativeOrderingKey,
                @NonNull OrderingKey superClusterRepresentativeOrderingKey,
                @NonNull String propertyName) {
            return new AccessorClusterOrderingKey(
                    srcStart,
                    alphaKey,
                    alphaSortingRank,
                    visibilityRank,
                    propertyName,
                    superClusterRepresentativeOrderingKey.getSrcStart(),
                    superClusterRepresentativeOrderingKey.getAlphaKey(),
                    superClusterRepresentativeOrderingKey.getVisibilityRank(),
                    propertyRepresentativeOrderingKey.getSrcStart(),
                    propertyRepresentativeOrderingKey.getVisibilityRank());
        }

        /**
         * Resolves the source-start key to use when comparing accessor clusters against other members.
         *
         * @return the effective source-start key
         */
        int resolveClusterSrcStart() {
            return srcStart;
        }

        /**
         * Resolves the alpha key to use when comparing accessor clusters against other members.
         *
         * @return the effective alpha key
         */
        @NonNull
        String resolveClusterAlphaKey() {
            return alphaKey;
        }

        /**
         * Resolves the alpha key to use when comparing different accessor property clusters.
         *
         * @return the accessor property alpha key
         */
        @NonNull
        String resolveAccessorPropertyAlphaKey() {
            return alphaKey;
        }

        /**
         * Resolves the visibility key to use when comparing accessor clusters against other members.
         *
         * @return the effective visibility key
         */
        int resolveClusterVisibilityRank() {
            return visibilityRank;
        }

        /**
         * Resolves the source-start key to use when comparing different accessor property clusters.
         *
         * @return the accessor property source-start key
         */
        int resolveAccessorPropertySrcStart() {
            return srcStart;
        }

        /**
         * Resolves the visibility key to use when comparing different accessor property clusters.
         *
         * @return the accessor property visibility key
         */
        int resolveAccessorPropertyVisibilityRank() {
            return visibilityRank;
        }

        /**
         * An {@link OrderingKey} with complete accessor-cluster metadata.
         */
        @Getter
        @EqualsAndHashCode(callSuper = true)
        @ToString(callSuper = true)
        static final class AccessorClusterOrderingKey extends OrderingKey {

            @NonNull
            final String clusterPropertyName;

            final int clusterSrcStart;

            @NonNull
            final String clusterAlphaKey;

            final int clusterVisibilityRank;

            final int propertyClusterSrcStart;

            final int propertyClusterVisibilityRank;

            private AccessorClusterOrderingKey(
                    int srcStart,
                    String alphaKey,
                    int alphaSortingRank,
                    int visibilityRank,
                    String clusterPropertyName,
                    int clusterSrcStart,
                    String clusterAlphaKey,
                    int clusterVisibilityRank,
                    int propertyClusterSrcStart,
                    int propertyClusterVisibilityRank) {
                super(srcStart, alphaKey, alphaSortingRank, visibilityRank);
                this.clusterPropertyName = requireNonNull(clusterPropertyName, "clusterPropertyName");
                this.clusterSrcStart = clusterSrcStart;
                this.clusterAlphaKey = requireNonNull(clusterAlphaKey, "clusterAlphaKey");
                this.clusterVisibilityRank = clusterVisibilityRank;
                this.propertyClusterSrcStart = propertyClusterSrcStart;
                this.propertyClusterVisibilityRank = propertyClusterVisibilityRank;
            }

            @Override
            int resolveClusterSrcStart() {
                return clusterSrcStart;
            }

            @Override
            @NonNull
            String resolveClusterAlphaKey() {
                return clusterAlphaKey;
            }

            @Override
            @NonNull
            String resolveAccessorPropertyAlphaKey() {
                return clusterPropertyName;
            }

            @Override
            int resolveClusterVisibilityRank() {
                return clusterVisibilityRank;
            }

            @Override
            int resolveAccessorPropertySrcStart() {
                return propertyClusterSrcStart;
            }

            @Override
            int resolveAccessorPropertyVisibilityRank() {
                return propertyClusterVisibilityRank;
            }
        }
    }
}
