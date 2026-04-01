package io.github.lemon_ant.jharmonizer.core.config.input.jharmonizer.model;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import lombok.NonNull;
import org.apache.commons.lang3.StringUtils;

/**
 * Jackson deserializer for the {@code ordering-rules} field in JHarmonizer YAML configs.
 * Accepts either a comma-separated string or a YAML sequence of strings.
 */
class OrderingRulesDeserializer extends JsonDeserializer<List<JHarmonizerOrderingRule>> {

    /**
     * Performs the deserialize.
     * @param p the parser to read from
     * @param ctxt the deserialization context
     * @return the resulting list
     */
    @Override
    @NonNull
    public List<JHarmonizerOrderingRule> deserialize(@NonNull JsonParser p, @NonNull DeserializationContext ctxt)
            throws IOException {
        ObjectCodec codec = p.getCodec();
        JsonNode node = codec.readTree(p);

        List<JHarmonizerOrderingRule> result = new ArrayList<>();

        if (node.isTextual()) {
            String[] parts = StringUtils.split(node.asText().trim(), ',');
            for (String part : parts) {
                result.add(EnumDeserializerUtil.deserialize(JHarmonizerOrderingRule.class, part));
            }
        } else if (node.isArray()) {
            for (JsonNode child : node) {
                if (!child.isTextual()) {
                    throw new IOException("Expected string in sorting array: " + child);
                }
                result.add(EnumDeserializerUtil.deserialize(JHarmonizerOrderingRule.class, child.asText()));
            }
        } else {
            throw new IOException("Unsupported sorting entry: " + node);
        }

        return Collections.unmodifiableList(result);
    }
}
