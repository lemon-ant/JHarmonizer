// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
package io.github.lemon_ant.jharmonizer.core.config.input.jharmonizer.model;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.NonNull;
import org.apache.commons.lang3.StringUtils;

/**
 * Deserializes selector DSL values into {@code Set<Set<String>>}, where:
 *
 * <ul>
 *   <li>outer set represents OR alternatives,</li>
 *   <li>each inner set represents an AND group (all tokens in that group must match).</li>
 * </ul>
 *
 * <p>Supported forms:
 *
 * <ul>
 *   <li>scalar token: {@code include: a} -> {@code {{"a"}}}</li>
 *   <li>scalar comma list: {@code include: a, b} -> {@code {{"a", "b"}}}</li>
 *   <li>flow array in one line: {@code include: [a, b]} -> {@code {{"a", "b"}}}</li>
 *   <li>block array with one item per line: <pre>{@code include:
 * - a
 * - b}</pre> -> {@code {{"a"}, {"b"}}}</li>
 * </ul>
 */
class SelectorsDeserializer extends JsonDeserializer<Set<Set<String>>> {

    /**
     * Performs the deserialize.
     * @param jsonParser the parser to read from
     * @param deserializationContext the deserialization context
     * @return the resulting set
     */
    @Override
    @NonNull
    public Set<Set<String>> deserialize(
            @NonNull JsonParser jsonParser, @NonNull DeserializationContext deserializationContext) throws IOException {
        JsonToken currentToken = jsonParser.currentToken();
        if (currentToken == null) {
            currentToken = jsonParser.nextToken();
        }

        if (currentToken == JsonToken.VALUE_STRING) {
            return Set.of(parseCommaSeparated(jsonParser.getText()));
        }

        if (currentToken == JsonToken.START_ARRAY) {
            return parseTopLevelArray(jsonParser);
        }

        throw new IllegalArgumentException("Unsupported selector value type: " + currentToken);
    }

    @NonNull
    private Set<Set<String>> parseTopLevelArray(JsonParser jsonParser) throws IOException {
        Set<Set<String>> alternatives = new HashSet<>();
        Set<String> singleLineFlowGroup = new HashSet<>();

        boolean allItemsTextual = true;
        boolean allTextItemsOnSingleLine = true;
        int firstTextLine = -1;

        while (jsonParser.nextToken() != JsonToken.END_ARRAY) {
            JsonToken itemToken = jsonParser.currentToken();

            if (itemToken == JsonToken.VALUE_STRING) {
                int currentLine = jsonParser.currentLocation().getLineNr();
                if (firstTextLine < 0) {
                    firstTextLine = currentLine;
                } else if (currentLine != firstTextLine) {
                    allTextItemsOnSingleLine = false;
                }

                Set<String> parsedGroup = parseCommaSeparated(jsonParser.getText());
                alternatives.add(parsedGroup);
                singleLineFlowGroup.addAll(parsedGroup);
                continue;
            }

            allItemsTextual = false;
            if (itemToken == JsonToken.START_ARRAY) {
                alternatives.add(parseNestedArrayItem(jsonParser));
                continue;
            }

            throw new IllegalArgumentException(
                    "Unsupported selector array item type: " + itemToken + ". Supported: string or array.");
        }

        if (allItemsTextual && allTextItemsOnSingleLine) {
            return Set.of(Collections.unmodifiableSet(singleLineFlowGroup));
        }

        return Collections.unmodifiableSet(alternatives);
    }

    @NonNull
    private Set<String> parseNestedArrayItem(JsonParser jsonParser) throws IOException {
        Set<String> group = new HashSet<>();
        while (jsonParser.nextToken() != JsonToken.END_ARRAY) {
            JsonToken nestedToken = jsonParser.currentToken();
            if (nestedToken != JsonToken.VALUE_STRING) {
                throw new IllegalArgumentException("Unsupported nested selector item type: " + nestedToken
                        + ". Nested arrays must contain only strings.");
            }
            group.addAll(parseCommaSeparated(jsonParser.getText()));
        }

        return Collections.unmodifiableSet(group);
    }

    @NonNull
    private Set<String> parseCommaSeparated(String text) {
        return Arrays.stream(StringUtils.split(text, ','))
                .map(String::trim)
                .filter(token -> !token.isEmpty())
                .collect(Collectors.toUnmodifiableSet());
    }
}
