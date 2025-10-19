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
    List<Predicate<MemberDescriptor>> includePredicate; // immutable, ordered

    @NonNull
    List<Predicate<MemberDescriptor>> excludePredicate; // immutable, ordered

    CompiledMemberGroupSelectorBlock(
            @NonNull List<Predicate<MemberDescriptor>> includePredicate,
            @NonNull List<Predicate<MemberDescriptor>> excludePredicate) {
        this.includePredicate = Collections.unmodifiableList(includePredicate);
        this.excludePredicate = Collections.unmodifiableList(excludePredicate);
    }

    public boolean match(@NonNull MemberDescriptor descriptor) {
        return matchIncludes(descriptor) && !matchExcludes(descriptor);
    }

    private boolean matchIncludes(MemberDescriptor descriptor) {
        return includePredicate.stream().anyMatch(includePredicate -> includePredicate.test(descriptor));
    }

    private boolean matchExcludes(MemberDescriptor descriptor) {

        return excludePredicate.stream().anyMatch(excludePredicate -> excludePredicate.test(descriptor));
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof CompiledMemberGroupSelectorBlock that)) {
            return false;
        }

        return includePredicate.equals(that.includePredicate) && excludePredicate.equals(that.excludePredicate);
    }

    @Override
    public int hashCode() {
        int result = includePredicate.hashCode();
        result = 31 * result + excludePredicate.hashCode();
        return result;
    }
}
