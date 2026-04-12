package io.github.antonlem.jharmonizer.config;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.*;
import java.io.IOException;
import java.util.*;
import org.apache.commons.lang3.StringUtils;

class SortKeysDeserializer extends JsonDeserializer<List<SortKey>> {

    @Override
    @SuppressWarnings("PMD.CyclomaticComplexity")
    public List<SortKey> deserialize(JsonParser parser, DeserializationContext ctxt) throws IOException {
        ObjectCodec codec = parser.getCodec();
        JsonNode node = codec.readTree(parser);

        List<SortKey> result = new ArrayList<>();

        if (node.isTextual()) {
            String[] parts = StringUtils.split(node.asText().trim(), ',');
            for (String part : parts) {
                result.add(EnumDeserializerUtil.deserialize(SortKey.class, part));
            }
        } else if (node.isArray()) {
            for (JsonNode child : node) {
                if (!child.isTextual()) {
                    throw new IOException("Expected string in sorting array: " + child);
                }
                result.add(EnumDeserializerUtil.deserialize(SortKey.class, child.asText()));
            }
        } else {
            throw new IOException("Unsupported sorting entry: " + node);
        }

        if (result.isEmpty()) {
            throw new IOException("SortKey list cannot be empty");
        }

        return Collections.unmodifiableList(result);
    }
}
