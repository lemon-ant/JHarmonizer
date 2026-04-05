package io.github.lemon_ant.jharmonizer.sorting;

import java.util.*;
import lombok.NonNull;
import lombok.experimental.UtilityClass;

/**
 * Package-private utility class holding sorting primitives and data structures extracted from
 * {@link SimplifiedDependencyAwareSorter} to keep that class below PMD's method-count and
 * god-class thresholds.
 *
 * <p>Contains:
 * <ul>
 *   <li>Boxing-free insertion sort and merge sort for {@code int[]} arrays keyed by a generic
 *       comparator.</li>
 *   <li>{@link SuperNodes} — immutable flat-array super-node layout.</li>
 *   <li>{@link IntHeap} — boxing-free min-heap ordered by super-node keys.</li>
 * </ul>
 */
@UtilityClass
class SimplifiedSortingInternals {

    /** Threshold below which merge sort falls back to insertion sort. */
    static final int INSERTION_SORT_THRESHOLD = 16;

    /** Minimum length that requires sorting at all. */
    static final int MIN_SORT_SIZE = 1;

    // ------------------------------------------------------------------ //
    // Sorting utilities                                                   //
    // ------------------------------------------------------------------ //

    /**
     * Insertion sort for a small range within {@code data[offset .. offset+length-1]}.
     * Used for intra-group ordering where groups are typically small.
     *
     * @param data       the array containing indices to sort
     * @param offset     start position within the array
     * @param length     number of elements to sort
     * @param items      the item list for key lookup
     * @param comparator the ordering comparator
     */
    // AvoidArrayLoops: this IS an insertion sort, not a simple array copy — System.arraycopy
    // does not apply because elements are shifted conditionally based on comparator ordering.
    @SuppressWarnings("PMD.AvoidArrayLoops")
    static <TSortableItem> void insertionSortRange(
            @NonNull int[] data,
            int offset,
            int length,
            @NonNull List<TSortableItem> items,
            @NonNull Comparator<TSortableItem> comparator) {
        for (int i = 1; i < length; i++) {
            int insertedIndex = data[offset + i];
            TSortableItem insertedItem = items.get(insertedIndex);
            int j = i - 1;
            while (j >= 0 && comparator.compare(items.get(data[offset + j]), insertedItem) > 0) {
                data[offset + j + 1] = data[offset + j];
                j--;
            }
            data[offset + j + 1] = insertedIndex;
        }
    }

    /**
     * Sorts {@code array[0..length)} by comparing {@code keys[array[i]]} using the comparator.
     * Uses merge sort with insertion-sort base case.  No {@code Integer} boxing.
     *
     * @param array      the index array to sort in-place
     * @param length     the number of elements to sort
     * @param keys       the key array for comparisons
     * @param comparator the ordering comparator
     */
    static <TSortableItem> void sortIndicesByKey(
            @NonNull int[] array,
            int length,
            @NonNull TSortableItem[] keys,
            @NonNull Comparator<TSortableItem> comparator) {
        if (length <= MIN_SORT_SIZE) return;
        int[] workspace = new int[length];
        mergeSortByKey(array, workspace, 0, length, keys, comparator);
    }

    /**
     * Recursive merge sort that falls back to insertion sort for small ranges.
     *
     * @param array      the index array to sort in-place
     * @param workspace  scratch buffer for merging
     * @param lo         start index (inclusive)
     * @param hi         end index (exclusive)
     * @param keys       the key array for comparisons
     * @param comparator the ordering comparator
     */
    @SuppressWarnings({"PMD.AvoidArrayLoops", "PMD.CognitiveComplexity"})
    static <TSortableItem> void mergeSortByKey(
            @NonNull int[] array,
            @NonNull int[] workspace,
            int lo,
            int hi,
            @NonNull TSortableItem[] keys,
            @NonNull Comparator<TSortableItem> comparator) {
        int length = hi - lo;
        if (length <= INSERTION_SORT_THRESHOLD) {
            // Insertion sort for small ranges
            for (int i = lo + 1; i < hi; i++) {
                int insertedIndex = array[i];
                TSortableItem insertedKey = keys[insertedIndex];
                int j = i - 1;
                while (j >= lo && comparator.compare(keys[array[j]], insertedKey) > 0) {
                    array[j + 1] = array[j];
                    j--;
                }
                array[j + 1] = insertedIndex;
            }
            return;
        }

        int mid = (lo + hi) >>> 1;
        mergeSortByKey(array, workspace, lo, mid, keys, comparator);
        mergeSortByKey(array, workspace, mid, hi, keys, comparator);

        // Merge array[lo..mid) and array[mid..hi) — only left half needs a workspace copy
        // (right half stays in array and is consumed in place)
        System.arraycopy(array, lo, workspace, lo, mid - lo);
        int left = lo;
        int right = mid;
        int writePos = lo;
        while (left < mid && right < hi) {
            if (comparator.compare(keys[workspace[left]], keys[array[right]]) <= 0) {
                array[writePos] = workspace[left];
                writePos++;
                left++;
            } else {
                array[writePos] = array[right];
                writePos++;
                right++;
            }
        }
        while (left < mid) {
            array[writePos] = workspace[left];
            writePos++;
            left++;
        }
    }

    // ------------------------------------------------------------------ //
    // Inner data structures                                               //
    // ------------------------------------------------------------------ //

    /**
     * Immutable holder for the super-node layout computed by
     * {@link SimplifiedDependencyAwareSorter#buildSuperNodes}.
     *
     * <p>All item indices are stored in a single flat {@code memberIndices} array, with
     * per-super-node offset/length pairs for O(1) access.  This avoids thousands of small
     * {@code int[]} allocations for singleton super-nodes.</p>
     *
     * @param <TSortableItem> the item type (used for the keys array)
     */
    static final class SuperNodes<TSortableItem> {
        /** Item index → super-node index. */
        final int[] itemToSuperNode;
        /** Flat item-index storage, grouped by super-node. */
        final int[] memberIndices;
        /** Per-super-node start position in {@code memberIndices}. */
        final int[] nodeOffset;
        /** Per-super-node item count. */
        final int[] nodeLength;
        /** Per-super-node comparator-minimum item (tie-break key). */
        final TSortableItem[] nodeKeys;
        /** Total number of super-nodes. */
        final int count;
        /** Index of the first singleton (non-group) super-node. */
        final int firstSingletonIndex;

        @SuppressWarnings("PMD.ArrayIsStoredDirectly")
        SuperNodes(
                int[] itemToSuperNode,
                int[] memberIndices,
                int[] nodeOffset,
                int[] nodeLength,
                TSortableItem[] nodeKeys,
                int count,
                int firstSingletonIndex) {
            this.itemToSuperNode = itemToSuperNode;
            this.memberIndices = memberIndices;
            this.nodeOffset = nodeOffset;
            this.nodeLength = nodeLength;
            this.nodeKeys = nodeKeys;
            this.count = count;
            this.firstSingletonIndex = firstSingletonIndex;
        }
    }

    /**
     * Boxing-free min-heap that orders super-node indices by their keys.
     * Replaces {@code PriorityQueue<Integer>} to eliminate all {@code Integer}
     * boxing/unboxing in Kahn's topological sort.
     *
     * @param <TSortableItem> the item type (used for key comparison)
     */
    static final class IntHeap<TSortableItem> {
        private final int[] heap;
        private int size;
        private final TSortableItem[] keys;
        private final Comparator<TSortableItem> comparator;

        @SuppressWarnings("PMD.ArrayIsStoredDirectly")
        IntHeap(int capacity, TSortableItem[] keys, Comparator<TSortableItem> comparator) {
            this.heap = new int[capacity];
            this.keys = keys;
            this.comparator = comparator;
        }

        void add(int value) {
            heap[size] = value;
            siftUp(size);
            size++;
        }

        int poll() {
            int min = heap[0];
            size--;
            heap[0] = heap[size];
            if (size > 0) siftDown(0);
            return min;
        }

        int peek() {
            return heap[0];
        }

        boolean isEmpty() {
            return size == 0;
        }

        private void siftUp(int index) {
            int position = index;
            int value = heap[position];
            TSortableItem valueKey = keys[value];
            while (position > 0) {
                int parent = (position - 1) >>> 1;
                if (comparator.compare(keys[heap[parent]], valueKey) <= 0) break;
                heap[position] = heap[parent];
                position = parent;
            }
            heap[position] = value;
        }

        private void siftDown(int index) {
            int position = index;
            int value = heap[position];
            TSortableItem valueKey = keys[value];
            int half = size >>> 1;
            while (position < half) {
                int child = (position << 1) + 1;
                int right = child + 1;
                if (right < size && comparator.compare(keys[heap[right]], keys[heap[child]]) < 0) {
                    child = right;
                }
                if (comparator.compare(valueKey, keys[heap[child]]) <= 0) break;
                heap[position] = heap[child];
                position = child;
            }
            heap[position] = value;
        }
    }
}
