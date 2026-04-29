package io.github.lemon_ant.jharmonizer.core.config.unified;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * Validates DeclarationModifier applicability/conflict rules by TargetCategory.
 * This is a structural smoke test that assumes the model exposes such contracts.
 */
class DeclarationModifierTest {

    @Test
    void getApplicableTargets_forSealed_containsOnlyTypes() {
        // When
        Set<TargetCategory> applicableTargets = DeclarationModifier.SEALED.getApplicableTargets();

        // Then
        assertThat(applicableTargets).containsExactly(TargetCategory.TYPE);
    }

    @Test
    void hasConflictWith_forNonSealedAgainstSealed_returnTrue() {
        // When / Then
        assertThat(DeclarationModifier.NON_SEALED.hasConflictWith(DeclarationModifier.SEALED))
                .isTrue();
    }

    @Test
    void hasConflictWith_forSealedAgainstNonSealed_returnTrue() {
        // When / Then
        assertThat(DeclarationModifier.SEALED.hasConflictWith(DeclarationModifier.NON_SEALED))
                .isTrue();
    }
}
