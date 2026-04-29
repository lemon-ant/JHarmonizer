package io.github.lemon_ant.jharmonizer.core.config.input.jharmonizer.model;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;
import lombok.NonNull;

/**
 * Jackson deserializer that converts a YAML scalar or sequence into a
 * {@link JHarmonizerTopLevelTypeSelector} containing one or more {@link JHarmonizerTypeKind} values.
 */
class TypeGroupDeserializer extends JsonDeserializer<JHarmonizerTopLevelTypeSelector> {

    /**
     * Performs the deserialize.
     * @param p the parser to read from
     * @param ctxt the deserialization context
     * @return the result
     */
    @Override
    @NonNull
    public JHarmonizerTopLevelTypeSelector deserialize(@NonNull JsonParser p, @NonNull DeserializationContext ctxt)
            throws IOException {
        ObjectCodec codec = p.getCodec();
        JsonNode node = codec.readTree(p);

        Set<JHarmonizerTypeKind> result = new TreeSet<>();

        if (node.isTextual()) {
            result.add(EnumDeserializerUtil.deserialize(JHarmonizerTypeKind.class, node.asText()));
        } else if (node.isArray()) {
            for (JsonNode child : node) {
                if (!child.isTextual()) {
                    throw new IOException("Expected string in array");
                }
                result.add(EnumDeserializerUtil.deserialize(JHarmonizerTypeKind.class, child.asText()));
            }
        } else {
            throw new IOException("Unsupported type-order entry: " + node);
        }

        return new JHarmonizerTopLevelTypeSelector(Collections.unmodifiableSet(result));
    }
}
