package io.github.antonlem.jharmonizer.config;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.*;
import java.io.IOException;
import java.util.*;

class TypeOrderEntryDeserializer extends JsonDeserializer<TypeGroup> {

    @Override
    public TypeGroup deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        ObjectCodec codec = p.getCodec();
        JsonNode node = codec.readTree(p);

        List<TypeKind> result = new ArrayList<>();

        if (node.isTextual()) {
            result.add(TypeKind.fromString(node.asText()));
        } else if (node.isArray()) {
            for (JsonNode child : node) {
                if (!child.isTextual()) throw new IOException("Expected string in array");
                result.add(TypeKind.fromString(child.asText()));
            }
        } else {
            throw new IOException("Unsupported type-order entry: " + node);
        }

        return new TypeGroup(result);
    }
}
