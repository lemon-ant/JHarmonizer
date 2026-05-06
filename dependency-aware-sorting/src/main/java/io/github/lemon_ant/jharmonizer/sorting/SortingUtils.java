// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
package io.github.lemon_ant.jharmonizer.sorting;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.NonNull;
import lombok.Value;
import lombok.experimental.UtilityClass;

/**
 * Shared utilities used by {@link SimplifiedDependencyAwareSorter}.
 *
 * <p>Contains common validation routines, item-index building,
 * and low-level data structures.</p>
 */
@UtilityClass
class SortingUtils {

    /** Sentinel value indicating an item has not been assigned to any super-node yet. */
    static final int UNASSIGNED = -1;

    // ------------------------------------------------------------------ //
    // Item index                                                          //
    // ------------------------------------------------------------------ //

    /**
     * Builds a mapping from each item to its positional index in the list.
     *
     * @param items           the ordered list of items
     * @param <TNode> the item type
     * @return an item-to-index map
     * @throws SortingException if two items are equal (duplicates)
     */
    @NonNull
    @SuppressWarnings("PMD.UseConcurrentHashMap")
    static <TNode> Map<TNode, Integer> buildItemIndex(@NonNull List<TNode> items) {
        Map<TNode, Integer> itemToIndex = new HashMap<>(items.size() * 2);
        for (int i = 0; i < items.size(); i++) {
            TNode item = items.get(i);
            if (itemToIndex.put(item, i) != null) {
                throw new SortingException("Duplicate item: \"" + item + "\"");
            }
        }
        return Collections.unmodifiableMap(itemToIndex);
    }

    // ------------------------------------------------------------------ //
    // Group member validation                                             //
    // ------------------------------------------------------------------ //

    /**
     * Resolves a group member to its item index.
     *
     * @param itemToIndex     the item-to-index map
     * @param member          the group member item
     * @param <TNode> the item type
     * @return the item index
     * @throws SortingException if the item is not found in the index
     */
    static <TNode> int resolveGroupMemberIndex(@NonNull Map<TNode, Integer> itemToIndex, @NonNull TNode member) {
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
     * @param <TNode> the item type
     */
    @Value
    static class ResolvedEdge<TNode> {
        int providerIndex;
        int dependentIndex;

        @NonNull
        TNode provider;

        @NonNull
        TNode dependent;
    }

    /**
     * Resolves and validates a dependency edge — looks up both endpoints in the item index,
     * verifies they exist in the input set and are not the same item.
     *
     * @param edge            the raw dependency edge
     * @param itemToIndex     item-to-index map built from the input items
     * @param <TNode> the item type
     * @return a {@link ResolvedEdge} with validated item indices and original items
     * @throws SortingException if the provider or dependent is unknown, or it is a self-dependency
     */
    @NonNull
    static <TNode> ResolvedEdge<TNode> resolveDependencyEdge(
            @NonNull Dependencies.Dependency<TNode> edge, @NonNull Map<TNode, Integer> itemToIndex) {
        TNode provider = edge.getProvider();
        TNode dependent = edge.getDependent();

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
}
