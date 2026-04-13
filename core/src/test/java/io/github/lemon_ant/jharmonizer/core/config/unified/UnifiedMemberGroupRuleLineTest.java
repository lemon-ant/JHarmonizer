package io.github.lemon_ant.jharmonizer.core.config.unified;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class UnifiedMemberGroupRuleLineTest {

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
            assertThatThrownBy(() -> UnifiedMemberGroupRuleLine.builder()
                            .nameMatcher(null)
                            .build())
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("at least one selector");
        }
    }
}
