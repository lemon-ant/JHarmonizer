// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
package io.github.lemon_ant.jharmonizer.core.config.input.jharmonizer.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import java.io.IOException;
import java.util.Set;
import lombok.NonNull;
import org.junit.jupiter.api.Test;

class SelectorsDeserializerTest {

    private final ObjectMapper mapper = new ObjectMapper(new YAMLFactory());

    @Test
    void deserialize_scalarSingleToken_returnsSingleAndGroup() throws IOException {
        assertThat(parseIncludes("include: a")).isEqualTo(Set.of(Set.of("a")));
    }

    @Test
    void deserialize_scalarCommaSeparated_returnsSingleAndGroup() throws IOException {
        assertThat(parseIncludes("include: a, b")).isEqualTo(Set.of(Set.of("a", "b")));
    }

    @Test
    void deserialize_flowSequenceInOneLine_returnsSingleAndGroup() throws IOException {
        assertThat(parseIncludes("include: [a, b]")).isEqualTo(Set.of(Set.of("a", "b")));
    }

    @Test
    void deserialize_blockSequenceWithSingleTokens_returnsMultipleGroups() throws IOException {
        assertThat(parseIncludes("include:\n  - a\n  - b\n")).isEqualTo(Set.of(Set.of("a"), Set.of("b")));
    }

    @Test
    void deserialize_blockSequenceWithCommaSeparatedItems_returnsExpectedGroups() throws IOException {
        assertThat(parseIncludes("include:\n  - a, b\n  - c\n")).isEqualTo(Set.of(Set.of("a", "b"), Set.of("c")));
    }

    @Test
    void deserialize_blockSequenceWithNestedArrayItems_returnsExpectedGroups() throws IOException {
        assertThat(parseIncludes("include:\n  - [a, b]\n  - c\n")).isEqualTo(Set.of(Set.of("a", "b"), Set.of("c")));
    }

    @Test
    void deserialize_returnsUnmodifiableOuterAndInnerSets() throws IOException {
        Set<Set<String>> result = parseIncludes("include: [a, b]");

        assertThatThrownBy(() -> result.add(Set.of("c"))).isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> result.iterator().next().add("c")).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void deserialize_unsupportedTopLevelNodeType_throwsClearException() {
        assertThatThrownBy(() -> parseIncludes("include:\n  key: value\n"))
                .isInstanceOf(JsonMappingException.class)
                .hasRootCauseInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unsupported selector value type");
    }

    @Test
    void deserialize_unsupportedArrayItemType_throwsClearException() {
        assertThatThrownBy(() -> parseIncludes("include:\n  - key: value\n"))
                .isInstanceOf(JsonMappingException.class)
                .hasRootCauseInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unsupported selector array item type");
    }

    @Test
    void deserialize_unsupportedNestedArrayItemType_throwsClearException() {
        assertThatThrownBy(() -> parseIncludes("include:\n  - [a, { key: value }]\n"))
                .isInstanceOf(JsonMappingException.class)
                .hasRootCauseInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unsupported nested selector item type");
    }

    @NonNull
    private Set<Set<String>> parseIncludes(String yaml) throws IOException {
        return mapper.readValue(yaml, IncludesHolder.class).include();
    }

    private record IncludesHolder(
            @JsonDeserialize(using = SelectorsDeserializer.class)
            Set<Set<String>> include) {}
}
