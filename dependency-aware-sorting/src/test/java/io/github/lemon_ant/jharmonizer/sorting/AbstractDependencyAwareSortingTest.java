package io.github.lemon_ant.jharmonizer.sorting;

import static org.assertj.core.api.Assertions.*;

import java.util.*;
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
        return Arrays.stream(names).map(SortableTypeMember::staticMember).toList();
    }

    static List<String> names(List<SortableTypeMember> members) {
        return members.stream().map(SortableTypeMember::getName).toList();
    }

    /** Creates a {@link Group} from item names (all STATIC, convenience). */
    static Group<SortableTypeMember> group(String... names) {
        return new Group<>(
                Arrays.stream(names).map(SortableTypeMember::staticMember).toList());
    }

    /** Creates a {@link Groups} from vararg name-arrays. */
    @SuppressWarnings("unchecked") // Group[]::new creates a raw-typed array; unavoidable
    // with Java's generic array creation restrictions — safe because elements are always
    // Group<SortableTypeMember> and the array is immediately consumed by Grouping.of().
    static Groups<SortableTypeMember> grouping(String[]... groups) {
        return Groups.of(Arrays.stream(groups).map(g -> group(g)).toArray(Group[]::new));
    }

    /** Creates {@link Dependencies} from alternating provider/dependent name pairs (all STATIC). */
    static Dependencies<SortableTypeMember> deps(String... pairs) {
        SortableTypeMember[] arr =
                Arrays.stream(pairs).map(SortableTypeMember::staticMember).toArray(SortableTypeMember[]::new);
        return Dependencies.of(arr);
    }

    // ------------------------------------------------ scenario 1 --------------- //

    @Test
    void defaultOrderStaticBeforeDynamic() {
        var result = sort(
                List.of(
                        dynamicMember("bravo"), staticMember("charlie"),
                        dynamicMember("alpha"), staticMember("delta")),
                Groups.empty(),
                Dependencies.empty());

        assertThat(names(result)).containsExactly("charlie", "delta", "alpha", "bravo");
    }

    @Test
    void plainAlphabeticalSort() {
        var result = sort(staticItems("cherry", "apple", "banana"), Groups.empty(), Dependencies.empty());

        assertThat(names(result)).containsExactly("apple", "banana", "cherry");
    }

    @Test
    void emptyInput() {
        var result = sort(List.of(), Groups.empty(), Dependencies.empty());
        assertThat(result).isEmpty();
    }

    @Test
    void singleItem() {
        var result = sort(staticItems("solo"), Groups.empty(), Dependencies.empty());
        assertThat(names(result)).containsExactly("solo");
    }

    // ------------------------------------------------ scenario 2 --------------- //

    @Test
    void oneGroupPlusSingletons() {
        var result = sort(
                staticItems("zebra", "bravo", "alpha", "mango"),
                grouping(new String[] {"zebra", "bravo"}),
                Dependencies.empty());

        assertThat(names(result)).containsExactly("alpha", "bravo", "zebra", "mango");
    }

    // ------------------------------------------------ scenario 3 --------------- //

    @Test
    void multipleGroups() {
        var result = sort(
                staticItems("delta", "echo", "alpha", "beta", "charlie"),
                grouping(new String[] {"delta", "echo"}, new String[] {"alpha", "beta"}),
                Dependencies.empty());

        assertThat(names(result)).containsExactly("alpha", "beta", "charlie", "delta", "echo");
    }

    @Test
    void groupItemsSortedByComparatorInternally() {
        var result = sort(staticItems("z", "m", "a"), grouping(new String[] {"z", "m", "a"}), Dependencies.empty());

        assertThat(names(result)).containsExactly("a", "m", "z");
    }

    // ------------------------------------------------ scenario 4 --------------- //

    @Test
    void dependenciesWithoutGroups() {
        var result = sort(staticItems("gamma", "alpha", "delta", "beta"), Groups.empty(), deps("gamma", "alpha"));

        List<String> resultNames = names(result);
        assertThat(resultNames.indexOf("gamma")).isLessThan(resultNames.indexOf("alpha"));
    }

    @Test
    void dependencyForcesProviderBeforeDependent() {
        var result = sort(staticItems("alpha", "bravo", "charlie"), Groups.empty(), deps("charlie", "alpha"));

        List<String> resultNames = names(result);
        assertThat(resultNames.indexOf("charlie")).isLessThan(resultNames.indexOf("alpha"));
    }

    // ------------------------------------------------ scenario 6 --------------- //

    @Test
    void transitiveDependencies() {
        var result = sort(staticItems("a", "b", "c", "d"), Groups.empty(), deps("d", "c", "c", "b", "b", "a"));

        assertThat(names(result)).containsExactly("d", "c", "b", "a");
    }

    @Test
    void multipleDependenciesOnOneItem() {
        var result = sort(
                staticItems("alpha", "beta", "charlie", "delta"),
                Groups.empty(),
                deps("charlie", "alpha", "delta", "alpha"));

        List<String> resultNames = names(result);
        assertThat(resultNames.indexOf("charlie")).isLessThan(resultNames.indexOf("alpha"));
        assertThat(resultNames.indexOf("delta")).isLessThan(resultNames.indexOf("alpha"));
    }

    // ------------------------------------------------ scenario 7 --------------- //

    @Test
    void cycleThrowsSortingException() {
        assertThatThrownBy(() -> sort(staticItems("a", "b", "c"), Groups.empty(), deps("a", "b", "b", "c", "c", "a")))
                .isInstanceOf(SortingException.class)
                .message()
                .containsIgnoringCase("cycle");
    }

    @Test
    void selfDependencyCycleThrows() {
        assertThatThrownBy(() -> sort(staticItems("a", "b"), Groups.empty(), deps("a", "a")))
                .isInstanceOf(SortingException.class)
                .message()
                .containsIgnoringCase("self");
    }

    // ------------------------------------------------ scenario 8 --------------- //

    @Test
    void memberInTwoGroupsThrowsSortingException() {
        assertThatThrownBy(() -> sort(
                        staticItems("a", "b", "c"),
                        grouping(new String[] {"a", "b"}, new String[] {"b", "c"}),
                        Dependencies.empty()))
                .isInstanceOf(SortingException.class)
                .hasMessageContaining("b");
    }

    // ------------------------------------------------ scenario 9 --------------- //

    @Test
    void duplicateMemberNamesThrowsSortingException() {
        assertThatThrownBy(() -> sort(staticItems("apple", "banana", "apple"), Groups.empty(), Dependencies.empty()))
                .isInstanceOf(SortingException.class)
                .hasMessageContaining("apple");
    }

    // ------------------------------------------------ scenario 11 -------------- //

    @Test
    void determinismSameInputSameOutput() {
        List<SortableTypeMember> itemList = staticItems("fig", "cherry", "apple", "elderberry", "banana", "date");
        Groups g = grouping(new String[] {"fig", "date"});
        Dependencies d = deps("cherry", "apple", "elderberry", "banana");

        List<String> first = names(sort(itemList, g, d));
        List<String> second = names(sort(itemList, g, d));
        List<String> third = names(sort(itemList, g, d));

        assertThat(second).isEqualTo(first);
        assertThat(third).isEqualTo(first);
    }

    @Test
    void determinismShuffledInputSameOutput() {
        List<SortableTypeMember> base = staticItems("fig", "cherry", "apple", "elderberry", "banana", "date");
        Groups g = grouping(new String[] {"fig", "date"});
        Dependencies d = deps("cherry", "apple");

        List<String> expected = names(sort(base, g, d));

        Random rng = new Random(42);
        for (int run = 0; run < 20; run++) {
            List<SortableTypeMember> shuffled = new ArrayList<>(base);
            Collections.shuffle(shuffled, rng);
            assertThat(names(sort(shuffled, g, d)))
                    .as("run %d: result must be deterministic regardless of input order", run)
                    .isEqualTo(expected);
        }
    }

    // ------------------------------------------------ scenario 12 -------------- //

    @Test
    void largeInputSanityTest() {
        int total = 2000;
        List<SortableTypeMember> bigItems = IntStream.range(0, total)
                .mapToObj(i -> SortableTypeMember.staticMember(String.format("item%04d", i)))
                .collect(Collectors.toList());

        List<Dependencies.Dependency<SortableTypeMember>> depList = IntStream.range(1, 500)
                .mapToObj(i -> new Dependencies.Dependency<>(bigItems.get(i + 500), bigItems.get(i)))
                .toList();
        Dependencies<SortableTypeMember> bigDeps = new Dependencies<>(depList);

        long start = System.nanoTime();
        List<SortableTypeMember> result = sort(bigItems, Groups.empty(), bigDeps);
        long elapsedMs = (System.nanoTime() - start) / 1_000_000;

        assertThat(result).hasSize(total);
        List<String> resultNames = names(result);
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
    void customComparatorReversesOrder() {
        Comparator<SortableTypeMember> reversed =
                Comparator.comparing(SortableTypeMember::getName).reversed();

        var result = sort(staticItems("alpha", "beta", "charlie"), Groups.empty(), Dependencies.empty(), reversed);

        assertThat(names(result)).containsExactly("charlie", "beta", "alpha");
    }

    @Test
    void customComparatorAppliedInsideGroup() {
        Comparator<SortableTypeMember> reversed =
                Comparator.comparing(SortableTypeMember::getName).reversed();

        var result = sort(
                staticItems("alpha", "charlie", "beta"),
                grouping(new String[] {"alpha", "charlie", "beta"}),
                Dependencies.empty(),
                reversed);

        assertThat(names(result)).containsExactly("charlie", "beta", "alpha");
    }

    // ------------------------------------------------ edge cases --------------- //

    @Test
    void blankMemberNameThrowsSortingException() {
        assertThatThrownBy(() -> SortableTypeMember.staticMember("   "))
                .isInstanceOf(SortingException.class)
                .message()
                .containsIgnoringCase("blank");
    }

    @Test
    void unknownMemberInGroupThrows() {
        assertThatThrownBy(() ->
                        sort(staticItems("a", "b"), grouping(new String[] {"a", "NONEXISTENT"}), Dependencies.empty()))
                .isInstanceOf(SortingException.class);
    }

    @Test
    void unknownProviderInDependencyThrows() {
        assertThatThrownBy(() -> sort(staticItems("a", "b"), Groups.empty(), deps("GHOST", "a")))
                .isInstanceOf(SortingException.class);
    }

    @Test
    void unknownDependentInDependencyThrows() {
        assertThatThrownBy(() -> sort(staticItems("a", "b"), Groups.empty(), deps("a", "GHOST")))
                .isInstanceOf(SortingException.class);
    }

    @Test
    void resultIsUnmodifiable() {
        var result = sort(staticItems("b", "a"), Groups.empty(), Dependencies.empty());
        assertThatThrownBy(() -> result.add(staticMember("x"))).isInstanceOf(UnsupportedOperationException.class);
    }

    // ------------------------------------------------ mixed STATIC/DYNAMIC ----- //

    @Test
    void groupWithMixedNumerationOrderedByDefaultComparator() {
        Group<SortableTypeMember> mixed = new Group<>(
                List.of(dynamicMember("beta"), staticMember("charlie"), dynamicMember("alpha"), staticMember("delta")));

        var result = sort(
                List.of(
                        dynamicMember("beta"), staticMember("charlie"),
                        dynamicMember("alpha"), staticMember("delta")),
                new Groups<>(List.of(mixed)),
                Dependencies.empty());

        assertThat(names(result)).containsExactly("charlie", "delta", "alpha", "beta");
    }
}
