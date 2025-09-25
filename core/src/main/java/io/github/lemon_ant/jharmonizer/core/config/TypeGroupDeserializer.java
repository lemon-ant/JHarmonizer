package io.github.lemon_ant.jharmonizer.core.config;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;

class TypeGroupDeserializer extends JsonDeserializer<TypeGroup> {

    @Override
    public TypeGroup deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        ObjectCodec codec = p.getCodec();
        JsonNode node = codec.readTree(p);

        Set<TypeKind> result = new TreeSet<>();

        if (node.isTextual()) {
            result.add(EnumDeserializerUtil.deserialize(TypeKind.class, node.asText()));
        } else if (node.isArray()) {
            for (JsonNode child : node) {
                if (!child.isTextual()) {
                    throw new IOException("Expected string in array");
                }
                result.add(EnumDeserializerUtil.deserialize(TypeKind.class, child.asText()));
            }
        } else {
            throw new IOException("Unsupported type-order entry: " + node);
        }

        return new TypeGroup(Collections.unmodifiableSet(result));
    }
}
