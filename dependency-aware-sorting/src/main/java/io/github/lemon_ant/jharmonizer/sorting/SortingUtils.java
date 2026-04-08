package io.github.lemon_ant.jharmonizer.sorting;

import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.util.List;
import lombok.NonNull;
import lombok.experimental.UtilityClass;

/**
 * Shared validation and index-building utilities for sorting algorithms.
 */
@UtilityClass
@SuppressWarnings({"PMD.LooseCoupling", "PMD.UseVarargs"})
class SortingUtils {

    static final int UNASSIGNED = -1;

    /**
     * Builds a boxing-free map from each item to its list index, detecting duplicates.
     *
     * <p>Uses {@link Object2IntOpenHashMap} to store primitive {@code int} values without
     * boxing to {@link Integer}, improving lookup performance in hot paths.</p>
     *
     * @param items the item list
     * @param <TSortableItem> the item type
     * @return boxing-free map from item to index
     * @throws SortingException if a duplicate item is found
     */
    @NonNull
    static <TSortableItem> Object2IntOpenHashMap<TSortableItem> buildItemIndex(@NonNull List<TSortableItem> items) {
        Object2IntOpenHashMap<TSortableItem> index = new Object2IntOpenHashMap<>(items.size(), 0.5f);
        index.defaultReturnValue(UNASSIGNED);
        for (int i = 0; i < items.size(); i++) {
            TSortableItem item = items.get(i);
            if (index.putIfAbsent(item, i) != UNASSIGNED) {
                throw new SortingException("Duplicate item: " + item);
            }
        }
        return index;
    }

    /**
     * Resolves a group member's index, validating it exists in the item list.
     *
     * @param itemToIndex the item-to-index map
     * @param member the group member to resolve
     * @param <TSortableItem> the item type
     * @return the member's index
     * @throws SortingException if the member is unknown
     */
    static <TSortableItem> int resolveGroupMemberIndex(
            @NonNull Object2IntOpenHashMap<TSortableItem> itemToIndex, TSortableItem member) {
        int idx = itemToIndex.getInt(member);
        if (idx == UNASSIGNED) {
            throw new SortingException("Group references unknown member: " + member);
        }
        return idx;
    }

    /**
     * Validates that a member is not already assigned to a super-node (i.e. not in another group).
     *
     * @param currentSuperNode the member's current super-node assignment
     * @param member the member being validated
     * @throws SortingException if the member is already grouped
     */
    static void validateNotAlreadyGrouped(int currentSuperNode, Object member) {
        if (currentSuperNode != UNASSIGNED) {
            throw new SortingException("Member " + member + " appears in more than one group");
        }
    }

    /**
     * Resolves a dependency edge's provider and dependent indices, validating that both
     * endpoints exist and are not self-referencing. Returns indices via the provided
     * two-element output array to avoid per-edge object allocation.
     *
     * @param edge the dependency edge
     * @param itemToIndex the item-to-index map
     * @param outIndices two-element array: {@code [0]} = provider index, {@code [1]} = dependent index
     * @param <TSortableItem> the item type
     * @throws SortingException if an endpoint is unknown or the edge is a self-dependency
     */
    static <TSortableItem> void resolveDependencyEdge(
            @NonNull Dependencies.Dependency<TSortableItem> edge,
            @NonNull Object2IntOpenHashMap<TSortableItem> itemToIndex,
            int[] outIndices) {
        TSortableItem provider = edge.getProvider();
        TSortableItem dependent = edge.getDependent();

        int providerIdx = itemToIndex.getInt(provider);
        if (providerIdx == UNASSIGNED) {
            throw new SortingException("Dependency references unknown provider: " + provider);
        }
        int dependentIdx = itemToIndex.getInt(dependent);
        if (dependentIdx == UNASSIGNED) {
            throw new SortingException("Dependency references unknown dependent: " + dependent);
        }
        if (providerIdx == dependentIdx) {
            throw new SortingException("Self-dependency: " + provider);
        }
        outIndices[0] = providerIdx;
        outIndices[1] = dependentIdx;
    }
}
