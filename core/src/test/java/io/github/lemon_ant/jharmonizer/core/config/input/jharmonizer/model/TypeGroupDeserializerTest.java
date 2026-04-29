package io.github.lemon_ant.jharmonizer.core.config.input.jharmonizer.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.Test;

class TypeGroupDeserializerTest {

    private final ObjectMapper mapper = new ObjectMapper(new YAMLFactory());

    @Test
    void deserialize_listOfTypeKindStrings_returnsEntryWithMultipleKinds() throws IOException {
        // Given
        String yaml = "- [class, INTERFACE, RECORD]";

        // When
        List<JHarmonizerTopLevelTypeSelector> result = mapper.readValue(
                yaml,
                mapper.getTypeFactory().constructCollectionType(List.class, JHarmonizerTopLevelTypeSelector.class));

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getTypeKinds())
                .containsExactly(JHarmonizerTypeKind.CLASS, JHarmonizerTypeKind.INTERFACE, JHarmonizerTypeKind.RECORD);
    }

    @Test
    void deserialize_unknownTypeKind_throwsException() {
        // Given
        String yaml = "- UNICORN";

        // When
        Throwable thrown = catchThrowable(() -> mapper.readValue(
                yaml,
                mapper.getTypeFactory().constructCollectionType(List.class, JHarmonizerTopLevelTypeSelector.class)));

        // Then
        assertThat(thrown).isInstanceOf(Exception.class).hasMessageContaining("UNICORN");
    }
}
