package io.github.lemon_ant.jharmonizer.core.config.unified;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import org.junit.jupiter.api.Test;

class NameAndAnnotationMatcherTest {

    @Test
    void nameMatcher_exact_shouldMatchOnlyIdentical() {
        UnifiedRuleLine rule = UnifiedRuleLine.builder()
                .names(Set.of(new NameMatcher(NameMatchKind.EXACT, "toString")))
                .build();

        assertThat(matchesName(rule, "toString")).isTrue();
        assertThat(matchesName(rule, "ToString")).isFalse();
        assertThat(matchesName(rule, "toStringExtended")).isFalse();
    }

    @Test
    void nameMatcher_regex_shouldMatchPattern() {
        UnifiedRuleLine rule = UnifiedRuleLine.builder()
                .names(Set.of(new NameMatcher(NameMatchKind.REGEX, "get[A-Z].*")))
                .build();

        assertThat(matchesName(rule, "getValue")).isTrue();
        assertThat(matchesName(rule, "setValue")).isFalse();
    }

    @Test
    void annotationMatcher_exact_and_regex() {
        UnifiedRuleLine rule = UnifiedRuleLine.builder()
                .annotations(Set.of(
                        new AnnotationMatcher(NameMatchKind.EXACT, "Nonnull"),
                        new AnnotationMatcher(NameMatchKind.REGEX, "Size|Length")
                ))
                .build();

        assertThat(matchesAnnotation(rule, "Nonnull")).isTrue();
        assertThat(matchesAnnotation(rule, "Size")).isTrue();
        assertThat(matchesAnnotation(rule, "Min")).isFalse();
    }

    private static boolean matchesName(UnifiedRuleLine rule, String name) {
        if (rule.getNames().isEmpty()) return true;
        return rule.getNames().stream().anyMatch(n -> switch (n.getKind()) {
            case EXACT -> name.equals(n.getValue());
            case REGEX -> name.matches(n.getValue());
        });
    }

    private static boolean matchesAnnotation(UnifiedRuleLine rule, String annoSimpleName) {
        if (rule.getAnnotations().isEmpty()) return true;
        return rule.getAnnotations().stream().anyMatch(a -> switch (a.getNameMatchKind()) {
            case EXACT -> annoSimpleName.equals(a.getValue());
            case REGEX -> annoSimpleName.matches(a.getValue());
        });
    }
}
