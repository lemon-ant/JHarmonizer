// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
package io.github.lemon_ant.jharmonizer.core.sorter.spoon;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.experimental.UtilityClass;

/** Collects final element order and detects identity changes during the same traversal. */
@UtilityClass
class OrderChangeCollector {

    /**
     * Collects a final permutation and reports whether it differs from its input order.
     * @param elementCount number of elements in the permutation
     * @param elementsBeforeOrdering iterator over the input order
     * @param orderedElements sequential stream producing the final permutation
     * @param <TElement> element type
     * @return immutable final order and its change flag
     */
    @NonNull
    static <TElement> ElementOrdering<TElement> collectOrderedElements(
            int elementCount,
            @NonNull Iterator<TElement> elementsBeforeOrdering,
            @NonNull Stream<TElement> orderedElements) {
        List<TElement> elementsInSortedOrder = new ArrayList<>(elementCount);
        ElementOrderAccumulator<TElement> accumulator =
                new ElementOrderAccumulator<>(elementsBeforeOrdering, elementsInSortedOrder);
        orderedElements.forEachOrdered(accumulator::append);
        return new ElementOrdering<>(Collections.unmodifiableList(elementsInSortedOrder), accumulator.reordered);
    }

    /** Prepared element order and its change flag, which may describe order within the elements. */
    @Value
    @AllArgsConstructor(access = AccessLevel.PACKAGE)
    static class ElementOrdering<TElement> {

        @NonNull
        List<TElement> elementsInSortedOrder;

        boolean reordered;
    }

    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    private static final class ElementOrderAccumulator<TElement> {

        @NonNull
        private final Iterator<TElement> elementsBeforeOrdering;

        @NonNull
        private final List<TElement> elementsInSortedOrder;

        private boolean reordered;

        @SuppressWarnings("PMD.CompareObjectsWithEquals")
        private void append(TElement element) {
            // Compare the final emitted order: dependency repair can undo intermediate comparator moves.
            if (!reordered && elementsBeforeOrdering.next() != element) {
                reordered = true;
            }
            elementsInSortedOrder.add(element);
        }
    }
}
