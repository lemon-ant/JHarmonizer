package io.github.lemon_ant.jharmonizer.core.config.compiled;

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
     * Checks whether this compiled member group selector block matches another object.
     * @param o the object to compare with
     * @return {@code true} if the check succeeds; otherwise {@code false}
     */
    @Override
    public boolean equals(Object o) {
        if (!(o instanceof CompiledMemberGroupSelectorBlock that)) {
            return false;
        }

        return includePredicate.equals(that.includePredicate) && excludePredicate.equals(that.excludePredicate);
    }

    /**
     * Returns the hash code of this compiled member group selector block.
     * @return the hash code value
     */
    @Override
    public int hashCode() {
        int result = includePredicate.hashCode();
        result = 31 * result + excludePredicate.hashCode();
        return result;
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
