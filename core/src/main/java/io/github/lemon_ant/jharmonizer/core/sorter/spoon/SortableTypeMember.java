package io.github.lemon_ant.jharmonizer.core.sorter.spoon;

import static io.github.lemon_ant.jharmonizer.core.sorter.spoon.SpoonTypeMemberUtils.deriveAlphaKey;
import static io.github.lemon_ant.jharmonizer.core.sorter.spoon.SpoonTypeMemberUtils.deriveAlphaSortingRank;
import static io.github.lemon_ant.jharmonizer.core.sorter.spoon.SpoonTypeMemberUtils.deriveVisibilityRank;
import static io.github.lemon_ant.jharmonizer.core.sorter.spoon.SpoonTypeMemberUtils.extractSrcStart;

import io.github.lemon_ant.jharmonizer.core.sorter.spoon.dependency_graph.SpoonJavaBeansAccessorUtils;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NonNull;
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
     * An immutable key used to compare {@link SortableTypeMember} instances.
     */
    @Value
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    static class OrderingKey {

        /**
         * Creates a memoizing provider that maps each {@link CtTypeMember} to its {@link OrderingKey}.
         *
         * <p>This single-member overload is intended for callers that do not need accessor clustering
         * (for example, top-level types). It always derives keys with no cluster context, so each
         * member's {@link #clusterAlphaKey} equals its own {@link #alphaKey}.
         *
         * @return the ordering key provider function
         */
        @NonNull
        static Function<CtTypeMember, OrderingKey> createOrderingKeyProvider() {
            @SuppressWarnings("PMD.UseConcurrentHashMap")
            Map<CtTypeMember, OrderingKey> typeMember2OrderingKey = new HashMap<>();
            return typeMember ->
                    typeMember2OrderingKey.computeIfAbsent(typeMember, OrderingKey::deriveWithoutClustering);
        }

        /**
         * Derives an {@link OrderingKey} for each member in the given group, computing per-cluster
         * representative alpha keys when {@code keepAccessorsTogether} is {@code true}.
         *
         * <p>Conceptually, every member belongs to exactly one cluster:
         * <ul>
         *   <li>When {@code keepAccessorsTogether} is {@code true} and the member is a recognized
         *       JavaBeans accessor (see
         *       {@link SpoonJavaBeansAccessorUtils#findAccessorPropertyName}), it joins the cluster
         *       identified by the underlying property name. All accessors of the same property
         *       therefore share one cluster.</li>
         *   <li>Every other member (non-accessors, and accessors when
         *       {@code keepAccessorsTogether} is {@code false}) is its own singleton cluster.</li>
         * </ul>
         *
         * <p>The resulting {@link #clusterAlphaKey} is the minimum {@link #alphaKey} across all
         * members of a cluster. The ALPHA comparator orders members primarily by
         * {@link #clusterAlphaKey} and only secondarily by their own {@link #alphaKey}, which
         * yields a totally ordered, transitive comparator: members of the same accessor cluster
         * always appear contiguously, positioned where the cluster's alphabetically-first member
         * would sort, and non-accessors are interleaved deterministically by their own alpha key.
         *
         * @param groupMembers the members to derive ordering keys for
         * @param keepAccessorsTogether whether to cluster recognized JavaBeans accessor methods by
         *     their underlying property name
         * @return an immutable map from each input member to its derived {@link OrderingKey}
         */
        @NonNull
        @SuppressWarnings({"PMD.UseConcurrentHashMap", "PMD.AvoidInstantiatingObjectsInLoops"})
        static Map<CtTypeMember, OrderingKey> deriveAll(
                @NonNull List<? extends CtTypeMember> groupMembers, boolean keepAccessorsTogether) {
            int memberCount = groupMembers.size();
            // Capacity * 2 ensures no resize at the default 0.75 load factor.
            Map<CtTypeMember, String> memberToAlphaKey = new HashMap<>(memberCount * 2);
            Map<CtTypeMember, String> memberToPropertyName = new HashMap<>(memberCount * 2);
            Map<String, String> propertyNameToClusterAlphaKey = new HashMap<>(memberCount * 2);

            for (CtTypeMember groupMember : groupMembers) {
                String alphaKey = deriveAlphaKey(groupMember);
                memberToAlphaKey.put(groupMember, alphaKey);

                if (keepAccessorsTogether && groupMember instanceof CtMethod<?> method) {
                    SpoonJavaBeansAccessorUtils.findAccessorPropertyName(method).ifPresent(propertyName -> {
                        memberToPropertyName.put(groupMember, propertyName);
                        propertyNameToClusterAlphaKey.merge(
                                propertyName,
                                alphaKey,
                                (existing, candidate) -> existing.compareTo(candidate) <= 0 ? existing : candidate);
                    });
                }
            }

            Map<CtTypeMember, OrderingKey> memberToOrderingKey = new HashMap<>(memberCount * 2);
            for (CtTypeMember groupMember : groupMembers) {
                String alphaKey = memberToAlphaKey.get(groupMember);
                String propertyName = memberToPropertyName.get(groupMember);
                String clusterAlphaKey =
                        propertyName == null ? alphaKey : propertyNameToClusterAlphaKey.get(propertyName);
                memberToOrderingKey.put(
                        groupMember,
                        new OrderingKey(
                                extractSrcStart(groupMember),
                                alphaKey,
                                deriveAlphaSortingRank(groupMember),
                                deriveVisibilityRank(groupMember),
                                clusterAlphaKey));
            }
            return Collections.unmodifiableMap(memberToOrderingKey);
        }

        @NonNull
        private static OrderingKey deriveWithoutClustering(@NonNull CtTypeMember typeMember) {
            String alphaKey = deriveAlphaKey(typeMember);
            return new OrderingKey(
                    extractSrcStart(typeMember),
                    alphaKey,
                    deriveAlphaSortingRank(typeMember),
                    deriveVisibilityRank(typeMember),
                    alphaKey);
        }

        int srcStart;

        @NonNull
        String alphaKey;

        /**
         * Rank applied first in ALPHA ordering, before the alpha key and cluster alpha key.
         * Non-zero only for {@code CtAnonymousExecutable} (initializer blocks), which receive rank
         * {@code 1} so that they sort after all regular named members regardless of their position
         * in the source.
         */
        int alphaSortingRank;

        int visibilityRank;

        /**
         * Representative alpha key of the accessor cluster this member belongs to, used as the
         * primary key by the ALPHA comparator.
         *
         * <p>For members that participate in a multi-member accessor cluster (recognized JavaBeans
         * accessors of the same underlying property when {@code keepAccessorsTogether} is enabled),
         * this is the minimum {@link #alphaKey} across all members of that cluster. For every other
         * member it equals the member's own {@link #alphaKey}.
         *
         * <p>Using a single representative key per cluster, rather than comparing accessors by
         * their property name and non-accessors by their full alpha key, ensures the resulting
         * comparator is total and transitive: it is impossible for any three members to form a
         * comparison cycle, regardless of how property names and method names interleave.
         */
        @NonNull
        String clusterAlphaKey;
    }
}
