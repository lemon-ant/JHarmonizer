package io.github.lemon_ant.jharmonizer.core.config.unified;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * Verifies unified selector semantics:
 * accept = any(includes) && !any(excludes)
 * empty includes -> true, empty excludes -> false
 */
class UnifiedSelectorBlockSemanticsTest {

    @Test
    void accept_shouldBeTrue_whenIncludesEmptyAndExcludesEmpty() {
        UnifiedSelectorBlock block = UnifiedSelectorBlock.builder().build();
        // any(includes)=true (empty -> true), any(excludes)=false (empty -> false)
        boolean accepted = matches(block, sampleRuleContext());
        assertThat(accepted).isTrue();
    }

    @Test
    void accept_shouldBeTrue_whenAnyIncludeMatches_andNoExcludeMatches() {
        UnifiedRuleLine includeA = UnifiedRuleLine.builder()
                .kinds(Set.of(MemberKind.FIELD))
                .build();
        UnifiedRuleLine includeB = UnifiedRuleLine.builder()
                .kinds(Set.of(MemberKind.METHOD))
                .build();

        UnifiedSelectorBlock block = UnifiedSelectorBlock.builder()
                .include(includeA)
                .include(includeB)
                .build();

        boolean accepted = matches(block, MatchingContext.of(MemberKind.METHOD));
        assertThat(accepted).isTrue();
    }

    @Test
    void accept_shouldBeFalse_whenNoIncludeMatches_andNoExcludeMatches() {
        UnifiedRuleLine includeField = UnifiedRuleLine.builder()
                .kinds(Set.of(MemberKind.FIELD))
                .build();
        UnifiedSelectorBlock block = UnifiedSelectorBlock.builder()
                .include(includeField)
                .build();

        boolean accepted = matches(block, MatchingContext.of(MemberKind.CONSTRUCTOR));
        assertThat(accepted).isFalse();
    }

    @Test
    void accept_shouldBeFalse_whenExcludeMatches_evenIfIncludeMatches() {
        UnifiedRuleLine includeMethod = UnifiedRuleLine.builder()
                .kinds(Set.of(MemberKind.METHOD))
                .build();
        UnifiedRuleLine excludeByName = UnifiedRuleLine.builder()
                .kinds(Set.of(MemberKind.METHOD))
                .names(Set.of(new NameMatcher(NameMatchKind.EXACT, "danger")))
                .build();

        UnifiedSelectorBlock block = UnifiedSelectorBlock.builder()
                .include(includeMethod)
                .exclude(excludeByName)
                .build();

        boolean accepted = matches(block, MatchingContext.of(MemberKind.METHOD, "danger"));
        assertThat(accepted).isFalse();
    }

    // --- Minimal fake matching just for model semantics demonstration ---
    private static boolean matches(UnifiedSelectorBlock block, MatchingContext ctx) {
        boolean includeOk = block.getIncludes().isEmpty()
                || block.getIncludes().stream().anyMatch(rule -> matchesRule(rule, ctx));
        boolean excludeHit = !block.getExcludes().isEmpty()
                && block.getExcludes().stream().anyMatch(rule -> matchesRule(rule, ctx));
        return includeOk && !excludeHit;
    }

    private static boolean matchesRule(UnifiedRuleLine rule, MatchingContext ctx) {
        boolean kindOk = rule.getKinds().isEmpty() || rule.getKinds().contains(ctx.kind);
        boolean nameOk = rule.getNames().isEmpty()
                || rule.getNames().stream().anyMatch(n -> switch (n.getKind()) {
                    case EXACT -> ctx.name != null && ctx.name.equals(n.getValue());
                    case REGEX -> ctx.name != null && ctx.name.matches(n.getValue());
                });
        return kindOk && nameOk; // Minimal conjunction for this smoke test
    }

    private record MatchingContext(MemberKind kind, String name) {
        static MatchingContext of(MemberKind kind) { return new MatchingContext(kind, null); }
        static MatchingContext of(MemberKind kind, String name) { return new MatchingContext(kind, name); }
    }

    private static MatchingContext sampleRuleContext() {
        return MatchingContext.of(MemberKind.FIELD);
    }
}
