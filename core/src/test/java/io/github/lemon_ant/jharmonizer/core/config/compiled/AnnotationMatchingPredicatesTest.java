package io.github.lemon_ant.jharmonizer.core.config.compiled;

import static io.github.lemon_ant.jharmonizer.core.config.unified.MemberAccess.PUBLIC;
import static io.github.lemon_ant.jharmonizer.core.config.unified.MemberKind.FIELD;
import static io.github.lemon_ant.jharmonizer.core.config.unified.UnifiedMatchMethod.EXACT;
import static io.github.lemon_ant.jharmonizer.core.config.unified.UnifiedMatchMethod.REGEX;
import static org.assertj.core.api.Assertions.assertThat;

import io.github.lemon_ant.jharmonizer.core.config.unified.MemberDescriptor;
import io.github.lemon_ant.jharmonizer.core.config.unified.UnifiedAnnotationMatcher;
import io.github.lemon_ant.jharmonizer.core.config.unified.UnifiedMemberGroupRuleLine;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

// TODO Refactor
class AnnotationMatchingPredicatesTest {

    @Test
    void createAnnotationExactFqnOrSimple_expectedSimpleNameMatchesQualifiedAnnotationName() {
        MemberDescriptor memberDescriptor = MemberDescriptor.builder()
                .memberKind(FIELD)
                .memberAccess(PUBLIC)
                .name("field")
                .annotationQualifiedName("a.b.Foo")
                .build();

        Predicate<MemberDescriptor> predicate = RuleAtomPredicates.createAnnotationExactFqnOrSimple("Foo");

        assertThat(predicate.test(memberDescriptor)).isTrue();
    }

    @Test
    void createAnnotationExactFqnOrSimple_expectedQualifiedNameMatchesQualifiedAnnotationName() {
        MemberDescriptor memberDescriptor = MemberDescriptor.builder()
                .memberKind(FIELD)
                .memberAccess(PUBLIC)
                .name("field")
                .annotationQualifiedName("a.b.Foo")
                .build();

        Predicate<MemberDescriptor> predicate = RuleAtomPredicates.createAnnotationExactFqnOrSimple("a.b.Foo");

        assertThat(predicate.test(memberDescriptor)).isTrue();
    }

    @Test
    void createAnnotationExactFqnOrSimple_expectedNameDoesNotMatch_returnsFalse() {
        MemberDescriptor memberDescriptor = MemberDescriptor.builder()
                .memberKind(FIELD)
                .memberAccess(PUBLIC)
                .name("field")
                .annotationQualifiedName("a.b.Foo")
                .build();

        Predicate<MemberDescriptor> predicate = RuleAtomPredicates.createAnnotationExactFqnOrSimple("Bar");

        assertThat(predicate.test(memberDescriptor)).isFalse();
    }

    @Test
    void createAnnotationRegexFqnOrSimple_patternMatchesSimpleNameDerivedFromFqcn_returnsTrue() {
        MemberDescriptor memberDescriptor = MemberDescriptor.builder()
                .memberKind(FIELD)
                .memberAccess(PUBLIC)
                .name("field")
                .annotationQualifiedName("a.b.Foo")
                .build();

        Predicate<MemberDescriptor> predicate =
                RuleAtomPredicates.createAnnotationRegexFqnOrSimple(Pattern.compile("F.."));

        assertThat(predicate.test(memberDescriptor)).isTrue();
    }

    @Test
    void compileRuleLine_annotationExact_compiledPredicateMatchesBySimpleName() {
        UnifiedMemberGroupRuleLine unifiedRuleLine = UnifiedMemberGroupRuleLine.builder()
                .annotationMatcher(new UnifiedAnnotationMatcher(EXACT, "Foo"))
                .build();

        Predicate<MemberDescriptor> compiledPredicate = MemberGroupRuleLineCompiler.compileRuleLine(unifiedRuleLine);

        MemberDescriptor annotatedDescriptor = MemberDescriptor.builder()
                .memberKind(FIELD)
                .memberAccess(PUBLIC)
                .name("field")
                .annotationQualifiedName("a.b.Foo")
                .build();

        MemberDescriptor notAnnotatedDescriptor = MemberDescriptor.builder()
                .memberKind(FIELD)
                .memberAccess(PUBLIC)
                .name("field")
                .build();

        assertThat(compiledPredicate.test(annotatedDescriptor)).isTrue();
        assertThat(compiledPredicate.test(notAnnotatedDescriptor)).isFalse();
    }

    @Test
    void compileRuleLine_annotationRegex_compiledPredicateMatchesBySimpleNameOrFqcn() {
        UnifiedMemberGroupRuleLine unifiedRuleLine = UnifiedMemberGroupRuleLine.builder()
                .annotationMatcher(new UnifiedAnnotationMatcher(REGEX, ".*Foo"))
                .build();

        Predicate<MemberDescriptor> compiledPredicate = MemberGroupRuleLineCompiler.compileRuleLine(unifiedRuleLine);

        MemberDescriptor annotatedDescriptor = MemberDescriptor.builder()
                .memberKind(FIELD)
                .memberAccess(PUBLIC)
                .name("field")
                .annotationQualifiedName("a.b.Foo")
                .build();

        assertThat(compiledPredicate.test(annotatedDescriptor)).isTrue();
    }

    @Test
    void compileRuleLine_twoAnnotationMatchers_allMatchersMustMatch() {
        UnifiedMemberGroupRuleLine unifiedRuleLine = UnifiedMemberGroupRuleLine.builder()
                .annotationMatcher(new UnifiedAnnotationMatcher(EXACT, "Foo"))
                .annotationMatcher(new UnifiedAnnotationMatcher(EXACT, "Bar"))
                .build();

        Predicate<MemberDescriptor> compiledPredicate = MemberGroupRuleLineCompiler.compileRuleLine(unifiedRuleLine);

        MemberDescriptor descriptorWithTwoAnnotations = MemberDescriptor.builder()
                .memberKind(FIELD)
                .memberAccess(PUBLIC)
                .name("field")
                .annotationQualifiedName("a.b.Foo")
                .annotationQualifiedName("c.d.Bar")
                .build();

        MemberDescriptor descriptorWithOnlyOneAnnotation = MemberDescriptor.builder()
                .memberKind(FIELD)
                .memberAccess(PUBLIC)
                .name("field")
                .annotationQualifiedName("a.b.Foo")
                .build();

        assertThat(compiledPredicate.test(descriptorWithTwoAnnotations)).isTrue();
        assertThat(compiledPredicate.test(descriptorWithOnlyOneAnnotation)).isFalse();
    }
}
