package io.github.lemon_ant.jharmonizer.core.translator;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class SerializationStatisticTest {

    @Test
    void equals_sameInstance_returnsTrue() {
        // Given
        SerializationStatistic statistic = new SerializationStatistic(100L, 5000L);

        // When / Then
        assertThat(statistic).isEqualTo(statistic);
    }

    @Test
    void equals_nullObject_returnsFalse() {
        // Given
        SerializationStatistic statistic = new SerializationStatistic(100L, 5000L);

        // When / Then
        assertThat(statistic).isNotEqualTo(null);
    }

    @Test
    void equals_differentClass_returnsFalse() {
        // Given
        SerializationStatistic statistic = new SerializationStatistic(100L, 5000L);

        // When / Then
        assertThat(statistic).isNotEqualTo("not a statistic");
    }

    @Test
    void equals_sameFieldValues_returnsTrue() {
        // Given
        SerializationStatistic first = new SerializationStatistic(100L, 5000L);
        SerializationStatistic second = new SerializationStatistic(100L, 5000L);

        // When / Then
        assertThat(first).isEqualTo(second);
    }

    @Test
    void equals_differentSerializedCodeLength_returnsFalse() {
        // Given
        SerializationStatistic first = new SerializationStatistic(100L, 5000L);
        SerializationStatistic second = new SerializationStatistic(200L, 5000L);

        // When / Then
        assertThat(first).isNotEqualTo(second);
    }

    @Test
    void equals_differentProcessingTimeInNanos_returnsFalse() {
        // Given
        SerializationStatistic first = new SerializationStatistic(100L, 5000L);
        SerializationStatistic second = new SerializationStatistic(100L, 9999L);

        // When / Then
        assertThat(first).isNotEqualTo(second);
    }

    @Test
    void hashCode_equalObjects_produceSameHashCode() {
        // Given
        SerializationStatistic first = new SerializationStatistic(100L, 5000L);
        SerializationStatistic second = new SerializationStatistic(100L, 5000L);

        // When / Then
        assertThat(first.hashCode()).isEqualTo(second.hashCode());
    }

    @Test
    void toString_validInstance_containsFieldValues() {
        // Given
        SerializationStatistic statistic = new SerializationStatistic(100L, 5000L);

        // When
        String result = statistic.toString();

        // Then
        assertThat(result).contains("100").contains("5000");
    }
}
