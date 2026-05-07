// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
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
     * @param <TNode> the item type
     * @return the computed super-node layout
     */
    @SuppressWarnings("unchecked")
    @NonNull
    static <TNode> SuperNodes<TNode> buildSuperNodes(
            @NonNull List<TNode> items,
            @NonNull Map<TNode, Integer> itemToIndex,
            @NonNull Groups<TNode> groups,
            @NonNull Comparator<TNode> comparator) {
        int itemCount = items.size();
        int[] itemToSuperNode = new int[itemCount];
        Arrays.fill(itemToSuperNode, SortingUtils.UNASSIGNED);

        int[] memberIndices = new int[itemCount];
        int[] nodeOffset = new int[itemCount];
        int[] nodeLength = new int[itemCount];
        // Safe unchecked cast: the array is only used internally; all reads go through
        // TNode-typed fields/variables, and the array is never exposed outside this class.
        // This is the standard pattern used by java.util.ArrayList and java.util.HashMap.
        TNode[] nodeKeys = (TNode[]) new Object[itemCount];

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
                totalNodeCount, firstSingletonIndex, itemToSuperNode, memberIndices, nodeKeys, nodeLength, nodeOffset);
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
     * @param <TNode> the item type
     * @return the items in the computed order
     */
    @NonNull
    static <TNode> List<TNode> expandOrder(
            @NonNull int[] superNodeOrder, @NonNull SuperNodes<TNode> superNodes, @NonNull List<TNode> items) {
        List<TNode> result = new ArrayList<>(items.size());
        for (int nodeIndex : superNodeOrder) {
            int start = superNodes.getNodeOffset()[nodeIndex];
            int memberCount = superNodes.getNodeLength()[nodeIndex];
            for (int i = start; i < start + memberCount; i++) {
                result.add(items.get(superNodes.getMemberIndices()[i]));
            }
        }
        return result;
    }

    /**
     * Assigns group super-nodes: one super-node per non-empty group. Populates array slots
     * and returns the number of group super-nodes created.
     */
    // Array parameter is intentional: varargs would add allocation overhead in this performance path.
    @SuppressWarnings("PMD.UseVarargs")
    private static <TNode> int assignGroupSuperNodes(
            Groups<TNode> groups,
            List<TNode> items,
            Map<TNode, Integer> itemToIndex,
            Comparator<TNode> comparator,
            int[] itemToSuperNode,
            int[] memberIndices,
            int[] nodeOffset,
            int[] nodeLength,
            TNode[] nodeKeys) {
        int superNodeCount = 0;
        int dataPosition = 0;

        for (Group<TNode> group : groups.getGroups()) {
            List<TNode> groupItems = group.getItems();
            if (groupItems.isEmpty()) {
                continue;
            }

            nodeOffset[superNodeCount] = dataPosition;
            nodeLength[superNodeCount] = groupItems.size();
            resolveGroupMembers(groupItems, itemToIndex, itemToSuperNode, superNodeCount, memberIndices, dataPosition);

            insertionSortRange(memberIndices, dataPosition, groupItems.size(), items, comparator);
            nodeKeys[superNodeCount] = items.get(memberIndices[dataPosition]);
            dataPosition += groupItems.size();
            superNodeCount++;
        }

        return superNodeCount;
    }

    /**
     * Assigns singleton super-nodes for items not assigned to any group.
     * Returns the total number of super-nodes (groups + singletons).
     */
    private static <TNode> int assignSingletonSuperNodes(
            List<TNode> items,
            int[] itemToSuperNode,
            int[] memberIndices,
            int[] nodeOffset,
            int[] nodeLength,
            TNode[] nodeKeys,
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

    // ------------------------------------------------------------------ //
    // Intra-group sorting                                                 //
    // ------------------------------------------------------------------ //

    /**
     * Insertion sort for a small range within {@code data[offset .. offset+length-1]}.
     * Used for intra-group ordering where groups are typically small.
     */
    @SuppressWarnings("PMD.AvoidArrayLoops")
    private static <TNode> void insertionSortRange(
            int[] data, int offset, int length, List<TNode> items, Comparator<TNode> comparator) {
        for (int i = 1; i < length; i++) {
            int insertedIndex = data[offset + i];
            TNode insertedItem = items.get(insertedIndex);
            int shiftPos = i - 1;
            while (shiftPos >= 0 && comparator.compare(items.get(data[offset + shiftPos]), insertedItem) > 0) {
                data[offset + shiftPos + 1] = data[offset + shiftPos];
                shiftPos--;
            }
            data[offset + shiftPos + 1] = insertedIndex;
        }
    }

    /**
     * Resolves group item identities to indices, validates uniqueness, and populates
     * {@code itemToSuperNode} and {@code memberIndices} arrays.
     */
    private static <TNode> void resolveGroupMembers(
            List<TNode> groupItems,
            Map<TNode, Integer> itemToIndex,
            int[] itemToSuperNode,
            int superNodeIndex,
            int[] memberIndices,
            int dataPosition) {
        for (int j = 0; j < groupItems.size(); j++) {
            TNode item = groupItems.get(j);
            int itemIndex = SortingUtils.resolveGroupMemberIndex(itemToIndex, item);
            SortingUtils.validateNotAlreadyGrouped(itemToSuperNode[itemIndex], item);
            itemToSuperNode[itemIndex] = superNodeIndex;
            memberIndices[dataPosition + j] = itemIndex;
        }
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
     * @param <TNode> the item type (used for the keys array)
     */
    @Value
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    static class SuperNodes<TNode> {

        /** Total number of super-nodes. */
        int count;

        /** Index of the first singleton (non-group) super-node. */
        int firstSingletonIndex;

        /** Item index → super-node index. */
        int[] itemToSuperNode;

        /** Flat item-index storage, grouped by super-node. */
        int[] memberIndices;

        /** Per-super-node comparator-minimum item (tie-break key). */
        TNode[] nodeKeys;

        /** Per-super-node item count. */
        int[] nodeLength;

        /** Per-super-node start position in {@code memberIndices}. */
        int[] nodeOffset;
    }
}
