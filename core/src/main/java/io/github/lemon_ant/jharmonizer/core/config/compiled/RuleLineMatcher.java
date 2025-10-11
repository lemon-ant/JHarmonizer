package io.github.lemon_ant.jharmonizer.core.config.compiled;

import io.github.lemon_ant.jharmonizer.core.config.unified.MemberDescriptor;
import java.util.function.Predicate;

/**
 * One compiled function per rule line: no branching in runtime.
 * The converter composes all atomic checks (mask, name, annotations) into this single function.
 */
@FunctionalInterface
public interface RuleLineMatcher extends Predicate<MemberDescriptor> {
    // Inherits boolean test(MemberDescriptor descriptor)
}
