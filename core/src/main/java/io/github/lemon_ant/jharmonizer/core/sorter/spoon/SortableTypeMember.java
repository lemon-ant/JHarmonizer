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

    /**
     * Creates a copy of the given member with its ordering key annotated with the specified accessor
     * property name. The property name is used by the ALPHA comparator when ordering accessor clusters
     * against each other: two clusters are compared by their property names, while a cluster compared
     * against a non-cluster member falls back to the method-name {@code alphaKey}.
     *
     * @param original the original sortable type member
     * @param clusterPropertyName the JavaBeans property name shared by the accessor cluster
     * @return a new sortable member with the cluster property name recorded in its ordering key
     */
    @NonNull
    static SortableTypeMember withClusterPropertyName(
            @NonNull SortableTypeMember original, @NonNull String clusterPropertyName) {
        OrderingKey baseKey = original.getOrderingKey();
        OrderingKey clusterKey = new OrderingKey(
                baseKey.getSrcStart(),
                baseKey.getAlphaKey(),
                baseKey.getAlphaSortingRank(),
                baseKey.getVisibilityRank(),
                clusterPropertyName);
        return new SortableTypeMember(original.getTypeMember(), clusterKey);
    }

    @NonNull
    private static SortableTypeMember.OrderingKey deriveOrderingKey(CtTypeMember typeMember) {
        return new SortableTypeMember.OrderingKey(
                extractSrcStart(typeMember),
                deriveAlphaKey(typeMember),
                deriveAlphaSortingRank(typeMember),
                deriveVisibilityRank(typeMember),
                null);
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
         * Returns a memoizing provider that maps each {@link CtTypeMember} to its {@link OrderingKey}.
         *
         * @return the ordering key provider function
         */
        @NonNull
        static Function<CtTypeMember, OrderingKey> getOrderingKeyProvider() {
            @SuppressWarnings("PMD.UseConcurrentHashMap")
            Map<CtTypeMember, OrderingKey> typeMember2OrderingKey = new HashMap<>();
            return typeMember ->
                    typeMember2OrderingKey.computeIfAbsent(typeMember, SortableTypeMember::deriveOrderingKey);
        }

        int srcStart;

        @NonNull
        String alphaKey;

        int alphaSortingRank;

        int visibilityRank;

        /**
         * The JavaBeans property name of the accessor cluster this member belongs to, or {@code null}
         * when the member is not part of an accessor bundle. When non-null, the ALPHA comparator uses
         * this name for cluster-vs-cluster comparison, so that accessor clusters sort by property name
         * rather than by the method-name prefix ({@code get/is/has}).
         */
        @Nullable
        String clusterPropertyName;
    }
}
