package io.github.lemon_ant.jharmonizer.sorting;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Comparator;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link SimplifiedDependencyAwareSorter}.
 *
 * <p>Inherits all shared tests from {@link AbstractDependencyAwareSortingTest} and adds
 * validation tests specific to the simplified constraints:</p>
 * <ul>
 *   <li>Group members must not participate in any dependency</li>
 * </ul>
 */
class SimplifiedDependencyAwareSorterTest extends AbstractDependencyAwareSortingTest {

    // --------------------------------------------------------- sort impl --- //

    @Override
    List<SortableTypeMember> sort(
            List<SortableTypeMember> members,
            Groups<SortableTypeMember> groups,
            Dependencies<SortableTypeMember> dependencies) {
        return SimplifiedDependencyAwareSorter.sort(members, groups, dependencies, SortableTypeMember.DEFAULT_ORDER);
    }

    @Override
    List<SortableTypeMember> sort(
            List<SortableTypeMember> members,
            Groups<SortableTypeMember> groups,
            Dependencies<SortableTypeMember> dependencies,
            Comparator<SortableTypeMember> comparator) {
        return SimplifiedDependencyAwareSorter.sort(members, groups, dependencies, comparator);
    }

    // --- simplified constraint validations --- //

    @Test
    void groupSizeOfFourIsAccepted() {
        var result = SimplifiedDependencyAwareSorter.sort(
                staticItems("delta", "charlie", "bravo", "alpha", "echo"),
                grouping(new String[] {"delta", "charlie", "bravo", "alpha"}),
                Dependencies.empty(),
                SortableTypeMember.DEFAULT_ORDER);

        assertThat(names(result)).containsExactly("alpha", "bravo", "charlie", "delta", "echo");
    }

    @Test
    void largeGroupIsAccepted() {
        var result = SimplifiedDependencyAwareSorter.sort(
                staticItems("a", "b", "c", "d", "e"),
                grouping(new String[] {"a", "b", "c", "d", "e"}),
                Dependencies.empty(),
                SortableTypeMember.DEFAULT_ORDER);

        assertThat(names(result)).containsExactly("a", "b", "c", "d", "e");
    }

    @Test
    void groupsAndDepsOnDifferentMembersAccepted() {
        // Groups on {alpha, beta}, deps on gamma → delta (no overlap)
        var result = SimplifiedDependencyAwareSorter.sort(
                staticItems("delta", "gamma", "beta", "alpha"),
                grouping(new String[] {"alpha", "beta"}),
                deps("gamma", "delta"),
                SortableTypeMember.DEFAULT_ORDER);

        List<String> resultNames = names(result);
        // alpha, beta are grouped (in comparator order); gamma before delta
        assertThat(resultNames.indexOf("alpha")).isLessThan(resultNames.indexOf("beta"));
        assertThat(resultNames.indexOf("gamma")).isLessThan(resultNames.indexOf("delta"));
    }

    @Test
    void multipleGroupsWithDepsOnSingletons() {
        var result = SimplifiedDependencyAwareSorter.sort(
                staticItems("f", "e", "d", "c", "b", "a"),
                grouping(new String[] {"a", "b"}, new String[] {"e", "f"}),
                deps("d", "c"),
                SortableTypeMember.DEFAULT_ORDER);

        List<String> resultNames = names(result);
        // Groups {a,b} and {e,f} are independent; d before c
        assertThat(resultNames.indexOf("a")).isLessThan(resultNames.indexOf("b"));
        assertThat(resultNames.indexOf("e")).isLessThan(resultNames.indexOf("f"));
        assertThat(resultNames.indexOf("d")).isLessThan(resultNames.indexOf("c"));
    }
}
