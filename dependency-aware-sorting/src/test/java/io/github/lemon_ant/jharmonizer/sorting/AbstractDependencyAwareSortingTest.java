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
import org.junit.jupiter.api.Test;

/**
 * Shared test scenarios for any implementation of the dependency-aware sorting contract.
 *
 * <p>Subclasses must implement the two {@code sort} overloads. Every {@code @Test} method
 * in this class exercises behaviour that is valid under the
 * {@link SimplifiedDependencyAwareSorter} algorithm —
 * i.e. groups and dependencies never overlap.</p>
 */
abstract class AbstractDependencyAwareSortingTest {

    // --------------------------------------------------------- abstract sort --- //

    abstract List<SortableTypeMember> sort(
            List<SortableTypeMember> members,
            Groups<SortableTypeMember> groups,
            Dependencies<SortableTypeMember> dependencies);

    abstract List<SortableTypeMember> sort(
            List<SortableTypeMember> members,
            Groups<SortableTypeMember> groups,
            Dependencies<SortableTypeMember> dependencies,
            Comparator<SortableTypeMember> comparator);

    // --------------------------------------------------------- helpers --------- //

    static SortableTypeMember staticMember(String name) {
        return SortableTypeMember.staticMember(name);
    }

    static SortableTypeMember dynamicMember(String name) {
        return SortableTypeMember.dynamicMember(name);
    }

    /** Builds a list of STATIC members from the given names. */
    static List<SortableTypeMember> staticItems(String... names) {
        return java.util.Arrays.stream(names)
                .map(SortableTypeMember::staticMember)
                .toList();
    }

    static List<String> names(List<SortableTypeMember> members) {
        return members.stream().map(SortableTypeMember::getName).toList();
    }

    /** Creates a {@link Group} from item names (all STATIC, convenience). */
    static Group<SortableTypeMember> group(String... names) {
        return new Group<>(java.util.Arrays.stream(names)
                .map(SortableTypeMember::staticMember)
                .toList());
    }

    /** Creates a {@link Groups} from vararg name-arrays. */
    @SuppressWarnings("unchecked")
    static Groups<SortableTypeMember> grouping(String[]... groups) {
        return Groups.of(java.util.Arrays.stream(groups)
                .map(groupNames -> group(groupNames))
                .toArray(Group[]::new));
    }

    /** Creates {@link Dependencies} from alternating provider/dependent name pairs (all STATIC). */
    static Dependencies<SortableTypeMember> deps(String... pairs) {
        SortableTypeMember[] arr = java.util.Arrays.stream(pairs)
                .map(SortableTypeMember::staticMember)
                .toArray(SortableTypeMember[]::new);
        return Dependencies.of(arr);
    }

    // ------------------------------------------------ scenario 1 --------------- //

    @Test
    void sort_defaultOrder_placesStaticBeforeDynamic() {
        // Given
        var members = List.of(
                dynamicMember("bravo"), staticMember("charlie"),
                dynamicMember("alpha"), staticMember("delta"));
        var groups = Groups.<SortableTypeMember>empty();
        var dependencies = Dependencies.<SortableTypeMember>empty();

        // When
        var result = sort(members, groups, dependencies);

        // Then
        assertThat(names(result)).containsExactly("charlie", "delta", "alpha", "bravo");
    }

    @Test
    void sort_plainAlphabeticalInput_returnsAlphabeticalOrder() {
        // Given
        var members = staticItems("cherry", "apple", "banana");

        // When
        var result = sort(members, Groups.empty(), Dependencies.empty());

        // Then
        assertThat(names(result)).containsExactly("apple", "banana", "cherry");
    }

    @Test
    void sort_emptyInput_returnsEmptyList() {
        // When
        var result = sort(List.of(), Groups.empty(), Dependencies.empty());

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void sort_singleItem_returnsSameItem() {
        // When
        var result = sort(staticItems("solo"), Groups.empty(), Dependencies.empty());

        // Then
        assertThat(names(result)).containsExactly("solo");
    }

    // ------------------------------------------------ scenario 2 --------------- //

    @Test
    void sort_oneGroupPlusSingletons_groupKeepsTogether() {
        // Given
        var members = staticItems("zebra", "bravo", "alpha", "mango");
        var groups = grouping(new String[] {"zebra", "bravo"});

        // When
        var result = sort(members, groups, Dependencies.empty());

        // Then
        assertThat(names(result)).containsExactly("alpha", "bravo", "zebra", "mango");
    }

    // ------------------------------------------------ scenario 3 --------------- //

    @Test
    void sort_multipleGroups_groupsMaintainInternalOrder() {
        // Given
        var members = staticItems("delta", "echo", "alpha", "beta", "charlie");
        var groups = grouping(new String[] {"delta", "echo"}, new String[] {"alpha", "beta"});

        // When
        var result = sort(members, groups, Dependencies.empty());

        // Then
        assertThat(names(result)).containsExactly("alpha", "beta", "charlie", "delta", "echo");
    }

    @Test
    void sort_allItemsInOneGroup_sortedByComparatorInternally() {
        // When
        var result = sort(staticItems("z", "m", "a"), grouping(new String[] {"z", "m", "a"}), Dependencies.empty());

        // Then
        assertThat(names(result)).containsExactly("a", "m", "z");
    }

    // ------------------------------------------------ scenario 4 --------------- //

    @Test
    void sort_withDependencyNoGroups_providerPrecedesDependent() {
        // Given
        var members = staticItems("gamma", "alpha", "delta", "beta");

        // When
        var result = sort(members, Groups.empty(), deps("gamma", "alpha"));

        // Then
        var resultNames = names(result);
        assertThat(resultNames.indexOf("gamma")).isLessThan(resultNames.indexOf("alpha"));
    }

    @Test
    void sort_dependencyReversesNaturalOrder_providerBeforeDependent() {
        // Given
        var members = staticItems("alpha", "bravo", "charlie");

        // When
        var result = sort(members, Groups.empty(), deps("charlie", "alpha"));

        // Then
        var resultNames = names(result);
        assertThat(resultNames.indexOf("charlie")).isLessThan(resultNames.indexOf("alpha"));
    }

    // ------------------------------------------------ scenario 6 --------------- //

    @Test
    void sort_transitiveChain_respectsFullOrdering() {
        // When
        var result = sort(staticItems("a", "b", "c", "d"), Groups.empty(), deps("d", "c", "c", "b", "b", "a"));

        // Then
        assertThat(names(result)).containsExactly("d", "c", "b", "a");
    }

    @Test
    void sort_multipleProvidersOneDependent_allProvidersPrecedeDependent() {
        // Given
        var members = staticItems("alpha", "beta", "charlie", "delta");

        // When
        var result = sort(members, Groups.empty(), deps("charlie", "alpha", "delta", "alpha"));

        // Then
        var resultNames = names(result);
        assertThat(resultNames.indexOf("charlie")).isLessThan(resultNames.indexOf("alpha"));
        assertThat(resultNames.indexOf("delta")).isLessThan(resultNames.indexOf("alpha"));
    }

    // ------------------------------------------------ scenario 7 --------------- //

    @Test
    void sort_cyclicDependency_throwsSortingException() {
        // When / Then
        assertThatThrownBy(() -> sort(staticItems("a", "b", "c"), Groups.empty(), deps("a", "b", "b", "c", "c", "a")))
                .isInstanceOf(SortingException.class)
                .message()
                .containsIgnoringCase("cycle");
    }

    @Test
    void sort_selfDependency_throwsSortingException() {
        // When / Then
        assertThatThrownBy(() -> sort(staticItems("a", "b"), Groups.empty(), deps("a", "a")))
                .isInstanceOf(SortingException.class)
                .message()
                .containsIgnoringCase("self");
    }

    // ------------------------------------------------ scenario 8 --------------- //

    @Test
    void sort_memberInTwoGroups_throwsSortingException() {
        // When / Then
        assertThatThrownBy(() -> sort(
                        staticItems("a", "b", "c"),
                        grouping(new String[] {"a", "b"}, new String[] {"b", "c"}),
                        Dependencies.empty()))
                .isInstanceOf(SortingException.class)
                .hasMessageContaining("b");
    }

    // ------------------------------------------------ scenario 9 --------------- //

    @Test
    void sort_duplicateMemberNames_throwsSortingException() {
        // When / Then
        assertThatThrownBy(() -> sort(staticItems("apple", "banana", "apple"), Groups.empty(), Dependencies.empty()))
                .isInstanceOf(SortingException.class)
                .hasMessageContaining("apple");
    }

    // ------------------------------------------------ scenario 11 -------------- //

    @Test
    void sort_sameSeed_producesIdenticalResultsAcrossThreeRuns() {
        // Given
        var itemList = staticItems("fig", "cherry", "apple", "elderberry", "banana", "date");
        var groups = grouping(new String[] {"fig", "date"});
        var dependencies = deps("cherry", "apple", "elderberry", "banana");

        // When
        var first = names(sort(itemList, groups, dependencies));
        var second = names(sort(itemList, groups, dependencies));
        var third = names(sort(itemList, groups, dependencies));

        // Then
        assertThat(second).isEqualTo(first);
        assertThat(third).isEqualTo(first);
    }

    @Test
    void sort_shuffledInput_producesDeterministicResult() {
        // Given
        var base = staticItems("fig", "cherry", "apple", "elderberry", "banana", "date");
        var groups = grouping(new String[] {"fig", "date"});
        var dependencies = deps("cherry", "apple");
        var expected = names(sort(base, groups, dependencies));
        var rng = new Random(42);

        for (int run = 0; run < 20; run++) {
            // When
            var shuffled = new ArrayList<>(base);
            Collections.shuffle(shuffled, rng);
            var actual = names(sort(shuffled, groups, dependencies));

            // Then
            assertThat(actual)
                    .as("run %d: result must be deterministic regardless of input order", run)
                    .isEqualTo(expected);
        }
    }

    // ------------------------------------------------ scenario 12 -------------- //

    @Test
    void sort_twoThousandItems_completesWithinReasonableTime() {
        // Given
        int total = 2000;
        var bigItems = IntStream.range(0, total)
                .mapToObj(i -> SortableTypeMember.staticMember(String.format("item%04d", i)))
                .collect(Collectors.toList());
        var depList = IntStream.range(1, 500)
                .mapToObj(i -> new Dependencies.Dependency<>(bigItems.get(i + 500), bigItems.get(i)))
                .toList();
        var bigDeps = new Dependencies<>(depList);

        // When
        long start = System.nanoTime();
        var result = sort(bigItems, Groups.empty(), bigDeps);
        long elapsedMs = (System.nanoTime() - start) / 1_000_000;

        // Then
        assertThat(result).hasSize(total);
        var resultNames = names(result);
        depList.forEach(dep -> assertThat(resultNames.indexOf(dep.getProvider().getName()))
                .as(
                        "%s must precede %s",
                        dep.getProvider().getName(), dep.getDependent().getName())
                .isLessThan(resultNames.indexOf(dep.getDependent().getName())));
        System.out.printf(
                "Large-input test (%d items, %d deps) completed in %d ms%n", total, depList.size(), elapsedMs);
    }

    // ------------------------------------------------ scenario 13 -------------- //

    @Test
    void sort_reversedComparator_returnsReverseAlphabeticalOrder() {
        // Given
        var reversed = Comparator.comparing(SortableTypeMember::getName).reversed();

        // When
        var result = sort(staticItems("alpha", "beta", "charlie"), Groups.empty(), Dependencies.empty(), reversed);

        // Then
        assertThat(names(result)).containsExactly("charlie", "beta", "alpha");
    }

    @Test
    void sort_reversedComparatorWithGroup_groupSortedByComparatorInternally() {
        // Given
        var reversed = Comparator.comparing(SortableTypeMember::getName).reversed();

        // When
        var result = sort(
                staticItems("alpha", "charlie", "beta"),
                grouping(new String[] {"alpha", "charlie", "beta"}),
                Dependencies.empty(),
                reversed);

        // Then
        assertThat(names(result)).containsExactly("charlie", "beta", "alpha");
    }

    // ------------------------------------------------ edge cases --------------- //

    @Test
    void staticMember_blankName_throwsSortingException() {
        // When / Then
        assertThatThrownBy(() -> SortableTypeMember.staticMember("   "))
                .isInstanceOf(SortingException.class)
                .message()
                .containsIgnoringCase("blank");
    }

    @Test
    void sort_unknownMemberInGroup_throwsSortingException() {
        // When / Then
        assertThatThrownBy(() ->
                        sort(staticItems("a", "b"), grouping(new String[] {"a", "NONEXISTENT"}), Dependencies.empty()))
                .isInstanceOf(SortingException.class);
    }

    @Test
    void sort_unknownProviderInDependency_throwsSortingException() {
        // When / Then
        assertThatThrownBy(() -> sort(staticItems("a", "b"), Groups.empty(), deps("GHOST", "a")))
                .isInstanceOf(SortingException.class);
    }

    @Test
    void sort_unknownDependentInDependency_throwsSortingException() {
        // When / Then
        assertThatThrownBy(() -> sort(staticItems("a", "b"), Groups.empty(), deps("a", "GHOST")))
                .isInstanceOf(SortingException.class);
    }

    @Test
    void sort_validInput_resultIsUnmodifiable() {
        // When
        var result = sort(staticItems("b", "a"), Groups.empty(), Dependencies.empty());

        // Then
        assertThatThrownBy(() -> result.add(staticMember("x"))).isInstanceOf(UnsupportedOperationException.class);
    }

    // ------------------------------------------------ empty group -------------- //

    @Test
    void sort_groupWithNoItems_emptyGroupSkippedAndItemsSorted() {
        // Given
        List<SortableTypeMember> members = staticItems("b", "a");
        Groups<SortableTypeMember> emptyGroup = new Groups<>(List.of(new Group<>(List.of())));

        // When
        List<SortableTypeMember> result = sort(members, emptyGroup, Dependencies.empty());

        // Then
        assertThat(names(result)).containsExactly("a", "b");
    }

    // ------------------------------------------------ provider-lift edge cases - //

    @Test
    void sort_blockedNodeWithAlreadyEmittedProvider_liftsMissingProviderOnly() {
        // Given: a->b and d->b; natural order a,b,c,d — when b is processed, a is already
        // emitted but d is not, so only d is lifted; this covers the seedStack branch where
        // an emitted provider is encountered during transitive-closure computation.
        List<SortableTypeMember> members = staticItems("a", "b", "c", "d");
        Dependencies<SortableTypeMember> dependencies = deps("a", "b", "d", "b");

        // When
        List<SortableTypeMember> result = sort(members, Groups.empty(), dependencies);

        // Then
        List<String> resultNames = names(result);
        assertThat(resultNames).hasSize(4);
        assertThat(resultNames.indexOf("a")).isLessThan(resultNames.indexOf("b"));
        assertThat(resultNames.indexOf("d")).isLessThan(resultNames.indexOf("b"));
    }

    @Test
    void sort_multiProviderTransitiveClosure_allDependencyConstraintsRespected() {
        // Given: b->a, c->a, d->b, d->c, e->c — node a is blocked; its transitive
        // provider closure {b,c,d,e} requires a multi-node topological subset sort,
        // exercising the intra-subset in-degree tracking and nodeSet membership check.
        List<SortableTypeMember> members = staticItems("a", "b", "c", "d", "e");
        Dependencies<SortableTypeMember> dependencies = deps("b", "a", "c", "a", "d", "b", "d", "c", "e", "c");

        // When
        List<SortableTypeMember> result = sort(members, Groups.empty(), dependencies);

        // Then
        List<String> resultNames = names(result);
        assertThat(resultNames).hasSize(5);
        assertThat(resultNames.indexOf("b")).isLessThan(resultNames.indexOf("a"));
        assertThat(resultNames.indexOf("c")).isLessThan(resultNames.indexOf("a"));
        assertThat(resultNames.indexOf("d")).isLessThan(resultNames.indexOf("b"));
        assertThat(resultNames.indexOf("d")).isLessThan(resultNames.indexOf("c"));
        assertThat(resultNames.indexOf("e")).isLessThan(resultNames.indexOf("c"));
    }

    // ------------------------------------------------ mixed STATIC/DYNAMIC ----- //

    @Test
    void sort_groupWithMixedNumeration_sortedByDefaultComparator() {
        // Given
        var mixed = new Group<>(List.of(
                dynamicMember("beta"), staticMember("charlie"),
                dynamicMember("alpha"), staticMember("delta")));
        var members = List.of(
                dynamicMember("beta"), staticMember("charlie"),
                dynamicMember("alpha"), staticMember("delta"));

        // When
        var result = sort(members, new Groups<>(List.of(mixed)), Dependencies.empty());

        // Then
        assertThat(names(result)).containsExactly("charlie", "delta", "alpha", "beta");
    }
}
