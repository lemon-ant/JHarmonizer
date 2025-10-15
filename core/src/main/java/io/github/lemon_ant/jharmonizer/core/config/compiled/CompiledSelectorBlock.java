package io.github.lemon_ant.jharmonizer.core.config.compiled;

import java.util.Collections;
import java.util.List;
import lombok.NonNull;
import lombok.Value;

/**
 * OR-semantics over rule lines.
 * Empty includes => true; empty excludes => false.
 */
@Value
public class CompiledSelectorBlock {

    @NonNull
    List<RuleLineMatcher> includeRuleLineMatchers; // immutable, ordered

    @NonNull
    List<RuleLineMatcher> excludeRuleLineMatchers; // immutable, ordered

    public CompiledSelectorBlock(
            @NonNull List<RuleLineMatcher> includeRuleLineMatchers,
            @NonNull List<RuleLineMatcher> excludeRuleLineMatchers) {
        this.includeRuleLineMatchers = Collections.unmodifiableList(includeRuleLineMatchers);
        this.excludeRuleLineMatchers = Collections.unmodifiableList(excludeRuleLineMatchers);
    }

    public boolean match(@NonNull MemberDescriptor descriptor) {
        return matchIncludes(descriptor) && !matchExcludes(descriptor);
    }

    private boolean matchIncludes(MemberDescriptor descriptor) {
        return includeRuleLineMatchers.stream()
                .anyMatch(includeRuleLineMatcher -> includeRuleLineMatcher.test(descriptor));
    }

    private boolean matchExcludes(MemberDescriptor descriptor) {

        return excludeRuleLineMatchers.stream()
                .anyMatch(excludeRuleLineMatcher -> excludeRuleLineMatcher.test(descriptor));
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof CompiledSelectorBlock that)) {
            return false;
        }

        return includeRuleLineMatchers.equals(that.includeRuleLineMatchers)
                && excludeRuleLineMatchers.equals(that.excludeRuleLineMatchers);
    }

    @Override
    public int hashCode() {
        int result = includeRuleLineMatchers.hashCode();
        result = 31 * result + excludeRuleLineMatchers.hashCode();
        return result;
    }
}
