package io.github.lemon_ant.jharmonizer.core.config.unified;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * Validates DeclarationModifier applicability/conflict rules by TargetCategory.
 * This is a structural smoke test that assumes the model exposes such contracts.
 */
class DeclarationModifierApplicabilityTest {

    @Test
    void modifiers_shouldAllowSealedOnlyForTypes() {
        boolean applicableToTypes = DeclarationModifier.SEALED.getApplicableTargets().contains(TargetCategory.TYPE);
        boolean applicableToMethods = DeclarationModifier.SEALED.getApplicableTargets().contains(TargetCategory.METHOD);

        assertThat(applicableToTypes).isTrue();
        assertThat(applicableToMethods).isFalse();
    }

    @Test
    void modifiers_conflicts_nonSealed_vs_sealed() {
        boolean conflict = DeclarationModifier.SEALED.hasConflictWith(DeclarationModifier.NON_SEALED)
                && DeclarationModifier.NON_SEALED.hasConflictWith(DeclarationModifier.SEALED);
        assertThat(conflict).isTrue();
    }
}
