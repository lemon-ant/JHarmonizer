package io.github.lemon_ant.jharmonizer.core.sorter.spoon;

import static io.github.lemon_ant.jharmonizer.core.sorter.spoon.SpoonTypeMemberUtils.deriveAlphaKey;
import static io.github.lemon_ant.jharmonizer.core.sorter.spoon.SpoonTypeMemberUtils.deriveAlphaSortingRank;
import static io.github.lemon_ant.jharmonizer.core.sorter.spoon.SpoonTypeMemberUtils.deriveVisibilityRank;
import static io.github.lemon_ant.jharmonizer.core.sorter.spoon.SpoonTypeMemberUtils.extractSrcStart;

import edu.umd.cs.findbugs.annotations.Nullable;
import io.github.lemon_ant.jharmonizer.core.sorter.spoon.dependency_graph.SpoonJavaBeansAccessorUtils;
import java.util.HashMap;
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
         * Returns a memoizing provider that maps each {@link CtTypeMember} to its {@link OrderingKey}.
         *
         * @param keepAccessorsTogether whether to populate {@link #clusterPropertyName} for
         *     recognized accessor methods; pass {@code false} when accessor clustering is not needed
         *     (for example, when ordering top-level types)
         * @return the ordering key provider function
         */
        @NonNull
        static Function<CtTypeMember, OrderingKey> getOrderingKeyProvider(boolean keepAccessorsTogether) {
            @SuppressWarnings("PMD.UseConcurrentHashMap")
            Map<CtTypeMember, OrderingKey> typeMember2OrderingKey = new HashMap<>();
            return typeMember ->
                    typeMember2OrderingKey.computeIfAbsent(typeMember, m -> derive(m, keepAccessorsTogether));
        }

        /**
         * Derives an {@link OrderingKey} for the given type member.
         *
         * <p>When {@code keepAccessorsTogether} is {@code true} and the member is a method that
         * matches a JavaBeans accessor contract, the property name is extracted via
         * {@link SpoonJavaBeansAccessorUtils#findAccessorPropertyName} and stored as
         * {@link #clusterPropertyName}, so that accessor methods sort by the underlying property
         * rather than by the method-name prefix ({@code get/is/has/set}).
         *
         * <p>When {@code keepAccessorsTogether} is {@code false}, {@link #clusterPropertyName}
         * is always {@code null}, so all members sort purely by their {@link #alphaKey}.
         *
         * @param typeMember the type member to derive a key for
         * @param keepAccessorsTogether whether to populate {@link #clusterPropertyName} for
         *     recognized accessor methods
         * @return the derived ordering key
         */
        @NonNull
        static OrderingKey derive(@NonNull CtTypeMember typeMember, boolean keepAccessorsTogether) {
            String propertyName = keepAccessorsTogether && typeMember instanceof CtMethod<?> method
                    ? SpoonJavaBeansAccessorUtils.findAccessorPropertyName(method)
                            .orElse(null)
                    : null;
            return new OrderingKey(
                    extractSrcStart(typeMember),
                    deriveAlphaKey(typeMember),
                    deriveAlphaSortingRank(typeMember),
                    deriveVisibilityRank(typeMember),
                    propertyName);
        }

        int srcStart;

        @NonNull
        String alphaKey;

        /**
         * Rank applied first in ALPHA ordering, before the alpha key and cluster property name.
         * Non-zero only for {@code CtAnonymousExecutable} (initializer blocks), which receive rank
         * {@code 1} so that they sort after all regular named members regardless of their position
         * in the source.
         */
        int alphaSortingRank;

        int visibilityRank;

        /**
         * The JavaBeans property name derived from this member's accessor method signature
         * (e.g. {@code getValue} → {@code value}), or {@code null} when the member is not a
         * recognized accessor method. The ALPHA comparator uses this name when both compared members
         * expose a derived property name, so that accessor methods sort by the underlying property
         * rather than by the method-name prefix ({@code get/is/has/set}).
         */
        @Nullable
        String clusterPropertyName;
    }
}
