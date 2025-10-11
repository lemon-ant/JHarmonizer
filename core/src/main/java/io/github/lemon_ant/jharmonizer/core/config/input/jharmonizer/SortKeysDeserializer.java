package io.github.lemon_ant.jharmonizer.core.config.input.jharmonizer;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.apache.commons.lang3.StringUtils;

class SortKeysDeserializer extends JsonDeserializer<List<SortKey>> {

    @Override
    @SuppressWarnings("PMD.CyclomaticComplexity")
    public List<SortKey> deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        ObjectCodec codec = p.getCodec();
        JsonNode node = codec.readTree(p);

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
            throw new IOException("UnifiedSortKey list cannot be empty");
        }

        return Collections.unmodifiableList(result);
    }
}
