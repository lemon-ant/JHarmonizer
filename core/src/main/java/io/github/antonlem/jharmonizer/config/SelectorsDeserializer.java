package io.github.antonlem.jharmonizer.config;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;

class SelectorsDeserializer extends JsonDeserializer<Set<Set<String>>> {

    @Override
    public Set<Set<String>> deserialize(JsonParser jsonParser, DeserializationContext deserializationContext)
            throws IOException, JacksonException {
        JsonNode node = jsonParser.readValueAsTree();
        Set<Set<String>> result = new HashSet<>();

        if (node.isTextual()) {
            result.add(parseCommaSeparated(node.textValue()));
        } else if (node.isArray()) {
            for (JsonNode item : node) {
                if (item.isTextual()) {
                    result.add(parseCommaSeparated(item.textValue()));
                } else if (item.isArray()) {
                    @SuppressWarnings("PMD.AvoidInstantiatingObjectsInLoops")
                    Set<String> group = new TreeSet<>();
                    item.forEach(subItem -> group.add(subItem.asText()));
                    result.add(group);
                } else {
                    throw new IllegalArgumentException("Unsupported item type in match array: " + item);
                }
            }
        } else {
            throw new IllegalArgumentException("Unsupported match format: " + node);
        }

        return result;
    }

    private Set<String> parseCommaSeparated(String text) {
        return Collections.unmodifiableSet(Arrays.stream(StringUtils.split(text, ','))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.<String, Set<String>>toCollection(HashSet::new)));
    }
}
