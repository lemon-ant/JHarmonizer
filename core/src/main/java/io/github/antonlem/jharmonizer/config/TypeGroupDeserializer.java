package io.github.antonlem.jharmonizer.config;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.*;
import java.io.IOException;
import java.util.*;

class TypeGroupDeserializer extends JsonDeserializer<TypeGroup> {

    @Override
    public TypeGroup deserialize(JsonParser parser, DeserializationContext ctxt) throws IOException {
        ObjectCodec codec = parser.getCodec();
        JsonNode node = codec.readTree(parser);

        Set<TypeKind> result = new TreeSet<>();

        if (node.isTextual()) {
            result.add(EnumDeserializerUtil.deserialize(TypeKind.class, node.asText()));
        } else if (node.isArray()) {
            for (JsonNode child : node) {
                if (!child.isTextual()) throw new IOException("Expected string in array");
                result.add(EnumDeserializerUtil.deserialize(TypeKind.class, child.asText()));
            }
        } else {
            throw new IOException("Unsupported type-order entry: " + node);
        }

        return new TypeGroup(Collections.unmodifiableSet(result));
    }
}
