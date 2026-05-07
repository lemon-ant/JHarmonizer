// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
package io.github.lemon_ant.jharmonizer.sorting;

// @jharmonizer:fully-off
// jharmonizer v1.0.1 incorrectly reorders @Value class fields, breaking Lombok constructors;
// remove this directive once jharmonizer is upgraded to a version that fixes the @Value field-ordering bug.
import java.util.List;
import lombok.NonNull;
import lombok.Value;

/**
 * An indivisible group of items that must appear together as a contiguous block in the
 * sorted output.
 *
 * <p>Within the block, items are ordered by the comparator supplied to the sorting algorithm.
 *
 * @param <TNode> the type of items in this group
 */
@Value
public class Group<TNode> {

    List<TNode> items;

    /** Convenience factory: creates a group from the given items. */
    @NonNull
    @SafeVarargs
    public static <TNode> Group<TNode> of(@NonNull TNode... items) {
        return new Group<>(List.of(items));
    }
}
