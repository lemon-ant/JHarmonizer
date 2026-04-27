package io.github.lemon_ant.jharmonizer.core.sorter.spoon;

import static io.github.lemon_ant.jharmonizer.core.sorter.spoon.SpoonTypeMemberUtils.deriveAlphaKey;
import static io.github.lemon_ant.jharmonizer.core.sorter.spoon.SpoonTypeMemberUtils.deriveAlphaSortingRank;
import static io.github.lemon_ant.jharmonizer.core.sorter.spoon.SpoonTypeMemberUtils.deriveVisibilityRank;
import static io.github.lemon_ant.jharmonizer.core.sorter.spoon.SpoonTypeMemberUtils.extractSrcStart;

import edu.umd.cs.findbugs.annotations.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
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
     */
    @Value
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    static class OrderingKey {

        /**
         * Returns a memoizing provider that maps each {@link CtTypeMember} to its {@link OrderingKey}
         * with no cluster property name. Intended for contexts where accessor bundling is not applied,
         * such as top-level type ordering.
         *
         * @return the ordering key provider function
         */
        @NonNull
        static Function<CtTypeMember, OrderingKey> getOrderingKeyProvider() {
            @SuppressWarnings("PMD.UseConcurrentHashMap")
            Map<CtTypeMember, OrderingKey> typeMember2OrderingKey = new HashMap<>();
            return typeMember -> typeMember2OrderingKey.computeIfAbsent(typeMember, member -> derive(member, null));
        }

        /**
         * Derives an {@link OrderingKey} for the given type member, optionally annotated with a
         * cluster property name for accessor-bundle ordering.
         *
         * @param typeMember the type member to derive a key for
         * @param clusterPropertyName the JavaBeans property name of the accessor cluster, or
         *     {@code null} when the member is not part of a cluster
         * @return the derived ordering key
         */
        @NonNull
        static OrderingKey derive(CtTypeMember typeMember, @Nullable String clusterPropertyName) {
            return new OrderingKey(
                    extractSrcStart(typeMember),
                    deriveAlphaKey(typeMember),
                    deriveAlphaSortingRank(typeMember),
                    deriveVisibilityRank(typeMember),
                    clusterPropertyName);
        }

        int srcStart;

        @NonNull
        String alphaKey;

        /**
         * Secondary rank applied before the alpha key in ALPHA ordering. Non-zero only for
         * {@code CtAnonymousExecutable} (initializer blocks), which receive rank {@code 1} so that
         * they sort after all regular named members regardless of their position in the source.
         */
        int alphaSortingRank;

        int visibilityRank;

        /**
         * The JavaBeans property name of the accessor cluster this member belongs to, or {@code null}
         * when the member is not part of an accessor bundle. When non-null and the compared member
         * also belongs to a cluster, the ALPHA comparator uses this name for cluster-vs-cluster
         * comparison so that accessor clusters sort by property name rather than by the method-name
         * prefix ({@code get/is/has}).
         */
        @Nullable
        String clusterPropertyName;
    }
}
