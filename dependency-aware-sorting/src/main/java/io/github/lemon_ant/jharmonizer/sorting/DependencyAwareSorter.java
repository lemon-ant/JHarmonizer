package io.github.lemon_ant.jharmonizer.sorting;

import java.util.*;
import lombok.NonNull;
import lombok.experimental.UtilityClass;

/**
 * Utility class that sorts a collection of items while respecting two kinds of constraints:
 * <ol>
 *   <li><b>Groups</b> — items that belong to the same {@link Group} travel together as an
 *       indivisible block; within the block they are ordered by the supplied comparator.</li>
 *   <li><b>Dependencies</b> — {@code provider → dependent} edges that force the provider
 *       block to appear before the dependent block.  The edges form a DAG.</li>
 * </ol>
 *
 * <p>When no constraints force a particular order the comparator-based ordering is used as a
 * deterministic tie-breaker.</p>
 *
 * <p>The algorithm is fully generic: it accepts any item type {@code TSortableItem}, a comparator, and
 * generic grouping/dependency constraints. Items are identified by their {@code equals/hashCode}
 * contract — no separate identity-extraction function is needed.</p>
 *
 * <h2>Algorithm</h2>
 * <ol>
 *   <li>Validate input (unique items, no item in two groups, intra-group dependency
 *       compatibility).</li>
 *   <li>Assign every item a compact {@code int} index; map items to <em>super-nodes</em>
 *       (groups → one super-node each; singletons → one super-node each).</li>
 *   <li>Build a directed graph on super-nodes from the inter-group edges.</li>
 *   <li>Run Kahn's topological sort; break ties by the super-node's <em>key</em>
 *       (= comparator-minimum item in that super-node).</li>
 *   <li>Expand super-nodes back to items in comparator order within each block.</li>
 * </ol>
 *
 * <p>Time complexity: <em>O(n log n + E)</em> · Space: <em>O(n + E)</em>.</p>
 */
@UtilityClass
@SuppressWarnings("PMD.CouplingBetweenObjects")
public class DependencyAwareSorter {

    // ------------------------------------------------------------------ //
    // Public API — Generic                                                //
    // ------------------------------------------------------------------ //

    /**
     * Sorts {@code items} according to the supplied constraints, comparator, and identity
     * function.
     *
     * <p>The comparator governs:
     * <ul>
     *   <li>the ordering of items <em>within</em> each group block, and</li>
     *   <li>the tie-breaking order among super-nodes at each step of the topological sort.</li>
     * </ul>
     *
     * <p>Items are identified by their {@code equals/hashCode} contract for duplicate
     * detection, dependency/group resolution, and error messages.
     * Two items must not be equal.
     *
     * @param <TSortableItem>        the item type
     * @param items             items to sort (input order is irrelevant)
     * @param groups          group definitions; use {@link Groups#empty()} if none
     * @param dependencies      provider → dependent ordering edges; use
     *                          {@link Dependencies#empty()} if none
     * @param comparator        determines intra-group and tie-break ordering
     * @return a new, unmodifiable list of the same items in the computed order
     * @throws SortingException if the input is invalid (see class javadoc)
     */
    @NonNull
    public static <TSortableItem> List<TSortableItem> sort(
            @NonNull Collection<TSortableItem> items,
            @NonNull Groups<TSortableItem> groups,
            @NonNull Dependencies<TSortableItem> dependencies,
            @NonNull Comparator<TSortableItem> comparator) {
        if (items.isEmpty()) {
            return List.of();
        }
        List<TSortableItem> itemList = new ArrayList<>(items);

        Map<TSortableItem, Integer> itemToIndex = CommonSortingUtils.buildItemIndex(itemList);

        int[] superNodeOf = new int[itemList.size()];
        Arrays.fill(superNodeOf, CommonSortingUtils.UNASSIGNED);
        List<List<Integer>> superNodeMembers = new ArrayList<>();
        List<TSortableItem> superNodeKeys = new ArrayList<>();
        createGroupSuperNodes(itemList, itemToIndex, groups, comparator, superNodeOf, superNodeMembers, superNodeKeys);
        createSingletonSuperNodes(itemList, superNodeOf, superNodeMembers, superNodeKeys);

        int superNodeCount = superNodeMembers.size();
        int[] inDegree = new int[superNodeCount];
        CommonSortingUtils.IntBag[] adjacencyLists = new CommonSortingUtils.IntBag[superNodeCount];
        for (int i = 0; i < superNodeCount; i++) {
            adjacencyLists[i] = new CommonSortingUtils.IntBag();
        }
        buildDependencyGraph(
                itemList, itemToIndex, superNodeOf, superNodeCount, dependencies, inDegree, adjacencyLists, comparator);

        List<TSortableItem> result = topologicalSortAndExpand(
                superNodeCount, inDegree, adjacencyLists, superNodeMembers, superNodeKeys, itemList, comparator);
        return Collections.unmodifiableList(result);
    }

    // ------------------------------------------------------------------ //
    // Step 1 – Create group super-nodes                                   //
    // ------------------------------------------------------------------ //

    /**
     * Creates one super-node for each non-empty group.  Items within a group are sorted
     * by the comparator; the comparator-minimum item becomes the super-node's key.
     */
    private static <TSortableItem> void createGroupSuperNodes(
            List<TSortableItem> items,
            Map<TSortableItem, Integer> itemToIndex,
            Groups<TSortableItem> groups,
            Comparator<TSortableItem> comparator,
            int[] superNodeOf,
            List<List<Integer>> superNodeMembers,
            List<TSortableItem> superNodeKeys) {
        for (Group<TSortableItem> group : groups.getGroups()) {
            if (group.getItems().isEmpty()) {
                continue;
            }
            int superNodeIndex = superNodeMembers.size();
            List<Integer> memberIndices = resolveGroupMemberIndices(group, itemToIndex, superNodeOf, superNodeIndex);
            sortIndicesByComparator(memberIndices, items, comparator);
            superNodeMembers.add(memberIndices);
            superNodeKeys.add(items.get(memberIndices.get(0)));
        }
    }

    private static <TSortableItem> List<Integer> resolveGroupMemberIndices(
            Group<TSortableItem> group,
            Map<TSortableItem, Integer> itemToIndex,
            int[] superNodeOf,
            int superNodeIndex) {
        List<Integer> indices = new ArrayList<>(group.getItems().size());
        for (TSortableItem item : group.getItems()) {
            int itemIndex = CommonSortingUtils.resolveGroupMemberIndex(itemToIndex, item);
            CommonSortingUtils.validateNotAlreadyGrouped(superNodeOf[itemIndex], item);
            superNodeOf[itemIndex] = superNodeIndex;
            indices.add(itemIndex);
        }
        return indices;
    }

    // ------------------------------------------------------------------ //
    // Step 2 – Create singleton super-nodes                               //
    // ------------------------------------------------------------------ //

    /**
     * Creates a singleton super-node for every item not already assigned to a group.
     */
    private static <TSortableItem> void createSingletonSuperNodes(
            List<TSortableItem> items,
            int[] superNodeOf,
            List<List<Integer>> superNodeMembers,
            List<TSortableItem> superNodeKeys) {
        for (int i = 0; i < items.size(); i++) {
            if (superNodeOf[i] == CommonSortingUtils.UNASSIGNED) {
                superNodeOf[i] = superNodeMembers.size();
                superNodeMembers.add(List.of(i));
                superNodeKeys.add(items.get(i));
            }
        }
    }

    // ------------------------------------------------------------------ //
    // Step 3 – Build super-node dependency graph                          //
    // ------------------------------------------------------------------ //

    /**
     * Translates item-level dependency edges into super-node-level graph edges.
     * Validates intra-group dependencies for comparator compatibility and deduplicates
     * inter-group edges.
     */
    private static <TSortableItem> void buildDependencyGraph(
            List<TSortableItem> items,
            Map<TSortableItem, Integer> itemToIndex,
            int[] superNodeOf,
            int superNodeCount,
            Dependencies<TSortableItem> dependencies,
            int[] inDegree,
            CommonSortingUtils.IntBag[] adjacencyLists,
            Comparator<TSortableItem> comparator) {
        Set<Long> seenEdges = new HashSet<>();

        for (Dependencies.Dependency<TSortableItem> edge : dependencies.getEdges()) {
            CommonSortingUtils.ResolvedEdge<TSortableItem> resolved =
                    CommonSortingUtils.resolveDependencyEdge(edge, itemToIndex);

            int providerSuperNode = superNodeOf[resolved.getProviderIndex()];
            int dependentSuperNode = superNodeOf[resolved.getDependentIndex()];

            if (providerSuperNode == dependentSuperNode) {
                validateIntraGroupDependency(items, resolved, comparator);
                continue;
            }

            long edgeKey = (long) providerSuperNode * superNodeCount + dependentSuperNode;
            if (seenEdges.add(edgeKey)) {
                adjacencyLists[providerSuperNode].add(dependentSuperNode);
                inDegree[dependentSuperNode]++;
            }
        }
    }

    /**
     * Validates that an intra-group dependency is compatible with the comparator ordering:
     * the provider must come before the dependent within the group's sorted order.
     */
    private static <TSortableItem> void validateIntraGroupDependency(
            List<TSortableItem> items,
            CommonSortingUtils.ResolvedEdge<TSortableItem> resolved,
            Comparator<TSortableItem> comparator) {
        if (comparator.compare(items.get(resolved.getProviderIndex()), items.get(resolved.getDependentIndex())) >= 0) {
            throw new SortingException("Intra-group dependency \"" + resolved.getProvider() + "\" → \""
                    + resolved.getDependent()
                    + "\" conflicts with required intra-group ordering");
        }
    }

    // ------------------------------------------------------------------ //
    // Step 4 – Kahn's topological sort + expansion                       //
    // ------------------------------------------------------------------ //

    /**
     * Performs Kahn's topological sort on the super-node graph, expanding each
     * super-node into its member items in comparator order.
     */
    private static <TSortableItem> List<TSortableItem> topologicalSortAndExpand(
            int superNodeCount,
            int[] inDegree,
            CommonSortingUtils.IntBag[] adjacencyLists,
            List<List<Integer>> superNodeMembers,
            List<TSortableItem> superNodeKeys,
            List<TSortableItem> items,
            Comparator<TSortableItem> comparator) {
        Queue<Integer> readyQueue = new PriorityQueue<>(Comparator.comparing(superNodeKeys::get, comparator));

        for (int i = 0; i < superNodeCount; i++) {
            if (inDegree[i] == 0) {
                readyQueue.add(i);
            }
        }

        List<TSortableItem> result = new ArrayList<>(items.size());
        int visitedCount = 0;

        while (!readyQueue.isEmpty()) {
            int currentNode = readyQueue.poll();
            visitedCount++;
            expandSuperNode(currentNode, superNodeMembers, items, result);
            advanceNeighbors(adjacencyLists[currentNode], inDegree, readyQueue);
        }

        if (visitedCount != superNodeCount) {
            throw new SortingException("Dependency cycle detected among members");
        }
        return result;
    }

    /** Appends all member items of a super-node to the result list. */
    private static <TSortableItem> void expandSuperNode(
            int nodeIndex,
            List<List<Integer>> superNodeMembers,
            List<TSortableItem> items,
            List<TSortableItem> result) {
        for (int memberIndex : superNodeMembers.get(nodeIndex)) {
            result.add(items.get(memberIndex));
        }
    }

    /** Decrements in-degrees of neighbors and enqueues any that become ready. */
    private static void advanceNeighbors(
            CommonSortingUtils.IntBag neighbors, int[] inDegree, Queue<Integer> readyQueue) {
        for (int i = 0; i < neighbors.size; i++) {
            int neighbor = neighbors.data[i];
            inDegree[neighbor]--;
            if (inDegree[neighbor] == 0) {
                readyQueue.add(neighbor);
            }
        }
    }

    // ------------------------------------------------------------------ //
    // Sorting utility                                                     //
    // ------------------------------------------------------------------ //

    /** Sorts {@code indices} in-place by the comparator order of the items they reference. */
    private static <TSortableItem> void sortIndicesByComparator(
            List<Integer> indices, List<TSortableItem> items, Comparator<TSortableItem> comparator) {
        indices.sort(Comparator.comparing(items::get, comparator));
    }
}
