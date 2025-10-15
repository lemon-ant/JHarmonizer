package io.github.lemon_ant.jharmonizer.core.config.compiled;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * Validates DeclarationModifier applicability/conflict rules by TargetCategory.
 * This is a structural smoke test that assumes the model exposes such contracts.
 */
class DeclarationModifierTest {

    @Test
    void getApplicableTargets_forSealed_containsOnlyTypes() {
        assertThat(DeclarationModifier.SEALED.getApplicableTargets()).containsExactly(TargetCategory.TYPE);
    }

    @Test
    void hasConflictWith_forSealedAgainstNonSealed_returnTrue() {
        assertThat(DeclarationModifier.SEALED.hasConflictWith(DeclarationModifier.NON_SEALED))
                .isTrue();
    }

    @Test
    void hasConflictWith_forNonSealedAgainstSealed_returnTrue() {
        boolean conflict = DeclarationModifier.SEALED.hasConflictWith(DeclarationModifier.NON_SEALED);
        assertThat(DeclarationModifier.NON_SEALED.hasConflictWith(DeclarationModifier.SEALED))
                .isTrue();
    }
}
