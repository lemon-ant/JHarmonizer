// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
package io.github.lemon_ant.jharmonizer.core.sorter.spoon;

import static io.github.lemon_ant.jharmonizer.core.sorter.spoon.SpoonTypeMemberUtils.deriveAlphaKey;
import static io.github.lemon_ant.jharmonizer.core.sorter.spoon.SpoonTypeMemberUtils.deriveAlphaSortingRank;
import static io.github.lemon_ant.jharmonizer.core.sorter.spoon.SpoonTypeMemberUtils.deriveSrcStart;
import static io.github.lemon_ant.jharmonizer.core.sorter.spoon.SpoonTypeMemberUtils.deriveVisibilityRank;

import io.github.lemon_ant.jharmonizer.core.sorter.spoon.SortableTypeMember.OrderingKey;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import lombok.NonNull;
import lombok.experimental.UtilityClass;
import spoon.reflect.declaration.CtTypeMember;

/**
 * Factory methods for ordering keys used by member and top-level type sorting.
 */
@UtilityClass
class OrderingKeyFactory {

    /**
     * Creates a memoizing provider that maps each {@link CtTypeMember} to its {@link OrderingKey}.
     *
     * @return the ordering key provider function
     */
    @NonNull
    static Function<CtTypeMember, OrderingKey> createOrderingKeyProvider() {
        @SuppressWarnings("PMD.UseConcurrentHashMap")
        Map<CtTypeMember, OrderingKey> typeMember2OrderingKey = new HashMap<>();
        return typeMember -> typeMember2OrderingKey.computeIfAbsent(typeMember, OrderingKeyFactory::derive);
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
}
