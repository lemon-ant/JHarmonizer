package io.github.lemon_ant.jharmonizer.core.translator;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class SerializationStatisticTest {

    @Test
    void toString_validInstance_containsFieldValues() {
        // Given
        SerializationStatistic statistic = new SerializationStatistic(100L, 5000L);

        // When
        String serializedStatistic = statistic.toString();

        // Then
        assertThat(serializedStatistic).contains("100").contains("5000");
    }
}
