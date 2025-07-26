package io.github.antonlem.jharmonizer.config;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.Test;

class TypeOrderEntryDeserializerTest {

    private final ObjectMapper mapper = new ObjectMapper(new YAMLFactory());

    @Test
    void deserialize_listOfTypeKindStrings_returnsEntryWithMultipleKinds() throws IOException {
        String yaml = "- [class, interface, record]";
        List<TypeOrderEntry> result = mapper.readValue(
                yaml, mapper.getTypeFactory().constructCollectionType(List.class, TypeOrderEntry.class));

        assertEquals(1, result.size());
        assertEquals(
                List.of(TypeKind.class_, TypeKind.interface_, TypeKind.record_),
                result.get(0).getKinds());
    }

    @Test
    void deserialize_unknownTypeKind_throwsException() {
        String yaml = "- unicorn";

        Exception exception = assertThrows(Exception.class, () -> {
            mapper.readValue(yaml, mapper.getTypeFactory().constructCollectionType(List.class, TypeOrderEntry.class));
        });

        assertTrue(exception.getMessage().toLowerCase().contains("unicorn"));
    }
}
