package io.github.lemon_ant.jharmonizer.sorting;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.Value;
import lombok.experimental.UtilityClass;

/**
 * Super-node construction and expansion utilities used by {@link SimplifiedDependencyAwareSorter}.
 *
 * <p>Handles building the super-node layout (groups + singletons) from input items,
 * and expanding a super-node ordering back into the final item list.</p>
 */
@UtilityClass
@SuppressWarnings("PMD.CouplingBetweenObjects")
class SuperNodeUtils {

    // ------------------------------------------------------------------ //
    // Super-node construction                                             //
    // ------------------------------------------------------------------ //

    /**
     * Builds the complete super-node layout: assigns every item to a super-node,
     * populates the flat storage arrays, and computes per-super-node keys.
     *
     * <p>Group super-nodes are created first (one per non-empty group), followed by
     * singleton super-nodes for items not assigned to any group.</p>
     *
     * @param items       the ordered list of items
     * @param itemToIndex item-to-index map
     * @param groups      group definitions
     * @param comparator  comparator for intra-group ordering
     * @param <TSortableItem> the item type
     * @return the computed super-node layout
     */
    @SuppressWarnings("unchecked")
    @NonNull
    static <TSortableItem> SuperNodes<TSortableItem> buildSuperNodes(
            @NonNull List<TSortableItem> items,
            @NonNull Map<TSortableItem, Integer> itemToIndex,
            @NonNull Groups<TSortableItem> groups,
            @NonNull Comparator<TSortableItem> comparator) {
        int itemCount = items.size();
        int[] itemToSuperNode = new int[itemCount];
        Arrays.fill(itemToSuperNode, SortingUtils.UNASSIGNED);

        int[] memberIndices = new int[itemCount];
        int[] nodeOffset = new int[itemCount];
        int[] nodeLength = new int[itemCount];
        // Safe unchecked cast: the array is only used internally; all reads go through
        // TSortableItem-typed fields/variables, and the array is never exposed outside this class.
        // This is the standard pattern used by java.util.ArrayList and java.util.HashMap.
        TSortableItem[] nodeKeys = (TSortableItem[]) new Object[itemCount];

        int firstSingletonIndex = assignGroupSuperNodes(
                groups,
                items,
                itemToIndex,
                comparator,
                itemToSuperNode,
                memberIndices,
                nodeOffset,
                nodeLength,
                nodeKeys);

        int dataPosition =
                firstSingletonIndex > 0 ? nodeOffset[firstSingletonIndex - 1] + nodeLength[firstSingletonIndex - 1] : 0;

        int totalNodeCount = assignSingletonSuperNodes(
                items,
                itemToSuperNode,
                memberIndices,
                nodeOffset,
                nodeLength,
                nodeKeys,
                firstSingletonIndex,
                dataPosition);

        return new SuperNodes<>(
                itemToSuperNode, memberIndices, nodeOffset, nodeLength, nodeKeys, totalNodeCount, firstSingletonIndex);
    }

    /**
     * Assigns group super-nodes: one super-node per non-empty group. Populates array slots
     * and returns the number of group super-nodes created.
     */
    // Array parameter is intentional: varargs would add allocation overhead in this performance path.
    @SuppressWarnings("PMD.UseVarargs")
    private static <TSortableItem> int assignGroupSuperNodes(
            Groups<TSortableItem> groups,
            List<TSortableItem> items,
            Map<TSortableItem, Integer> itemToIndex,
            Comparator<TSortableItem> comparator,
            int[] itemToSuperNode,
            int[] memberIndices,
            int[] nodeOffset,
            int[] nodeLength,
            TSortableItem[] nodeKeys) {
        int superNodeCount = 0;
        int dataPosition = 0;

        for (Group<TSortableItem> group : groups.getGroups()) {
            List<TSortableItem> groupItems = group.getItems();
            if (groupItems.isEmpty()) {
                continue;
            }

            nodeOffset[superNodeCount] = dataPosition;
            nodeLength[superNodeCount] = groupItems.size();
            resolveGroupMembers(groupItems, itemToIndex, itemToSuperNode, superNodeCount, memberIndices, dataPosition);

            nodeKeys[superNodeCount] =
                    findComparatorMinimum(memberIndices, dataPosition, groupItems.size(), items, comparator);
            dataPosition += groupItems.size();
            superNodeCount++;
        }

        return superNodeCount;
    }

    /**
     * Assigns singleton super-nodes for items not assigned to any group.
     * Returns the total number of super-nodes (groups + singletons).
     */
    private static <TSortableItem> int assignSingletonSuperNodes(
            List<TSortableItem> items,
            int[] itemToSuperNode,
            int[] memberIndices,
            int[] nodeOffset,
            int[] nodeLength,
            TSortableItem[] nodeKeys,
            int superNodeCount,
            int initialDataPosition) {
        int currentSuperNodeCount = superNodeCount;
        int dataPosition = initialDataPosition;

        for (int i = 0; i < items.size(); i++) {
            if (itemToSuperNode[i] == SortingUtils.UNASSIGNED) {
                itemToSuperNode[i] = currentSuperNodeCount;
                nodeOffset[currentSuperNodeCount] = dataPosition;
                nodeLength[currentSuperNodeCount] = 1;
                memberIndices[dataPosition] = i;
                dataPosition++;
                nodeKeys[currentSuperNodeCount] = items.get(i);
                currentSuperNodeCount++;
            }
        }

        return currentSuperNodeCount;
    }

    /**
     * Resolves group item identities to indices, validates uniqueness, and populates
     * {@code itemToSuperNode} and {@code memberIndices} arrays.
     */
    private static <TSortableItem> void resolveGroupMembers(
            List<TSortableItem> groupItems,
            Map<TSortableItem, Integer> itemToIndex,
            int[] itemToSuperNode,
            int superNodeIndex,
            int[] memberIndices,
            int dataPosition) {
        for (int j = 0; j < groupItems.size(); j++) {
            TSortableItem item = groupItems.get(j);
            int itemIndex = SortingUtils.resolveGroupMemberIndex(itemToIndex, item);
            SortingUtils.validateNotAlreadyGrouped(itemToSuperNode[itemIndex], item);
            itemToSuperNode[itemIndex] = superNodeIndex;
            memberIndices[dataPosition + j] = itemIndex;
        }
    }

    // ------------------------------------------------------------------ //
    // Expand super-node order to items                                    //
    // ------------------------------------------------------------------ //

    /**
     * Expands the super-node order into the final item list.
     *
     * @param superNodeOrder ordered array of super-node indices
     * @param superNodes     the super-node layout
     * @param items          the original item list
     * @param <TSortableItem> the item type
     * @return the items in the computed order
     */
    @NonNull
    static <TSortableItem> List<TSortableItem> expandOrder(
            @NonNull int[] superNodeOrder,
            @NonNull SuperNodes<TSortableItem> superNodes,
            @NonNull List<TSortableItem> items) {
        List<TSortableItem> result = new ArrayList<>(items.size());
        for (int nodeIndex : superNodeOrder) {
            int start = superNodes.getNodeOffset()[nodeIndex];
            int memberCount = superNodes.getNodeLength()[nodeIndex];
            for (int i = start; i < start + memberCount; i++) {
                result.add(items.get(superNodes.getMemberIndices()[i]));
            }
        }
        return result;
    }

    // ------------------------------------------------------------------ //
    // Intra-group sorting                                                 //
    // ------------------------------------------------------------------ //

    /**
     * Finds the comparator-minimum item across a range in {@code data[offset .. offset+length-1]}.
     * The range is not modified — only the minimum item is returned for use as the super-node key.
     */
    @NonNull
    private static <TSortableItem> TSortableItem findComparatorMinimum(
            int[] data, int offset, int length, List<TSortableItem> items, Comparator<TSortableItem> comparator) {
        TSortableItem minItem = items.get(data[offset]);
        for (int i = 1; i < length; i++) {
            TSortableItem candidate = items.get(data[offset + i]);
            if (comparator.compare(candidate, minItem) < 0) {
                minItem = candidate;
            }
        }
        return minItem;
    }

    // ------------------------------------------------------------------ //
    // Super-node data structure                                           //
    // ------------------------------------------------------------------ //

    /**
     * Immutable holder for the super-node layout computed by {@link #buildSuperNodes}.
     *
     * <p>All item indices are stored in a single flat {@code memberIndices} array, with
     * per-super-node offset/length pairs for O(1) access.  This avoids thousands of small
     * {@code int[]} allocations for singleton super-nodes.</p>
     *
     * @param <TSortableItem> the item type (used for the keys array)
     */
    @Value
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    static class SuperNodes<TSortableItem> {
        /** Item index → super-node index. */
        int[] itemToSuperNode;
        /** Flat item-index storage, grouped by super-node. */
        int[] memberIndices;
        /** Per-super-node start position in {@code memberIndices}. */
        int[] nodeOffset;
        /** Per-super-node item count. */
        int[] nodeLength;
        /** Per-super-node comparator-minimum item (tie-break key). */
        TSortableItem[] nodeKeys;
        /** Total number of super-nodes. */
        int count;
        /** Index of the first singleton (non-group) super-node. */
        int firstSingletonIndex;
    }
}
