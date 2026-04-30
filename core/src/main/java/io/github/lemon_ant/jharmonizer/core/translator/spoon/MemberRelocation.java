// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
package io.github.lemon_ant.jharmonizer.core.translator.spoon;

import edu.umd.cs.findbugs.annotations.Nullable;
import java.util.List;
import lombok.NonNull;
import lombok.Value;
import spoon.reflect.declaration.CtElement;

/**
 * Captures a member-ordering violation detected during a check flow.
 *
 * <p>This immutable value class holds a contiguous group of members that are being relocated
 * together and their immediate predecessor and successor in the correct sorted order, so that
 * diagnostic messages can tell the user exactly where the group should appear.
 *
 * <p>{@code sortedPredecessor} is {@code null} when the relocated group should be the very first
 * in its scope. {@code sortedSuccessor} is {@code null} when it should be the very last.
 */
@Value
public class MemberRelocation {

    /**
     * The contiguous group of type members that are in the wrong position in the original source.
     * Contains at least one element. When the members form a consecutive sequence in the original
     * source order, they are reported as a single chunk rather than individual violations.
     */
    @NonNull
    List<CtElement> relocatedMembers;

    /**
     * The element that immediately precedes the relocated group in the correct sorted order,
     * or {@code null} if the group should be first.
     */
    @Nullable
    CtElement sortedPredecessor;

    /**
     * The element that immediately follows the relocated group in the correct sorted order,
     * or {@code null} if the group should be last.
     */
    @Nullable
    CtElement sortedSuccessor;
}
