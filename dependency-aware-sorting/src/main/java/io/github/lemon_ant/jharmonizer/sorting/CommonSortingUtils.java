package io.github.lemon_ant.jharmonizer.sorting;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.NonNull;
import lombok.Value;
import lombok.experimental.UtilityClass;

/**
 * Shared utilities used by both {@link DependencyAwareSorter} and
 * {@link SimplifiedDependencyAwareSorter}.
 *
 * <p>Contains common validation routines, item-index building,
 * and low-level data structures that are identical across both algorithms.</p>
 */
@UtilityClass
class CommonSortingUtils {

    /** Sentinel value indicating an item has not been assigned to any super-node yet. */
    static final int UNASSIGNED = -1;

    // ------------------------------------------------------------------ //
    // Item index                                                          //
    // ------------------------------------------------------------------ //

    /**
     * Builds a mapping from each item to its positional index in the list.
     *
     * @param items           the ordered list of items
     * @param <TSortableItem> the item type
     * @return an item-to-index map
     * @throws SortingException if two items are equal (duplicates)
     */
    @NonNull
    @SuppressWarnings("PMD.UseConcurrentHashMap")
    static <TSortableItem> Map<TSortableItem, Integer> buildItemIndex(@NonNull List<TSortableItem> items) {
        Map<TSortableItem, Integer> itemToIndex = new HashMap<>(items.size() * 2);
        for (int i = 0; i < items.size(); i++) {
            TSortableItem item = items.get(i);
            if (itemToIndex.put(item, i) != null) {
                throw new SortingException("Duplicate item: \"" + item + "\"");
            }
        }
        return itemToIndex;
    }

    // ------------------------------------------------------------------ //
    // Group member validation                                             //
    // ------------------------------------------------------------------ //

    /**
     * Resolves a group member to its item index.
     *
     * @param itemToIndex     the item-to-index map
     * @param member          the group member item
     * @param <TSortableItem> the item type
     * @return the item index
     * @throws SortingException if the item is not found in the index
     */
    static <TSortableItem> int resolveGroupMemberIndex(
            @NonNull Map<TSortableItem, Integer> itemToIndex, @NonNull TSortableItem member) {
        Integer index = itemToIndex.get(member);
        if (index == null) {
            throw new SortingException("Group references unknown member: \"" + member + "\"");
        }
        return index;
    }

    /**
     * Validates that an item has not already been assigned to another super-node (group).
     *
     * @param currentSuperNode the item's current super-node assignment
     *                         ({@link #UNASSIGNED} if not yet assigned)
     * @param member           the member item (for error messages)
     * @throws SortingException if the item already belongs to a group
     */
    static void validateNotAlreadyGrouped(int currentSuperNode, @NonNull Object member) {
        if (currentSuperNode != UNASSIGNED) {
            throw new SortingException("Member \"" + member + "\" appears in more than one group");
        }
    }

    // ------------------------------------------------------------------ //
    // Dependency edge validation                                          //
    // ------------------------------------------------------------------ //

    /**
     * Holds the resolved endpoints of a dependency edge: provider and dependent item indices
     * together with the original items (retained for error messages in subsequent checks).
     *
     * @param <TSortableItem> the item type
     */
    @Value
    static class ResolvedEdge<TSortableItem> {
        int providerIndex;
        int dependentIndex;
        TSortableItem provider;
        TSortableItem dependent;
    }

    /**
     * Resolves and validates a dependency edge — looks up both endpoints in the item index,
     * verifies they exist in the input set and are not the same item.
     *
     * @param edge            the raw dependency edge
     * @param itemToIndex     item-to-index map built from the input items
     * @param <TSortableItem> the item type
     * @return a {@link ResolvedEdge} with validated item indices and original items
     * @throws SortingException if the provider or dependent is unknown, or it is a self-dependency
     */
    @NonNull
    static <TSortableItem> ResolvedEdge<TSortableItem> resolveDependencyEdge(
            @NonNull Dependencies.Dependency<TSortableItem> edge, @NonNull Map<TSortableItem, Integer> itemToIndex) {
        TSortableItem provider = edge.getProvider();
        TSortableItem dependent = edge.getDependent();

        Integer providerIndex = itemToIndex.get(provider);
        if (providerIndex == null) {
            throw new SortingException("Dependency references unknown provider: \"" + provider + "\"");
        }

        Integer dependentIndex = itemToIndex.get(dependent);
        if (dependentIndex == null) {
            throw new SortingException("Dependency references unknown dependent: \"" + dependent + "\"");
        }

        if (providerIndex.equals(dependentIndex)) {
            throw new SortingException("Self-dependency on \"" + provider + "\" is not allowed");
        }

        return new ResolvedEdge<>(providerIndex, dependentIndex, provider, dependent);
    }

    // ------------------------------------------------------------------ //
    // IntBag — compact resizable int collection                           //
    // ------------------------------------------------------------------ //

    /**
     * A compact, resizable collection of primitive {@code int} values.
     * Used for adjacency lists to avoid {@code Integer} boxing overhead.
     */
    static final class IntBag {
        int[] data = new int[4];
        int size;

        void add(int value) {
            if (size == data.length) {
                data = Arrays.copyOf(data, size * 2);
            }
            data[size] = value;
            size++;
        }

        /** Linear scan — efficient for the typically small adjacency lists in sorting graphs. */
        boolean contains(int value) {
            for (int i = 0; i < size; i++) {
                if (data[i] == value) return true;
            }
            return false;
        }
    }
}
