package io.github.lemon_ant.jharmonizer.sorting;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import lombok.Value;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests the generic sorting API using plain {@link String} and {@link Integer} items
 * (i.e. types other than {@link SortableTypeMember}) to verify true generalization.
 *
 * <p>{@link SimplifiedDependencyAwareSorter} is exercised.</p>
 */
class GenericSortingTest {

    private static final Comparator<String> NATURAL_ORDER = Comparator.naturalOrder();

    @Nested
    class SimplifiedDependencyAwareSorterStringTests {

        @Test
        void sort_emptyInput_returnsEmptyList() {
            // When
            List<String> result = SimplifiedDependencyAwareSorter.sort(
                    List.of(), Groups.empty(), Dependencies.empty(), NATURAL_ORDER);

            // Then
            assertThat(result).isEmpty();
        }

        @Test
        void sort_noGroupsOrDependencies_returnsNaturalOrder() {
            // When
            List<String> result = SimplifiedDependencyAwareSorter.sort(
                    List.of("cherry", "apple", "banana"), Groups.empty(), Dependencies.empty(), NATURAL_ORDER);

            // Then
            assertThat(result).containsExactly("apple", "banana", "cherry");
        }

        @Test
        void sort_singleItem_returnsSameItem() {
            // When
            List<String> result = SimplifiedDependencyAwareSorter.sort(
                    List.of("solo"), Groups.empty(), Dependencies.empty(), NATURAL_ORDER);

            // Then
            assertThat(result).containsExactly("solo");
        }

        @Test
        void sort_oneGroup_itemsKeptTogether() {
            // Given
            var items = List.of("zebra", "bravo", "alpha", "mango");
            var groups = new Groups<>(List.of(Group.of("zebra", "bravo")));

            // When
            List<String> result =
                    SimplifiedDependencyAwareSorter.sort(items, groups, Dependencies.empty(), NATURAL_ORDER);

            // Then
            assertThat(result).containsExactly("alpha", "bravo", "zebra", "mango");
        }

        @Test
        void sort_dependencyReversesNaturalOrder_providerBeforeDependent() {
            // Given
            var items = List.of("alpha", "bravo", "charlie");

            // When
            List<String> result = SimplifiedDependencyAwareSorter.sort(
                    items, Groups.empty(), Dependencies.of("charlie", "alpha"), NATURAL_ORDER);

            // Then
            assertThat(result.indexOf("charlie")).isLessThan(result.indexOf("alpha"));
        }

        @Test
        void sort_transitiveChain_respectsFullOrdering() {
            // When
            List<String> result = SimplifiedDependencyAwareSorter.sort(
                    List.of("a", "b", "c", "d"),
                    Groups.empty(),
                    Dependencies.of("d", "c", "c", "b", "b", "a"),
                    NATURAL_ORDER);

            // Then
            assertThat(result).containsExactly("d", "c", "b", "a");
        }

        @Test
        void sort_cyclicDependency_throwsSortingException() {
            // When / Then
            assertThatThrownBy(() -> SimplifiedDependencyAwareSorter.sort(
                            List.of("a", "b", "c"),
                            Groups.empty(),
                            Dependencies.of("a", "b", "b", "c", "c", "a"),
                            NATURAL_ORDER))
                    .isInstanceOf(SortingException.class)
                    .message()
                    .containsIgnoringCase("cycle");
        }

        @Test
        void sort_selfDependency_throwsSortingException() {
            // When / Then
            assertThatThrownBy(() -> SimplifiedDependencyAwareSorter.sort(
                            List.of("a", "b"), Groups.empty(), Dependencies.of("a", "a"), NATURAL_ORDER))
                    .isInstanceOf(SortingException.class)
                    .message()
                    .containsIgnoringCase("self");
        }

        @Test
        void sort_duplicateItem_throwsSortingException() {
            // When / Then
            assertThatThrownBy(() -> SimplifiedDependencyAwareSorter.sort(
                            List.of("apple", "banana", "apple"), Groups.empty(), Dependencies.empty(), NATURAL_ORDER))
                    .isInstanceOf(SortingException.class)
                    .hasMessageContaining("apple");
        }

        @Test
        void sort_memberInTwoGroups_throwsSortingException() {
            // When / Then
            assertThatThrownBy(() -> SimplifiedDependencyAwareSorter.sort(
                            List.of("a", "b", "c"),
                            new Groups<>(List.of(Group.of("a", "b"), Group.of("b", "c"))),
                            Dependencies.empty(),
                            NATURAL_ORDER))
                    .isInstanceOf(SortingException.class)
                    .hasMessageContaining("b");
        }

        @Test
        void sort_reversedComparator_returnsReverseOrder() {
            // When
            List<String> result = SimplifiedDependencyAwareSorter.sort(
                    List.of("alpha", "beta", "charlie"),
                    Groups.empty(),
                    Dependencies.empty(),
                    Comparator.<String>naturalOrder().reversed());

            // Then
            assertThat(result).containsExactly("charlie", "beta", "alpha");
        }

        @Test
        void sort_resultIsUnmodifiable_throwsOnMutation() {
            // When
            List<String> result = SimplifiedDependencyAwareSorter.sort(
                    List.of("b", "a"), Groups.empty(), Dependencies.empty(), NATURAL_ORDER);

            // Then
            assertThatThrownBy(() -> result.add("x")).isInstanceOf(UnsupportedOperationException.class);
        }

        @Test
        void sort_deterministic_shuffledInputProducesSameResult() {
            // Given
            var base = List.of("fig", "cherry", "apple", "elderberry", "banana", "date");
            var g = new Groups<>(List.of(Group.of("fig", "date")));
            var d = Dependencies.of("cherry", "apple");
            var expected = SimplifiedDependencyAwareSorter.sort(base, g, d, NATURAL_ORDER);
            var rng = new Random(42);

            for (int run = 0; run < 20; run++) {
                // When
                var shuffled = new ArrayList<>(base);
                Collections.shuffle(shuffled, rng);

                // Then
                assertThat(SimplifiedDependencyAwareSorter.sort(shuffled, g, d, NATURAL_ORDER))
                        .as("run %d", run)
                        .isEqualTo(expected);
            }
        }
    }

    @Nested
    class SimplifiedDependencyAwareSorterIntegerTests {

        @Test
        void sort_integerItems_sortedNaturally() {
            // When
            List<Integer> result = SimplifiedDependencyAwareSorter.sort(
                    List.of(5, 3, 1, 4, 2), Groups.empty(), Dependencies.empty(), Comparator.naturalOrder());

            // Then
            assertThat(result).containsExactly(1, 2, 3, 4, 5);
        }

        @Test
        void sort_integerGroups_groupKeptTogether() {
            // Given
            var items = List.of(5, 3, 1, 4, 2);
            var groups = new Groups<>(List.of(new Group<>(List.of(5, 3))));

            // When
            List<Integer> result = SimplifiedDependencyAwareSorter.sort(
                    items, groups, Dependencies.empty(), Comparator.naturalOrder());

            // Then
            assertThat(result).containsExactly(1, 2, 3, 5, 4);
        }

        @Test
        void sort_integerDependencyReversesOrder_providerFirst() {
            // When
            List<Integer> result = SimplifiedDependencyAwareSorter.sort(
                    List.of(1, 2, 3), Groups.empty(), Dependencies.of(3, 1), Comparator.naturalOrder());

            // Then
            assertThat(result.indexOf(3)).isLessThan(result.indexOf(1));
        }
    }

    @Nested
    class SimplifiedDependencyAwareSorterCustomTypeTests {

        @Value
        static class Task {
            String id;
            int priority;
        }

        private static final Comparator<Task> BY_PRIORITY_THEN_ID =
                Comparator.comparingInt(Task::getPriority).thenComparing(Task::getId);

        @Test
        void sort_customObjects_sortedByComparator() {
            // Given
            var tasks = List.of(new Task("deploy", 3), new Task("build", 1), new Task("test", 2));

            // When
            List<Task> result = SimplifiedDependencyAwareSorter.sort(
                    tasks, Groups.empty(), Dependencies.empty(), BY_PRIORITY_THEN_ID);

            // Then
            assertThat(result).extracting(Task::getId).containsExactly("build", "test", "deploy");
        }

        @Test
        void sort_customObjectsWithDeps_dependencyOverridesComparator() {
            // Given
            var deploy = new Task("deploy", 3);
            var build = new Task("build", 1);
            var test = new Task("test", 2);
            var items = List.of(deploy, build, test);
            var deps = new Dependencies<>(List.of(new Dependencies.Dependency<>(deploy, build)));

            // When
            List<Task> result = SimplifiedDependencyAwareSorter.sort(items, Groups.empty(), deps, BY_PRIORITY_THEN_ID);

            // Then
            assertThat(result.indexOf(deploy)).isLessThan(result.indexOf(build));
        }

        @Test
        void sort_largeInput_completesSuccessfully() {
            // Given
            int total = 1000;
            var tasks = IntStream.range(0, total)
                    .mapToObj(i -> new Task(String.format("task%04d", i), i % 5))
                    .collect(Collectors.toList());
            var depEdges = IntStream.range(1, 100)
                    .mapToObj(i -> new Dependencies.Dependency<>(tasks.get(i + 100), tasks.get(i)))
                    .toList();
            var deps = new Dependencies<>(depEdges);

            // When
            List<Task> result = SimplifiedDependencyAwareSorter.sort(tasks, Groups.empty(), deps, BY_PRIORITY_THEN_ID);

            // Then
            assertThat(result).hasSize(total);
            depEdges.forEach(dep -> assertThat(result.indexOf(dep.getProvider()))
                    .as(
                            "%s must precede %s",
                            dep.getProvider().getId(), dep.getDependent().getId())
                    .isLessThan(result.indexOf(dep.getDependent())));
        }
    }
}
