package io.github.lemon_ant.jharmonizer.sorting;

import lombok.Value;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests the generic sorting API using plain {@link String} and {@link Integer} items
 * (i.e. types other than {@link SortableTypeMember}) to verify true generalization.
 *
 * <p>Both {@link DependencyAwareSorter} and {@link SimplifiedDependencyAwareSorter} are exercised.</p>
 */
class GenericSortingTest {

    // ------------------------------------------------------------------ //
    // Helper constants for String-based tests                             //
    // ------------------------------------------------------------------ //

    private static final Comparator<String> NATURAL_ORDER = Comparator.naturalOrder();

    // ------------------------------------------------------------------ //
    // Tests using String items — DependencyAwareSorter                    //
    // ------------------------------------------------------------------ //

    @Nested
    class DependencyAwareSorterStringTests {

        @Test
        void emptyInput() {
            List<String> result = DependencyAwareSorter.sort(
                    List.of(), Groups.empty(), Dependencies.empty(),
                    NATURAL_ORDER);
            assertThat(result).isEmpty();
        }

        @Test
        void plainAlphabeticalSort() {
            List<String> result = DependencyAwareSorter.sort(
                    List.of("cherry", "apple", "banana"),
                    Groups.empty(), Dependencies.empty(),
                    NATURAL_ORDER);
            assertThat(result).containsExactly("apple", "banana", "cherry");
        }

        @Test
        void singleItem() {
            List<String> result = DependencyAwareSorter.sort(
                    List.of("solo"), Groups.empty(), Dependencies.empty(),
                    NATURAL_ORDER);
            assertThat(result).containsExactly("solo");
        }

        @Test
        void groupKeepsItemsTogether() {
            List<String> result = DependencyAwareSorter.sort(
                    List.of("zebra", "bravo", "alpha", "mango"),
                    new Groups<>(List.of(Group.of("zebra", "bravo"))),
                    Dependencies.empty(),
                    NATURAL_ORDER);
            // Group {bravo, zebra} together, alpha before group key (bravo), mango after
            assertThat(result).containsExactly("alpha", "bravo", "zebra", "mango");
        }

        @Test
        void dependencyForcesOrder() {
            List<String> result = DependencyAwareSorter.sort(
                    List.of("alpha", "bravo", "charlie"),
                    Groups.empty(),
                    Dependencies.of("charlie", "alpha"),
                    NATURAL_ORDER);
            assertThat(result.indexOf("charlie")).isLessThan(result.indexOf("alpha"));
        }

        @Test
        void transitiveDependencies() {
            List<String> result = DependencyAwareSorter.sort(
                    List.of("a", "b", "c", "d"),
                    Groups.empty(),
                    Dependencies.of("d", "c", "c", "b", "b", "a"),
                    NATURAL_ORDER);
            assertThat(result).containsExactly("d", "c", "b", "a");
        }

        @Test
        void cycleThrows() {
            assertThatThrownBy(() -> DependencyAwareSorter.sort(
                    List.of("a", "b", "c"),
                    Groups.empty(),
                    Dependencies.of("a", "b", "b", "c", "c", "a"),
                    NATURAL_ORDER))
                    .isInstanceOf(SortingException.class)
                    .message().containsIgnoringCase("cycle");
        }

        @Test
        void selfDependencyThrows() {
            assertThatThrownBy(() -> DependencyAwareSorter.sort(
                    List.of("a", "b"),
                    Groups.empty(),
                    Dependencies.of("a", "a"),
                    NATURAL_ORDER))
                    .isInstanceOf(SortingException.class)
                    .message().containsIgnoringCase("self");
        }

        @Test
        void duplicateIdentityThrows() {
            assertThatThrownBy(() -> DependencyAwareSorter.sort(
                    List.of("apple", "banana", "apple"),
                    Groups.empty(), Dependencies.empty(),
                    NATURAL_ORDER))
                    .isInstanceOf(SortingException.class)
                    .hasMessageContaining("apple");
        }

        @Test
        void memberInTwoGroupsThrows() {
            assertThatThrownBy(() -> DependencyAwareSorter.sort(
                    List.of("a", "b", "c"),
                    new Groups<>(List.of(Group.of("a", "b"), Group.of("b", "c"))),
                    Dependencies.empty(),
                    NATURAL_ORDER))
                    .isInstanceOf(SortingException.class)
                    .hasMessageContaining("b");
        }

        @Test
        void customComparatorReversesOrder() {
            List<String> result = DependencyAwareSorter.sort(
                    List.of("alpha", "beta", "charlie"),
                    Groups.empty(), Dependencies.empty(),
                    Comparator.<String>naturalOrder().reversed());
            assertThat(result).containsExactly("charlie", "beta", "alpha");
        }

        @Test
        void groupWithDependency() {
            List<String> result = DependencyAwareSorter.sort(
                    List.of("delta", "echo", "alpha", "beta"),
                    new Groups<>(List.of(
                            Group.of("delta", "echo"),
                            Group.of("alpha", "beta"))),
                    Dependencies.of("delta", "beta"),
                    NATURAL_ORDER);
            // Group {delta, echo} must come before group {alpha, beta}
            assertThat(result.indexOf("delta")).isLessThan(result.indexOf("alpha"));
            assertThat(result.indexOf("echo")).isLessThan(result.indexOf("beta"));
        }

        @Test
        void resultIsUnmodifiable() {
            List<String> result = DependencyAwareSorter.sort(
                    List.of("b", "a"), Groups.empty(), Dependencies.empty(),
                    NATURAL_ORDER);
            assertThatThrownBy(() -> result.add("x"))
                    .isInstanceOf(UnsupportedOperationException.class);
        }

        @Test
        void determinismShuffledInput() {
            List<String> base = List.of("fig", "cherry", "apple", "elderberry", "banana", "date");
            Groups<String> g = new Groups<>(List.of(Group.of("fig", "date")));
            Dependencies<String> d = Dependencies.of("cherry", "apple");

            List<String> expected = DependencyAwareSorter.sort(
                    base, g, d, NATURAL_ORDER);

            Random rng = new Random(42);
            for (int run = 0; run < 20; run++) {
                List<String> shuffled = new ArrayList<>(base);
                Collections.shuffle(shuffled, rng);
                assertThat(DependencyAwareSorter.sort(shuffled, g, d, NATURAL_ORDER))
                        .as("run %d: result must be deterministic", run)
                        .isEqualTo(expected);
            }
        }
    }

    // ------------------------------------------------------------------ //
    // Tests using String items — SimplifiedDependencyAwareSorter          //
    // ------------------------------------------------------------------ //

    @Nested
    class SimplifiedSorterStringTests {

        @Test
        void emptyInput() {
            List<String> result = SimplifiedDependencyAwareSorter.sort(
                    List.of(), Groups.empty(), Dependencies.empty(),
                    NATURAL_ORDER);
            assertThat(result).isEmpty();
        }

        @Test
        void plainAlphabeticalSort() {
            List<String> result = SimplifiedDependencyAwareSorter.sort(
                    List.of("cherry", "apple", "banana"),
                    Groups.empty(), Dependencies.empty(),
                    NATURAL_ORDER);
            assertThat(result).containsExactly("apple", "banana", "cherry");
        }

        @Test
        void groupKeepsItemsTogether() {
            List<String> result = SimplifiedDependencyAwareSorter.sort(
                    List.of("zebra", "bravo", "alpha", "mango"),
                    new Groups<>(List.of(Group.of("zebra", "bravo"))),
                    Dependencies.empty(),
                    NATURAL_ORDER);
            assertThat(result).containsExactly("alpha", "bravo", "zebra", "mango");
        }

        @Test
        void dependencyOnSingletons() {
            List<String> result = SimplifiedDependencyAwareSorter.sort(
                    List.of("alpha", "bravo", "charlie"),
                    Groups.empty(),
                    Dependencies.of("charlie", "alpha"),
                    NATURAL_ORDER);
            assertThat(result.indexOf("charlie")).isLessThan(result.indexOf("alpha"));
        }

        @Test
        void groupsAndDepsOnDifferentItems() {
            List<String> result = SimplifiedDependencyAwareSorter.sort(
                    List.of("delta", "gamma", "beta", "alpha"),
                    new Groups<>(List.of(Group.of("alpha", "beta"))),
                    Dependencies.of("gamma", "delta"),
                    NATURAL_ORDER);
            assertThat(result.indexOf("alpha")).isLessThan(result.indexOf("beta"));
            assertThat(result.indexOf("gamma")).isLessThan(result.indexOf("delta"));
        }

        @Test
        void groupMemberInDepThrows() {
            assertThatThrownBy(() -> SimplifiedDependencyAwareSorter.sort(
                    List.of("alpha", "beta", "gamma"),
                    new Groups<>(List.of(Group.of("alpha", "beta"))),
                    Dependencies.of("alpha", "gamma"),
                    NATURAL_ORDER))
                    .isInstanceOf(SortingException.class)
                    .hasMessageContaining("alpha");
        }

        @Test
        void resultIsUnmodifiable() {
            List<String> result = SimplifiedDependencyAwareSorter.sort(
                    List.of("b", "a"), Groups.empty(), Dependencies.empty(),
                    NATURAL_ORDER);
            assertThatThrownBy(() -> result.add("x"))
                    .isInstanceOf(UnsupportedOperationException.class);
        }
    }

    // ------------------------------------------------------------------ //
    // Tests using Integer items — both sorters                            //
    // ------------------------------------------------------------------ //

    @Nested
    class IntegerItemTests {

        private static final Comparator<Integer> INT_ORDER = Comparator.naturalOrder();

        @Test
        void dependencyAwareSorterWithIntegers() {
            List<Integer> result = DependencyAwareSorter.sort(
                    List.of(3, 1, 4, 1_000, 5, 9),
                    Groups.empty(), Dependencies.empty(),
                    INT_ORDER);
            assertThat(result).containsExactly(1, 3, 4, 5, 9, 1_000);
        }

        @Test
        void simplifiedSorterWithIntegers() {
            List<Integer> result = SimplifiedDependencyAwareSorter.sort(
                    List.of(3, 1, 4, 1_000, 5, 9),
                    Groups.empty(), Dependencies.empty(),
                    INT_ORDER);
            assertThat(result).containsExactly(1, 3, 4, 5, 9, 1_000);
        }

        @Test
        void integerGroups() {
            List<Integer> result = DependencyAwareSorter.sort(
                    List.of(50, 30, 10, 20, 40),
                    new Groups<>(List.of(Group.of(50, 30))),
                    Dependencies.empty(),
                    INT_ORDER);
            // Group {30, 50} together, key = 30
            assertThat(result.indexOf(30)).isLessThan(result.indexOf(50));
            assertThat(Math.abs(result.indexOf(30) - result.indexOf(50))).isEqualTo(1);
        }

        @Test
        void integerDependencies() {
            List<Integer> result = DependencyAwareSorter.sort(
                    List.of(1, 2, 3),
                    Groups.empty(),
                    Dependencies.of(3, 1),
                    INT_ORDER);
            assertThat(result.indexOf(3)).isLessThan(result.indexOf(1));
        }

        @Test
        void integerLargeInput() {
            int total = 500;
            List<Integer> items = IntStream.range(0, total)
                    .boxed()
                    .collect(Collectors.toList());

            // Create some dependencies: 499→498, 498→497, ..., 401→400
            List<Dependencies.Dependency<Integer>> depList = IntStream.range(401, 500)
                    .mapToObj(i -> new Dependencies.Dependency<>(i, i - 1))
                    .toList();

            List<Integer> result = DependencyAwareSorter.sort(
                    items,
                    Groups.empty(),
                    new Dependencies<>(depList),
                    INT_ORDER);

            assertThat(result).hasSize(total);
            // Verify dependency ordering
            depList.forEach(dep ->
                    assertThat(result.indexOf(dep.getProvider()))
                            .as("%d must precede %d", dep.getProvider(), dep.getDependent())
                            .isLessThan(result.indexOf(dep.getDependent())));
        }
    }

    // ------------------------------------------------------------------ //
    // Tests using custom record type                                      //
    // ------------------------------------------------------------------ //

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
        void dependencyAwareSorterWithCustomRecord() {
            Task a = new Task("deploy", 3);
            Task b = new Task("build", 1);
            Task c = new Task("test", 2);
            Task d = new Task("lint", 1);

            List<Task> result = DependencyAwareSorter.sort(
                    List.of(a, b, c, d),
                    Groups.empty(),
                    Dependencies.of(b, c, c, a), // build → test → deploy
                    BY_PRIORITY_THEN_ID);

            assertThat(result.indexOf(b)).isLessThan(result.indexOf(c));
            assertThat(result.indexOf(c)).isLessThan(result.indexOf(a));
        }

        @Test
        void simplifiedSorterWithCustomRecord() {
            Task a = new Task("deploy", 3);
            Task b = new Task("build", 1);
            Task c = new Task("test", 2);
            Task d = new Task("lint", 1);

            // Group {build, lint} (both priority 1), dep: test → deploy
            List<Task> result = SimplifiedDependencyAwareSorter.sort(
                    List.of(a, b, c, d),
                    new Groups<>(List.of(Group.of(b, d))),
                    Dependencies.of(c, a), // test → deploy (no overlap with group)
                    BY_PRIORITY_THEN_ID);

            // Group {build, lint} must be together in priority order
            assertThat(result.indexOf(b)).isLessThan(result.indexOf(d));
            assertThat(Math.abs(result.indexOf(b) - result.indexOf(d))).isEqualTo(1);
            // test before deploy
            assertThat(result.indexOf(c)).isLessThan(result.indexOf(a));
        }
    }
}
