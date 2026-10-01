// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
package io.github.lemon_ant.jharmonizer.core.spoon;

import java.util.Collections;
import java.util.List;
import lombok.NonNull;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import spoon.reflect.declaration.CtTypeMember;

/**
 * Captures a member-ordering violation computed by the sorter.
 *
 * <p>This value class retains a contiguous group of members that are being relocated
 * together and their immediate predecessor and successor in the correct sorted order, so that
 * diagnostic messages can tell the user exactly where the group should appear.
 *
 * <p>{@code sortedPredecessor} is {@code null} when the relocated group should be the very first
 * in its scope. {@code sortedSuccessor} is {@code null} when it should be the very last.
 * The member list is an unmodifiable view. Callers must not modify the handed-off list;
 * the referenced Spoon nodes remain mutable.
 */
@Value
public class MemberRelocation {

    /**
     * The contiguous group of type members that are in the wrong position in the original source.
     * Contains at least one element. Consecutive moved members in the sorted order form one chunk.
     */
    @NonNull
    List<CtTypeMember> relocatedMembers;

    /**
     * The type member that immediately precedes the relocated group in the correct sorted order,
     * or {@code null} if the group should be first.
     */
    @Nullable
    CtTypeMember sortedPredecessor;

    /**
     * The type member that immediately follows the relocated group in the correct sorted order,
     * or {@code null} if the group should be last.
     */
    @Nullable
    CtTypeMember sortedSuccessor;

    /**
     * Retains an unmodifiable view of a moved chunk and its sorted-order neighbours.
     * @param relocatedMembers the moved members in sorted order; callers must not modify this list after handoff
     * @param sortedPredecessor the preceding member, or {@code null} at the scope start
     * @param sortedSuccessor the following member, or {@code null} at the scope end
     */
    MemberRelocation(
            @NonNull List<? extends CtTypeMember> relocatedMembers,
            @Nullable CtTypeMember sortedPredecessor,
            @Nullable CtTypeMember sortedSuccessor) {
        this.relocatedMembers = Collections.unmodifiableList(relocatedMembers);
        this.sortedPredecessor = sortedPredecessor;
        this.sortedSuccessor = sortedSuccessor;
    }
}
