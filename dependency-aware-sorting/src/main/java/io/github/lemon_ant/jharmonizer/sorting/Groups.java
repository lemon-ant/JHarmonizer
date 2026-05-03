/*
 * SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.lemon_ant.jharmonizer.sorting;

import java.util.List;
import lombok.NonNull;
import lombok.Value;

/**
 * Describes which items form groups (groups that must stay together as an indivisible block).
 * <p>
 * Each {@link Group} contains the actual items that form that group.
 * Items that do not appear in any group are treated as singleton groups internally.
 *
 * @param <TNode> the type of items
 */
@Value
@SuppressWarnings("PMD.AvoidFieldNameMatchingTypeName")
public class Groups<TNode> {

    List<Group<TNode>> groups;

    private static final Groups<?> EMPTY_INSTANCE = new Groups<>(List.of());

    /** Returns an empty grouping (no groups — every item is its own singleton block). */
    @NonNull
    @SuppressWarnings("unchecked")
    public static <TNode> Groups<TNode> empty() {
        return (Groups<TNode>) EMPTY_INSTANCE;
    }

    /** Convenience factory: each vararg is one {@link Group}. */
    @NonNull
    @SafeVarargs
    public static <TNode> Groups<TNode> of(@NonNull Group<TNode>... groups) {
        return new Groups<>(List.of(groups));
    }
}
