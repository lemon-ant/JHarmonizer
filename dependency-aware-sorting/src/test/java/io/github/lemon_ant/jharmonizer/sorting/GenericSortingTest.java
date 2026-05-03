/*
 * SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.lemon_ant.jharmonizer.sorting;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

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
// TODO Split it
class GenericSortingTest {

    private static final Comparator<String> NATURAL_ORDER = Comparator.naturalOrder();

    @Nested
    class GeneralStringTests {

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
            // When
            Throwable thrown = catchThrowable(() -> SimplifiedDependencyAwareSorter.sort(
                    List.of("a", "b", "c"),
                    Groups.empty(),
                    Dependencies.of("a", "b", "b", "c", "c", "a"),
                    NATURAL_ORDER));

            // Then
            assertThat(thrown).isInstanceOf(SortingException.class).message().containsIgnoringCase("cycle");
        }

        @Test
        void sort_selfDependency_throwsSortingException() {
            // When
            Throwable thrown = catchThrowable(() -> SimplifiedDependencyAwareSorter.sort(
                    List.of("a", "b"), Groups.empty(), Dependencies.of("a", "a"), NATURAL_ORDER));

            // Then
            assertThat(thrown).isInstanceOf(SortingException.class).message().containsIgnoringCase("self");
        }

        @Test
        void sort_duplicateItem_throwsSortingException() {
            // When
            Throwable thrown = catchThrowable(() -> SimplifiedDependencyAwareSorter.sort(
                    List.of("apple", "banana", "apple"), Groups.empty(), Dependencies.empty(), NATURAL_ORDER));

            // Then
            assertThat(thrown).isInstanceOf(SortingException.class).hasMessageContaining("apple");
        }

        @Test
        void sort_memberInTwoGroups_throwsSortingException() {
            // When
            Throwable thrown = catchThrowable(() -> SimplifiedDependencyAwareSorter.sort(
                    List.of("a", "b", "c"),
                    new Groups<>(List.of(Group.of("a", "b"), Group.of("b", "c"))),
                    Dependencies.empty(),
                    NATURAL_ORDER));

            // Then
            assertThat(thrown).isInstanceOf(SortingException.class).hasMessageContaining("b");
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
        void sort_validInput_resultIsUnmodifiable() {
            // When
            List<String> result = SimplifiedDependencyAwareSorter.sort(
                    List.of("b", "a"), Groups.empty(), Dependencies.empty(), NATURAL_ORDER);
            Throwable thrown = catchThrowable(() -> result.add("x"));

            // Then
            assertThat(thrown).isInstanceOf(UnsupportedOperationException.class);
        }

        @Test
        void sort_shuffledInput_producesDeterministicResult() {
            // Given
            var base = List.of("fig", "cherry", "apple", "elderberry", "banana", "date");
            var groups = new Groups<>(List.of(Group.of("fig", "date")));
            var dependencies = Dependencies.of("cherry", "apple");
            var expected = SimplifiedDependencyAwareSorter.sort(base, groups, dependencies, NATURAL_ORDER);
            var rng = new Random(42);

            for (int run = 0; run < 20; run++) {
                // When
                var shuffled = new ArrayList<>(base);
                Collections.shuffle(shuffled, rng);

                // Then
                assertThat(SimplifiedDependencyAwareSorter.sort(shuffled, groups, dependencies, NATURAL_ORDER))
                        .as("run %d: result must be deterministic", run)
                        .isEqualTo(expected);
            }
        }
    }

    @Nested
    class SimplifiedSorterStringTests {

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
        void sort_dependencyOnSingletons_providerBeforeDependent() {
            // Given
            var items = List.of("alpha", "bravo", "charlie");

            // When
            List<String> result = SimplifiedDependencyAwareSorter.sort(
                    items, Groups.empty(), Dependencies.of("charlie", "alpha"), NATURAL_ORDER);

            // Then
            assertThat(result.indexOf("charlie")).isLessThan(result.indexOf("alpha"));
        }

        @Test
        void sort_groupsAndDepsOnDisjointMembers_bothConstraintsHonoured() {
            // Given
            var items = List.of("delta", "gamma", "beta", "alpha");
            var groups = new Groups<>(List.of(Group.of("alpha", "beta")));

            // When
            List<String> result = SimplifiedDependencyAwareSorter.sort(
                    items, groups, Dependencies.of("gamma", "delta"), NATURAL_ORDER);

            // Then
            assertThat(result.indexOf("alpha")).isLessThan(result.indexOf("beta"));
            assertThat(result.indexOf("gamma")).isLessThan(result.indexOf("delta"));
        }

        @Test
        void sort_groupMemberInDependency_throwsSortingException() {
            // When
            Throwable thrown = catchThrowable(() -> SimplifiedDependencyAwareSorter.sort(
                    List.of("alpha", "beta", "gamma"),
                    new Groups<>(List.of(Group.of("alpha", "beta"))),
                    Dependencies.of("alpha", "gamma"),
                    NATURAL_ORDER));

            // Then
            assertThat(thrown).isInstanceOf(SortingException.class).hasMessageContaining("alpha");
        }

        @Test
        void sort_validInput_resultIsUnmodifiable() {
            // When
            List<String> result = SimplifiedDependencyAwareSorter.sort(
                    List.of("b", "a"), Groups.empty(), Dependencies.empty(), NATURAL_ORDER);
            Throwable thrown = catchThrowable(() -> result.add("x"));

            // Then
            assertThat(thrown).isInstanceOf(UnsupportedOperationException.class);
        }
    }

    @Nested
    class IntegerItemTests {

        private static final Comparator<Integer> INT_ORDER = Comparator.naturalOrder();

        @Test
        void sort_integerItems_returnsNaturalOrder() {
            // When
            List<Integer> result = SimplifiedDependencyAwareSorter.sort(
                    List.of(3, 1, 4, 1_000, 5, 9), Groups.empty(), Dependencies.empty(), INT_ORDER);

            // Then
            assertThat(result).containsExactly(1, 3, 4, 5, 9, 1_000);
        }

        @Test
        void sort_integerGroup_membersKeptTogether() {
            // Given
            var items = List.of(50, 30, 10, 20, 40);
            var groups = new Groups<>(List.of(Group.of(50, 30)));

            // When
            List<Integer> result = SimplifiedDependencyAwareSorter.sort(items, groups, Dependencies.empty(), INT_ORDER);

            // Then
            assertThat(result.indexOf(30)).isLessThan(result.indexOf(50));
            assertThat(Math.abs(result.indexOf(30) - result.indexOf(50))).isEqualTo(1);
        }

        @Test
        void sort_integerDependency_providerBeforeDependent() {
            // When
            List<Integer> result = SimplifiedDependencyAwareSorter.sort(
                    List.of(1, 2, 3), Groups.empty(), Dependencies.of(3, 1), INT_ORDER);

            // Then
            assertThat(result.indexOf(3)).isLessThan(result.indexOf(1));
        }

        @Test
        void sort_fiveHundredIntegerItems_allDepsRespected() {
            // Given
            int total = 500;
            var items = IntStream.range(0, total).boxed().collect(Collectors.toList());
            var depList = IntStream.range(401, 500)
                    .mapToObj(i -> new Dependencies.Dependency<>(i, i - 1))
                    .toList();

            // When
            List<Integer> result =
                    SimplifiedDependencyAwareSorter.sort(items, Groups.empty(), new Dependencies<>(depList), INT_ORDER);

            // Then
            assertThat(result).hasSize(total);
            depList.forEach(dep -> assertThat(result.indexOf(dep.getProvider()))
                    .as("%d must precede %d", dep.getProvider(), dep.getDependent())
                    .isLessThan(result.indexOf(dep.getDependent())));
        }
    }

    @Nested
    class CustomRecordTests {

        @Value
        static class Task {
            String id;
            int priority;
        }

        private static final Comparator<Task> BY_PRIORITY_THEN_ID =
                Comparator.comparingInt(Task::getPriority).thenComparing(Task::getId);

        @Test
        void sort_customTypeWithDependency_depConstraintRespected() {
            // Given
            var deploy = new Task("deploy", 3);
            var build = new Task("build", 1);
            var test = new Task("test", 2);
            var lint = new Task("lint", 1);

            // When
            List<Task> result = SimplifiedDependencyAwareSorter.sort(
                    List.of(deploy, build, test, lint),
                    Groups.empty(),
                    Dependencies.of(build, test, test, deploy),
                    BY_PRIORITY_THEN_ID);

            // Then
            assertThat(result.indexOf(build)).isLessThan(result.indexOf(test));
            assertThat(result.indexOf(test)).isLessThan(result.indexOf(deploy));
        }

        @Test
        void sort_customTypeWithGroupAndDep_bothConstraintsHonoured() {
            // Given
            var deploy = new Task("deploy", 3);
            var build = new Task("build", 1);
            var test = new Task("test", 2);
            var lint = new Task("lint", 1);
            var groups = new Groups<>(List.of(Group.of(build, lint)));

            // When
            List<Task> result = SimplifiedDependencyAwareSorter.sort(
                    List.of(deploy, build, test, lint), groups, Dependencies.of(test, deploy), BY_PRIORITY_THEN_ID);

            // Then
            assertThat(result.indexOf(build)).isLessThan(result.indexOf(lint));
            assertThat(Math.abs(result.indexOf(build) - result.indexOf(lint))).isEqualTo(1);
            assertThat(result.indexOf(test)).isLessThan(result.indexOf(deploy));
        }
    }
}
