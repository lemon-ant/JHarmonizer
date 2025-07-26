package io.github.antonlem.jharmonizer.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

class TypeOrderEntryDeserializerTest {

    private final ObjectMapper mapper = new ObjectMapper(new YAMLFactory());

    @Test
    void deserialize_listOfTypeKindStrings_returnsEntryWithMultipleKinds() throws IOException {
        // given
        String yaml = "- [class, INTERFACE, RECORD]";

        // when
        List<TypeOrderEntry> result = mapper.readValue(
            yaml,
            mapper.getTypeFactory().constructCollectionType(List.class, TypeOrderEntry.class)
        );

        // then
        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getKinds())
            .containsExactly(TypeKind.CLASS, TypeKind.INTERFACE, TypeKind.RECORD);
    }

    @Test
    void deserialize_unknownTypeKind_throwsException() {
        // given
        String yaml = "- UNICORN";

        // when/then
        assertThatThrownBy(() -> mapper.readValue(
            yaml,
            mapper.getTypeFactory().constructCollectionType(List.class, TypeOrderEntry.class)
        ))
            .isInstanceOf(Exception.class)
            .hasMessageContaining("UNICORN");
    }
}
