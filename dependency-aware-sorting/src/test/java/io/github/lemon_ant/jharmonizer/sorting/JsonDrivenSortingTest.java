package io.github.lemon_ant.jharmonizer.sorting;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import lombok.Value;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * JSON-driven integration tests for {@link SimplifiedDependencyAwareSorter}.
 *
 * <p>Each sub-directory under {@code src/test/resources/test-cases/} contains two files:
 * <ul>
 *   <li>{@code input.json}    — items, groups, dependencies</li>
 *   <li>{@code expected.json} — expected sorted output (list of item names)</li>
 * </ul>
 *
 * <p>Input format:
 * <pre>{@code
 * {
 *   "description": "Human-readable explanation of the case",
 *   "items":        [{"name": "name1", "numeration": "STATIC"}, {"name": "name2", "numeration": "DYNAMIC"}, ...],
 *   "groups":       [["name1", "name3"], ...],
 *   "dependencies": [{"provider": "name1", "dependent": "name2"}, ...]
 * }
 * }</pre>
 *
 * <p>Expected output format:
 * <pre>{@code
 * ["result1", "result2", ...]
 * }</pre>
 *
 * <p>All cases run against {@link SimplifiedDependencyAwareSorter}. Cases that satisfy the simplified
 * preconditions (no group–dependency overlap) are included.</p>
 *
 * <p>To add a new case just drop a new sub-directory with the two JSON files — no code
 * changes required.
 */
class JsonDrivenSortingTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    // ------------------------------------------------------------------ //
    // Test case loading                                                   //
    // ------------------------------------------------------------------ //

    @Value
    static class TestCase {
        String name;
        List<SortableTypeMember> items;
        Groups<SortableTypeMember> groups;
        Dependencies<SortableTypeMember> dependencies;
        List<String> expected;
        String description;

        @Override
        public String toString() {
            return name;
        }

        /** Returns {@code true} if this case satisfies the simplified algorithm preconditions. */
        boolean isSimplifiedCompatible() {
            Set<String> groupedNames = groups.getGroups().stream()
                    .flatMap(g -> g.getItems().stream())
                    .map(SortableTypeMember::getName)
                    .collect(Collectors.toSet());

            boolean hasOverlap = dependencies.getEdges().stream()
                    .anyMatch(d -> groupedNames.contains(d.getProvider().getName())
                            || groupedNames.contains(d.getDependent().getName()));

            return !hasOverlap;
        }
    }

    static Stream<TestCase> loadSimplifiedCompatibleCases() throws IOException, URISyntaxException {
        return loadAllCases().stream().filter(TestCase::isSimplifiedCompatible);
    }

    private static List<TestCase> loadAllCases() throws IOException, URISyntaxException {
        URL casesUrl = JsonDrivenSortingTest.class.getClassLoader().getResource("test-cases");
        if (casesUrl == null) {
            throw new IllegalStateException("'test-cases' resource directory not found");
        }
        Path casesDir = Paths.get(casesUrl.toURI());

        try (Stream<Path> dirs = Files.list(casesDir)) {
            return dirs.filter(Files::isDirectory)
                    .sorted()
                    .map(dir -> {
                        try {
                            return parseCase(dir);
                        } catch (IOException e) {
                            throw new RuntimeException("Failed to parse case: " + dir, e);
                        }
                    })
                    .toList();
        }
    }

    private static TestCase parseCase(Path dir) throws IOException {
        String caseName = dir.getFileName().toString();

        JsonNode input = MAPPER.readTree(dir.resolve("input.json").toFile());
        JsonNode expected = MAPPER.readTree(dir.resolve("expected.json").toFile());

        // Parse items — preserve declared order/content in the list, and build a
        // separate name→SortableTypeMember index for reference lookup.
        // Each item must be an object: {"name": "...", "numeration": "STATIC|DYNAMIC"}
        List<SortableTypeMember> items = new ArrayList<>();
        Map<String, SortableTypeMember> itemMap = new LinkedHashMap<>();
        StreamSupport.stream(input.get("items").spliterator(), false).forEach(itemNode -> {
            String name = itemNode.get("name").asText();
            SortableTypeMember.Numeration numeration = SortableTypeMember.Numeration.valueOf(
                    itemNode.get("numeration").asText());
            SortableTypeMember member = numeration == SortableTypeMember.Numeration.STATIC
                    ? SortableTypeMember.staticMember(name)
                    : SortableTypeMember.dynamicMember(name);
            items.add(member);
            SortableTypeMember previous = itemMap.putIfAbsent(name, member);
            if (previous != null) {
                throw new IllegalArgumentException(
                        "Duplicate item name '" + name + "' in " + dir.resolve("input.json"));
            }
        });

        // Parse groups
        List<Group<SortableTypeMember>> groups = new ArrayList<>();
        JsonNode groupsNode = input.get("groups");
        if (groupsNode != null && groupsNode.isArray()) {
            StreamSupport.stream(groupsNode.spliterator(), false)
                    .map(groupNode -> new Group<>(StreamSupport.stream(groupNode.spliterator(), false)
                            .map(n -> requireMember(itemMap, n.asText(), dir))
                            .toList()))
                    .forEach(groups::add);
        }

        // Parse dependencies — reference the same SortableTypeMember instances
        List<Dependencies.Dependency<SortableTypeMember>> edges = new ArrayList<>();
        JsonNode depsNode = input.get("dependencies");
        if (depsNode != null && depsNode.isArray()) {
            StreamSupport.stream(depsNode.spliterator(), false)
                    .map(dep -> new Dependencies.Dependency<>(
                            requireMember(itemMap, dep.get("provider").asText(), dir),
                            requireMember(itemMap, dep.get("dependent").asText(), dir)))
                    .forEach(edges::add);
        }

        // Parse expected output
        List<String> expectedNames = StreamSupport.stream(expected.spliterator(), false)
                .map(JsonNode::asText)
                .toList();

        String description = input.has("description") ? input.get("description").asText() : caseName;

        return new TestCase(
                caseName, items, new Groups<>(groups), new Dependencies<>(edges), expectedNames, description);
    }

    private static SortableTypeMember requireMember(Map<String, SortableTypeMember> itemMap, String name, Path dir) {
        SortableTypeMember member = itemMap.get(name);
        if (member == null) {
            throw new IllegalArgumentException("Unknown item name '" + name + "' referenced in "
                    + dir.resolve("input.json") + ". Check that the name is listed in the 'items' array.");
        }
        return member;
    }

    // ------------------------------------------------------------------ //
    // Parameterized tests                                                 //
    // ------------------------------------------------------------------ //

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("loadSimplifiedCompatibleCases")
    @DisplayName("JSON-driven case — SimplifiedDependencyAwareSorter")
    void simplifiedDependencyAwareSorter_jsonDrivenCompatibleCase_returnsExpectedOrder(TestCase tc) {
        // Given
        printCase(tc);

        // When
        List<SortableTypeMember> result = SimplifiedDependencyAwareSorter.sort(
                tc.getItems(), tc.getGroups(), tc.getDependencies(), SortableTypeMember.DEFAULT_ORDER);
        List<String> actual = result.stream().map(SortableTypeMember::getName).toList();

        // Then
        System.out.println("  Actual (simplified): " + actual);
        assertThat(actual).as("Mismatch in simplified case '%s'", tc.getName()).isEqualTo(tc.getExpected());
    }

    private static void printCase(TestCase tc) {
        System.out.printf("%n=== %s ===%n%s%n", tc.getName(), tc.getDescription());
        System.out.println("  Input items : "
                + tc.getItems().stream()
                        .map(m -> m.getName() + ":" + m.getOrderingKey().getNumeration())
                        .toList());
        System.out.println("  Groups      : "
                + tc.getGroups().getGroups().stream()
                        .map(g -> g.getItems().stream()
                                .map(SortableTypeMember::getName)
                                .toList())
                        .toList());
        System.out.println("  Dependencies: "
                + tc.getDependencies().getEdges().stream()
                        .map(d -> d.getProvider().getName() + "->"
                                + d.getDependent().getName())
                        .toList());
        System.out.println("  Expected    : " + tc.getExpected());
    }
}
