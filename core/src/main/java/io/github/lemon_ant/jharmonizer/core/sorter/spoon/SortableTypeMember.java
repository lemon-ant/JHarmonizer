package io.github.lemon_ant.jharmonizer.core.sorter.spoon;

import static io.github.lemon_ant.jharmonizer.core.sorter.spoon.SpoonTypeMemberUtils.deriveAlphaKey;
import static io.github.lemon_ant.jharmonizer.core.sorter.spoon.SpoonTypeMemberUtils.deriveAlphaSortingRank;
import static io.github.lemon_ant.jharmonizer.core.sorter.spoon.SpoonTypeMemberUtils.deriveVisibilityRank;
import static io.github.lemon_ant.jharmonizer.core.sorter.spoon.SpoonTypeMemberUtils.extractSrcStart;

import edu.umd.cs.findbugs.annotations.Nullable;
import io.github.lemon_ant.jharmonizer.core.config.compiled.OrderingRule;
import io.github.lemon_ant.jharmonizer.core.sorter.spoon.dependency_graph.SpoonJavaBeansAccessorUtils;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
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
     *
     * <p>Each accessor method that participates in the JavaBeans accessor super-cluster (when
     * {@code keepAccessorsTogether} is enabled) carries a non-null {@link #propertyName} that
     * identifies its property cluster, plus three "cluster" representative attributes derived
     * from the cluster's <em>top member</em> (the one that sorts first inside the cluster under
     * the configured ordering rules applied to member-own keys):
     * {@link #clusterSrcStart}, {@link #clusterAlphaSortingRank}, {@link #clusterVisibilityRank}.
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
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    static class OrderingKey {

        /**
         * Creates a memoizing provider that maps each {@link CtTypeMember} to its {@link OrderingKey}.
         *
         * <p>This single-member overload is intended for callers that do not need accessor clustering
         * (for example, top-level types). It always derives keys with no cluster context, so each
         * member's cluster attributes equal its own attributes and {@link #propertyName} is
         * {@code null}.
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
         * Derives an {@link OrderingKey} for each member in the given group.
         *
         * <p>When {@code keepAccessorsTogether} is {@code true}, every recognized JavaBeans accessor
         * method (see {@link SpoonJavaBeansAccessorUtils#findAccessorPropertyName}) is assigned a
         * non-null {@link #propertyName}. All accessors sharing the same property name form one
         * cluster. For each cluster the <em>top member</em> is chosen as the minimum under the
         * member-only comparator built from {@code orderingRules}, and its
         * {@link #srcStart} / {@link #alphaSortingRank} / {@link #visibilityRank} become the
         * cluster's representative attributes for every member of that cluster. For non-accessor
         * members and for accessors when clustering is disabled, {@link #propertyName} is
         * {@code null} and cluster attributes equal the member's own attributes.
         *
         * <p>The downstream cluster-aware comparator (see
         * {@link ComparatorUtils#buildOrderingComparator(List)}) uses these cluster attributes
         * <em>only</em> when comparing two accessors of different property clusters; in that
         * case ALPHA additionally compares {@link #propertyName} instead of the full method
         * {@link #alphaKey}. All other comparisons use member-own attributes.
         *
         * @param groupMembers the members to derive ordering keys for
         * @param keepAccessorsTogether whether to cluster recognized JavaBeans accessor methods
         * @param orderingRules the configured ordering rules used to choose each cluster's top
         *     member; the same rules drive the final comparator (with cross-cluster substitutions)
         * @return an immutable map from each input member to its derived {@link OrderingKey}
         */
        @NonNull
        @SuppressWarnings({"PMD.UseConcurrentHashMap", "PMD.AvoidInstantiatingObjectsInLoops"})
        static Map<CtTypeMember, OrderingKey> deriveAll(
                @NonNull List<? extends CtTypeMember> groupMembers,
                boolean keepAccessorsTogether,
                @NonNull List<OrderingRule> orderingRules) {
            int memberCount = groupMembers.size();
            // Capacity * 2 ensures no resize at the default 0.75 load factor.
            Map<CtTypeMember, OrderingKey> memberToOwnKey = new HashMap<>(memberCount * 2);
            Map<String, List<CtTypeMember>> propertyToMembers = new HashMap<>(memberCount * 2);

            for (CtTypeMember groupMember : groupMembers) {
                String propertyName = null;
                if (keepAccessorsTogether && groupMember instanceof CtMethod<?> method) {
                    propertyName = SpoonJavaBeansAccessorUtils.findAccessorPropertyName(method)
                            .orElse(null);
                }
                OrderingKey ownKey = deriveWithoutClustering(groupMember, propertyName);
                memberToOwnKey.put(groupMember, ownKey);
                if (propertyName != null) {
                    propertyToMembers
                            .computeIfAbsent(propertyName, ignored -> new ArrayList<>())
                            .add(groupMember);
                }
            }

            // Discover the top member of every cluster using the member-only comparator,
            // i.e., the configured comparator with cross-cluster substitutions disabled.
            // Within a single cluster every member shares the same propertyName, so the
            // cross-cluster guard is naturally false for any pair, but we use the explicit
            // member-only variant so this method is independent of the cluster-key wiring.
            Comparator<OrderingKey> memberOnlyComparator =
                    ComparatorUtils.buildMemberOnlyOrderingComparator(orderingRules);
            Map<String, OrderingKey> propertyToTopMemberKey = new HashMap<>(propertyToMembers.size() * 2);
            for (Map.Entry<String, List<CtTypeMember>> clusterEntry : propertyToMembers.entrySet()) {
                OrderingKey topMemberKey = clusterEntry.getValue().stream()
                        .map(memberToOwnKey::get)
                        .min(memberOnlyComparator)
                        .orElseThrow(() -> new IllegalStateException(
                                "Empty accessor cluster for property: " + clusterEntry.getKey()));
                propertyToTopMemberKey.put(clusterEntry.getKey(), topMemberKey);
            }

            Map<CtTypeMember, OrderingKey> memberToOrderingKey = new HashMap<>(memberCount * 2);
            for (CtTypeMember groupMember : groupMembers) {
                OrderingKey ownKey = memberToOwnKey.get(groupMember);
                String propertyName = ownKey.getPropertyName();
                OrderingKey clusterTopKey = propertyName == null ? ownKey : propertyToTopMemberKey.get(propertyName);
                memberToOrderingKey.put(
                        groupMember,
                        new OrderingKey(
                                ownKey.getSrcStart(),
                                ownKey.getAlphaKey(),
                                ownKey.getAlphaSortingRank(),
                                ownKey.getVisibilityRank(),
                                propertyName,
                                clusterTopKey.getSrcStart(),
                                clusterTopKey.getAlphaSortingRank(),
                                clusterTopKey.getVisibilityRank()));
            }
            return Collections.unmodifiableMap(memberToOrderingKey);
        }

        @NonNull
        private static OrderingKey deriveWithoutClustering(@NonNull CtTypeMember typeMember) {
            return deriveWithoutClustering(typeMember, null);
        }

        @NonNull
        private static OrderingKey deriveWithoutClustering(
                @NonNull CtTypeMember typeMember, @Nullable String propertyName) {
            int srcStart = extractSrcStart(typeMember);
            int alphaSortingRank = deriveAlphaSortingRank(typeMember);
            int visibilityRank = deriveVisibilityRank(typeMember);
            return new OrderingKey(
                    srcStart,
                    deriveAlphaKey(typeMember),
                    alphaSortingRank,
                    visibilityRank,
                    propertyName,
                    srcStart,
                    alphaSortingRank,
                    visibilityRank);
        }

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
         * Alpha sorting rank of the cluster's top member; equals {@link #alphaSortingRank} when
         * this member is not part of a multi-member accessor cluster. Used by the ALPHA rule
         * when the cluster-aware comparator detects a cross-cluster accessor pair.
         */
        int clusterAlphaSortingRank;

        /**
         * Visibility rank of the cluster's top member; equals {@link #visibilityRank} when this
         * member is not part of a multi-member accessor cluster. Used by the
         * {@code VISIBILITY_*} rules when the cluster-aware comparator detects a cross-cluster
         * accessor pair.
         */
        int clusterVisibilityRank;
    }
}
