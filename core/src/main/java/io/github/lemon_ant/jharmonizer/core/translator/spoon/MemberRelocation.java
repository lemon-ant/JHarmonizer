// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
package io.github.lemon_ant.jharmonizer.core.translator.spoon;

import edu.umd.cs.findbugs.annotations.Nullable;
import lombok.NonNull;
import lombok.Value;
import spoon.reflect.declaration.CtElement;

/**
 * Captures a single member-ordering violation detected during a check flow.
 *
 * <p>This immutable value class holds the violating element and its immediate predecessor and
 * successor in the correct sorted order, so that diagnostic messages can tell the user exactly
 * where the element should appear.
 *
 * <p>{@code sortedPredecessor} is {@code null} when the element should be the very first member
 * of its scope. {@code sortedSuccessor} is {@code null} when it should be the very last.
 */
@Value
public class MemberRelocation {

    /** The member that is in the wrong position in the original source. */
    @NonNull
    CtElement violatingElement;

    /**
     * The element that immediately precedes the violating element in the correct sorted order,
     * or {@code null} if the violating element should be first.
     */
    @Nullable
    CtElement sortedPredecessor;

    /**
     * The element that immediately follows the violating element in the correct sorted order,
     * or {@code null} if the violating element should be last.
     */
    @Nullable
    CtElement sortedSuccessor;
}
