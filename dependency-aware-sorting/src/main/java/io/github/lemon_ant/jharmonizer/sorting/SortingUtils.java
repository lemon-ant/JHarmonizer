package io.github.lemon_ant.jharmonizer.sorting;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.NonNull;
import lombok.Value;
import lombok.experimental.UtilityClass;

/**
 * Shared validation and index-building utilities for sorting algorithms.
 */
@UtilityClass
class SortingUtils {

    static final int UNASSIGNED = -1;

    /**
     * Builds a map from each item to its list index, detecting duplicates.
     *
     * @param items the item list
     * @param <TSortableItem> the item type
     * @return unmodifiable map from item to index
     * @throws SortingException if a duplicate item is found
     */
    @NonNull
    @SuppressWarnings("PMD.UseConcurrentHashMap")
    static <TSortableItem> Map<TSortableItem, Integer> buildItemIndex(@NonNull List<TSortableItem> items) {
        Map<TSortableItem, Integer> index = new HashMap<>(items.size() * 2);
        for (int i = 0; i < items.size(); i++) {
            TSortableItem item = items.get(i);
            if (index.put(item, i) != null) {
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
            @NonNull Map<TSortableItem, Integer> itemToIndex, TSortableItem member) {
        Integer idx = itemToIndex.get(member);
        if (idx == null) {
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
     * Resolves and validates a dependency edge. Checks that both endpoints exist and are not self-referencing.
     *
     * @param edge the dependency edge
     * @param itemToIndex the item-to-index map
     * @param <TSortableItem> the item type
     * @return the resolved edge with provider and dependent indices
     * @throws SortingException if an endpoint is unknown or the edge is a self-dependency
     */
    @NonNull
    static <TSortableItem> ResolvedEdge<TSortableItem> resolveDependencyEdge(
            @NonNull Dependencies.Dependency<TSortableItem> edge, @NonNull Map<TSortableItem, Integer> itemToIndex) {
        TSortableItem provider = edge.getProvider();
        TSortableItem dependent = edge.getDependent();

        Integer providerIdx = itemToIndex.get(provider);
        if (providerIdx == null) {
            throw new SortingException("Dependency references unknown provider: " + provider);
        }
        Integer dependentIdx = itemToIndex.get(dependent);
        if (dependentIdx == null) {
            throw new SortingException("Dependency references unknown dependent: " + dependent);
        }
        if (providerIdx.equals(dependentIdx)) {
            throw new SortingException("Self-dependency: " + provider);
        }
        return new ResolvedEdge<>(provider, providerIdx, dependent, dependentIdx);
    }

    /**
     * A resolved dependency edge with both item references and their indices.
     *
     * @param <TSortableItem> the item type
     */
    @Value
    static class ResolvedEdge<TSortableItem> {

        @NonNull
        TSortableItem provider;

        int providerIndex;

        @NonNull
        TSortableItem dependent;

        int dependentIndex;
    }
}
