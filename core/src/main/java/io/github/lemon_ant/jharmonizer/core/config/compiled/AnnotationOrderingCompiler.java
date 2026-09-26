// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
package io.github.lemon_ant.jharmonizer.core.config.compiled;

import io.github.lemon_ant.jharmonizer.core.config.unified.AnnotationDescriptor;
import io.github.lemon_ant.jharmonizer.core.config.unified.UnifiedAnnotationOrderingRule;
import java.util.Comparator;
import java.util.List;
import lombok.NonNull;
import lombok.experimental.UtilityClass;

/** Compiles annotation criteria into one reusable comparator. */
@UtilityClass
class AnnotationOrderingCompiler {

    /**
     * Chains criteria in their configured priority order.
     * @param rules annotation ordering criteria; empty preserves order
     * @return compiled comparator, retaining source order on complete ties
     */
    @NonNull
    static Comparator<AnnotationDescriptor> compile(@NonNull List<UnifiedAnnotationOrderingRule> rules) {
        return rules.stream()
                .map(AnnotationOrderingCompiler::compileRule)
                .reduce(Comparator::thenComparing)
                .orElse((left, right) -> 0);
    }

    @NonNull
    private static Comparator<AnnotationDescriptor> compileRule(UnifiedAnnotationOrderingRule rule) {
        return switch (rule) {
            case ALPHA -> Comparator.comparing(AnnotationDescriptor::getName);
            case NAME_LENGTH_ASC ->
                Comparator.comparingInt(annotation -> annotation.getName().length());
            case DECLARATION_LENGTH_ASC -> Comparator.comparingInt(AnnotationDescriptor::getDeclarationLength);
            case ARGUMENTS_ALPHA ->
                Comparator.comparing(
                        AnnotationDescriptor::getArguments, Comparator.nullsFirst(Comparator.naturalOrder()));
        };
    }
}
