// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
package io.github.lemon_ant.jharmonizer.core.config.compiled;

// @jharmonizer:fully-off
// jharmonizer v1.0.1 incorrectly reorders @Value class fields, breaking Lombok constructors;
// remove this directive once jharmonizer is upgraded to a version that fixes the @Value field-ordering bug.
import io.github.lemon_ant.jharmonizer.core.config.unified.MemberDescriptor;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;
import lombok.NonNull;
import lombok.Value;

/**
 * OR-semantics over rule lines.
 * Empty includes => true; empty excludes => false.
 */
@Value
public class CompiledMemberGroupSelectorBlock {
    @NonNull
    List<Predicate<MemberDescriptor>> excludePredicate; // immutable, ordered

    @NonNull
    List<Predicate<MemberDescriptor>> includePredicate; // immutable, ordered

    /**
     * Creates a new CompiledMemberGroupSelectorBlock.
     * @param includePredicate the include predicate
     * @param excludePredicate the exclude predicate
     */
    CompiledMemberGroupSelectorBlock(
            @NonNull List<Predicate<MemberDescriptor>> includePredicate,
            @NonNull List<Predicate<MemberDescriptor>> excludePredicate) {
        this.includePredicate = Collections.unmodifiableList(includePredicate);
        this.excludePredicate = Collections.unmodifiableList(excludePredicate);
    }

    /**
     * Performs the match.
     * @param descriptor the member descriptor to inspect
     * @return {@code true} if match; otherwise {@code false}
     */
    public boolean match(@NonNull MemberDescriptor descriptor) {
        return matchIncludes(descriptor) && !matchExcludes(descriptor);
    }

    private boolean matchExcludes(MemberDescriptor descriptor) {
        return excludePredicate.stream().anyMatch(excludePredicate -> excludePredicate.test(descriptor));
    }

    private boolean matchIncludes(MemberDescriptor descriptor) {
        return includePredicate.isEmpty()
                || includePredicate.stream().anyMatch(includePredicate -> includePredicate.test(descriptor));
    }
}
