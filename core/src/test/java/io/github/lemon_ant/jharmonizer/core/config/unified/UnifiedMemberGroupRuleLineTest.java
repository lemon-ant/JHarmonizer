package io.github.lemon_ant.jharmonizer.core.config.unified;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class UnifiedMemberGroupRuleLineTest {

    private static final UnifiedMemberGroupRuleLine FIELD_RULE_LINE =
            UnifiedMemberGroupRuleLine.builder().memberKind(MemberKind.FIELD).build();

    @Nested
    class Equality {

        @Test
        void equals_sameInstance_returnsTrue() {
            // When / Then
            assertThat(FIELD_RULE_LINE).isEqualTo(FIELD_RULE_LINE);
        }

        @Test
        void equals_nonRuleLineObject_returnsFalse() {
            // When / Then
            assertThat(FIELD_RULE_LINE).isNotEqualTo("not a rule line");
        }

        @Test
        void equals_differentMemberKinds_returnsFalse() {
            // Given
            UnifiedMemberGroupRuleLine methodRuleLine = UnifiedMemberGroupRuleLine.builder()
                    .memberKind(MemberKind.METHOD)
                    .build();

            // When / Then
            assertThat(FIELD_RULE_LINE).isNotEqualTo(methodRuleLine);
        }

        @Test
        void equals_differentMemberAccesses_returnsFalse() {
            // Given
            UnifiedMemberGroupRuleLine publicFieldRule = UnifiedMemberGroupRuleLine.builder()
                    .memberKind(MemberKind.FIELD)
                    .memberAccess(MemberAccess.PUBLIC)
                    .build();
            UnifiedMemberGroupRuleLine privateFieldRule = UnifiedMemberGroupRuleLine.builder()
                    .memberKind(MemberKind.FIELD)
                    .memberAccess(MemberAccess.PRIVATE)
                    .build();

            // When / Then
            assertThat(publicFieldRule).isNotEqualTo(privateFieldRule);
        }

        @Test
        void equals_differentDeclarationModifiers_returnsFalse() {
            // Given
            UnifiedMemberGroupRuleLine staticFieldRule = UnifiedMemberGroupRuleLine.builder()
                    .memberKind(MemberKind.FIELD)
                    .declarationModifier(DeclarationModifier.STATIC)
                    .build();
            UnifiedMemberGroupRuleLine finalFieldRule = UnifiedMemberGroupRuleLine.builder()
                    .memberKind(MemberKind.FIELD)
                    .declarationModifier(DeclarationModifier.FINAL)
                    .build();

            // When / Then
            assertThat(staticFieldRule).isNotEqualTo(finalFieldRule);
        }

        @Test
        void equals_differentNameMatchers_returnsFalse() {
            // Given
            UnifiedMemberGroupRuleLine ruleWithName = UnifiedMemberGroupRuleLine.builder()
                    .memberKind(MemberKind.FIELD)
                    .nameMatcher(new UnifiedNameMatcher(UnifiedMatchMethod.EXACT, "fieldA"))
                    .build();
            UnifiedMemberGroupRuleLine ruleWithDifferentName = UnifiedMemberGroupRuleLine.builder()
                    .memberKind(MemberKind.FIELD)
                    .nameMatcher(new UnifiedNameMatcher(UnifiedMatchMethod.EXACT, "fieldB"))
                    .build();

            // When / Then
            assertThat(ruleWithName).isNotEqualTo(ruleWithDifferentName);
        }

        @Test
        void equals_differentAnnotationMatchers_returnsFalse() {
            // Given
            UnifiedMemberGroupRuleLine ruleWithAnnotation = UnifiedMemberGroupRuleLine.builder()
                    .memberKind(MemberKind.FIELD)
                    .annotationMatcher(new UnifiedAnnotationMatcher(UnifiedMatchMethod.EXACT, "Override"))
                    .build();
            UnifiedMemberGroupRuleLine ruleWithNoAnnotation = UnifiedMemberGroupRuleLine.builder()
                    .memberKind(MemberKind.FIELD)
                    .build();

            // When / Then
            assertThat(ruleWithAnnotation).isNotEqualTo(ruleWithNoAnnotation);
        }

        @Test
        void equals_identicalRuleLines_returnsTrue() {
            // Given
            UnifiedMemberGroupRuleLine firstRuleLine = UnifiedMemberGroupRuleLine.builder()
                    .memberKind(MemberKind.FIELD)
                    .memberAccess(MemberAccess.PUBLIC)
                    .build();
            UnifiedMemberGroupRuleLine secondRuleLine = UnifiedMemberGroupRuleLine.builder()
                    .memberKind(MemberKind.FIELD)
                    .memberAccess(MemberAccess.PUBLIC)
                    .build();

            // When / Then
            assertThat(firstRuleLine).isEqualTo(secondRuleLine);
        }
    }

    @Nested
    class HashCode {

        @Test
        void hashCode_identicalRuleLines_produceSameValue() {
            // Given
            UnifiedMemberGroupRuleLine firstRuleLine = UnifiedMemberGroupRuleLine.builder()
                    .memberKind(MemberKind.FIELD)
                    .build();
            UnifiedMemberGroupRuleLine secondRuleLine = UnifiedMemberGroupRuleLine.builder()
                    .memberKind(MemberKind.FIELD)
                    .build();

            // When / Then
            assertThat(firstRuleLine.hashCode()).isEqualTo(secondRuleLine.hashCode());
        }
    }

    @Nested
    class HasAnySelectorConfigured {

        @Test
        void build_onlyMemberAccessConfigured_buildsSuccessfully() {
            // When
            UnifiedMemberGroupRuleLine ruleLine = UnifiedMemberGroupRuleLine.builder()
                    .memberAccess(MemberAccess.PUBLIC)
                    .build();

            // Then
            assertThat(ruleLine.getMemberAccesses()).containsExactly(MemberAccess.PUBLIC);
        }

        @Test
        void build_onlyDeclarationModifierConfigured_buildsSuccessfully() {
            // When
            UnifiedMemberGroupRuleLine ruleLine = UnifiedMemberGroupRuleLine.builder()
                    .declarationModifier(DeclarationModifier.STATIC)
                    .build();

            // Then
            assertThat(ruleLine.getDeclarationModifiers()).containsExactly(DeclarationModifier.STATIC);
        }

        @Test
        void build_onlyAnnotationMatcherConfigured_buildsSuccessfully() {
            // When
            UnifiedMemberGroupRuleLine ruleLine = UnifiedMemberGroupRuleLine.builder()
                    .annotationMatcher(new UnifiedAnnotationMatcher(UnifiedMatchMethod.EXACT, "Override"))
                    .build();

            // Then
            assertThat(ruleLine.getAnnotationMatchers()).hasSize(1);
        }

        @Test
        void build_onlyNameMatcherConfigured_buildsSuccessfully() {
            // When
            UnifiedMemberGroupRuleLine ruleLine = UnifiedMemberGroupRuleLine.builder()
                    .nameMatcher(new UnifiedNameMatcher(UnifiedMatchMethod.EXACT, "fieldA"))
                    .build();

            // Then
            assertThat(ruleLine.getNameMatcher()).isNotNull();
        }

        @Test
        void build_nullNameMatcherWithNoOtherSelectors_throwsIllegalArgumentException() {
            // When / Then
            org.assertj.core.api.Assertions.assertThatThrownBy(() -> UnifiedMemberGroupRuleLine.builder()
                            .nameMatcher(null)
                            .build())
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("at least one selector");
        }
    }
}
