package io.github.lemon_ant.jharmonizer.core.optout;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;
import spoon.reflect.declaration.CtType;
import spoon.reflect.factory.Factory;
import spoon.reflect.factory.FactoryImpl;
import spoon.support.DefaultCoreFactory;
import spoon.support.StandardEnvironment;

class JHarmonizerOptOutsTest {

    @Test
    void empty_returnsInstanceWithNoOptOuts() {
        // When
        JHarmonizerOptOuts optOuts = JHarmonizerOptOuts.empty();

        // Then
        assertThat(optOuts.isEmpty()).isTrue();
        assertThat(optOuts.getFileOptOutMode()).isEmpty();
        assertThat(optOuts.getTypeOptOutModes()).isEmpty();
    }

    @Test
    void isEmpty_withFileOptOutMode_returnsFalse() {
        // Given
        JHarmonizerOptOuts optOuts = new JHarmonizerOptOuts(JHarmonizerOptOutMode.FULLY_OFF, Map.of());

        // When / Then
        assertThat(optOuts.isEmpty()).isFalse();
    }

    @Test
    void isEmpty_withTypeOptOutMode_returnsFalse() {
        // Given
        CtType<?> ctType = createCtType();
        JHarmonizerOptOuts optOuts = new JHarmonizerOptOuts(null, Map.of(ctType, JHarmonizerOptOutMode.SORTING_OFF));

        // When / Then
        assertThat(optOuts.isEmpty()).isFalse();
    }

    @Test
    void isEmpty_withNullFileOptOutAndEmptyTypes_returnsTrue() {
        // Given
        JHarmonizerOptOuts optOuts = new JHarmonizerOptOuts(null, Map.of());

        // When / Then
        assertThat(optOuts.isEmpty()).isTrue();
    }

    @Test
    void hasFileOptOutMode_matchingMode_returnsTrue() {
        // Given
        JHarmonizerOptOuts optOuts = new JHarmonizerOptOuts(JHarmonizerOptOutMode.FULLY_OFF, Map.of());

        // When / Then
        assertThat(optOuts.hasFileOptOutMode(JHarmonizerOptOutMode.FULLY_OFF)).isTrue();
    }

    @Test
    void hasFileOptOutMode_differentMode_returnsFalse() {
        // Given
        JHarmonizerOptOuts optOuts = new JHarmonizerOptOuts(JHarmonizerOptOutMode.SORTING_OFF, Map.of());

        // When / Then
        assertThat(optOuts.hasFileOptOutMode(JHarmonizerOptOutMode.FULLY_OFF)).isFalse();
    }

    @Test
    void hasFileOptOutMode_nullFileOptOut_returnsFalse() {
        // Given
        JHarmonizerOptOuts optOuts = new JHarmonizerOptOuts(null, Map.of());

        // When / Then
        assertThat(optOuts.hasFileOptOutMode(JHarmonizerOptOutMode.FULLY_OFF)).isFalse();
    }

    @Test
    void findTypeOptOutMode_knownType_returnsMode() {
        // Given
        CtType<?> ctType = createCtType();
        JHarmonizerOptOuts optOuts = new JHarmonizerOptOuts(null, Map.of(ctType, JHarmonizerOptOutMode.SORTING_OFF));

        // When / Then
        assertThat(optOuts.findTypeOptOutMode(ctType)).contains(JHarmonizerOptOutMode.SORTING_OFF);
    }

    @Test
    void getSortingSkippedTypes_noOptOuts_returnsEmptySet() {
        // Given
        JHarmonizerOptOuts optOuts = JHarmonizerOptOuts.empty();

        // When
        Set<CtType<?>> sortingSkipped = optOuts.getSortingSkippedTypes();

        // Then
        assertThat(sortingSkipped).isEmpty();
    }

    @Test
    void getFileOptOutMode_withMode_returnsPresent() {
        // Given
        JHarmonizerOptOuts optOuts = new JHarmonizerOptOuts(JHarmonizerOptOutMode.FULLY_OFF, Map.of());

        // When / Then
        assertThat(optOuts.getFileOptOutMode()).contains(JHarmonizerOptOutMode.FULLY_OFF);
    }

    @Test
    void getFileOptOutMode_withoutMode_returnsEmpty() {
        // Given
        JHarmonizerOptOuts optOuts = new JHarmonizerOptOuts(null, Map.of());

        // When / Then
        assertThat(optOuts.getFileOptOutMode()).isEmpty();
    }

    @SuppressWarnings("unchecked")
    private static CtType<?> createCtType() {
        Factory factory = new FactoryImpl(new DefaultCoreFactory(), new StandardEnvironment());
        return factory.Core().createClass();
    }
}
