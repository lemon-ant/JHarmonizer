// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
package io.github.lemon_ant.jharmonizer.core.sorter.spoon;

import static io.github.lemon_ant.jharmonizer.core.sorter.spoon.SpoonTypeMemberUtils.deriveAlphaKey;
import static io.github.lemon_ant.jharmonizer.core.sorter.spoon.SpoonTypeMemberUtils.deriveAlphaSortingRank;
import static io.github.lemon_ant.jharmonizer.core.sorter.spoon.SpoonTypeMemberUtils.deriveSrcStart;
import static io.github.lemon_ant.jharmonizer.core.sorter.spoon.SpoonTypeMemberUtils.deriveVisibilityRank;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;
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

    @NonNull
    OrderingKey superClusterRepresentative;

    @NonNull
    OrderingKey propertyClusterRepresentative;

    @Override
    public String toString() {
        return "member=" + describeTypeMember(typeMember)
                + ", orderingKey=" + orderingKey
                + ", superClusterRepresentative=" + describeOrderingKey(superClusterRepresentative)
                + ", propertyClusterRepresentative=" + describeOrderingKey(propertyClusterRepresentative);
    }

    @NonNull
    private static String describeTypeMember(CtTypeMember typeMember) {
        return typeMember.getClass().getSimpleName() + "@" + System.identityHashCode(typeMember);
    }

    /**
     * Formats an ordering key with its identity hash so shared representatives are visible in diagnostics.
     *
     * @param orderingKey the ordering key to describe
     * @return the ordering key diagnostic description
     */
    @NonNull
    private static String describeOrderingKey(OrderingKey orderingKey) {
        return orderingKey + "@" + System.identityHashCode(orderingKey);
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

        /**
         * Derives a distinct accessor property representative key using the property name as its alpha key.
         *
         * @param orderingKey the top member ordering key in the property cluster
         * @param propertyName the JavaBeans property name
         * @return the property representative ordering key
         */
        @NonNull
        static OrderingKey deriveAccessorPropertyRepresentative(
                @NonNull OrderingKey orderingKey, @NonNull String propertyName) {
            return new OrderingKey(
                    orderingKey.getSrcStart(),
                    propertyName,
                    orderingKey.getAlphaSortingRank(),
                    orderingKey.getVisibilityRank());
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
    }
}
