package io.github.lemon_ant.jharmonizer.sorting;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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

    // --- null-argument guards --- //

    @Test
    void sort_nullItems_throwsNullPointerException() {
        // When / Then
        assertThatThrownBy(() -> SimplifiedDependencyAwareSorter.sort(
                        null, Groups.empty(), Dependencies.empty(), SortableTypeMember.DEFAULT_ORDER))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void sort_nullGroups_throwsNullPointerException() {
        // When / Then
        assertThatThrownBy(() -> SimplifiedDependencyAwareSorter.sort(
                        List.of(), null, Dependencies.empty(), SortableTypeMember.DEFAULT_ORDER))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void sort_nullDependencies_throwsNullPointerException() {
        // When / Then
        assertThatThrownBy(() -> SimplifiedDependencyAwareSorter.sort(
                        List.of(), Groups.empty(), null, SortableTypeMember.DEFAULT_ORDER))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void sort_nullComparator_throwsNullPointerException() {
        // When / Then
        assertThatThrownBy(() ->
                        SimplifiedDependencyAwareSorter.sort(List.of(), Groups.empty(), Dependencies.empty(), null))
                .isInstanceOf(NullPointerException.class);
    }

    // --- simplified constraint validations --- //

    @Test
    void sort_groupMemberIsProvider_throwsSortingException() {
        // Given
        var members = staticItems("alpha", "beta", "gamma");
        var groups = grouping(new String[] {"alpha", "beta"});
        var dependencies = deps("alpha", "gamma");

        // When / Then
        assertThatThrownBy(() -> SimplifiedDependencyAwareSorter.sort(
                        members, groups, dependencies, SortableTypeMember.DEFAULT_ORDER))
                .isInstanceOf(SortingException.class)
                .hasMessageContaining("alpha")
                .hasMessageContaining("provider");
    }

    @Test
    void sort_groupMemberIsDependent_throwsSortingException() {
        // Given
        var members = staticItems("alpha", "beta", "gamma");
        var groups = grouping(new String[] {"alpha", "beta"});
        var dependencies = deps("gamma", "alpha");

        // When / Then
        assertThatThrownBy(() -> SimplifiedDependencyAwareSorter.sort(
                        members, groups, dependencies, SortableTypeMember.DEFAULT_ORDER))
                .isInstanceOf(SortingException.class)
                .hasMessageContaining("alpha")
                .hasMessageContaining("dependent");
    }

    @Test
    void sort_groupSizeOfFour_returnsMembersGroupedAndSorted() {
        // Given
        var members = staticItems("delta", "charlie", "bravo", "alpha", "echo");
        var groups = grouping(new String[] {"delta", "charlie", "bravo", "alpha"});

        // When
        var result = SimplifiedDependencyAwareSorter.sort(
                members, groups, Dependencies.empty(), SortableTypeMember.DEFAULT_ORDER);

        // Then
        assertThat(names(result)).containsExactly("alpha", "bravo", "charlie", "delta", "echo");
    }

    @Test
    void sort_largeGroup_returnsMembersGroupedAndSorted() {
        // Given
        var members = staticItems("a", "b", "c", "d", "e");
        var groups = grouping(new String[] {"a", "b", "c", "d", "e"});

        // When
        var result = SimplifiedDependencyAwareSorter.sort(
                members, groups, Dependencies.empty(), SortableTypeMember.DEFAULT_ORDER);

        // Then
        assertThat(names(result)).containsExactly("a", "b", "c", "d", "e");
    }

    @Test
    void sort_groupsAndDepsOnDisjointMembers_noExceptionAndBothConstraintsHonoured() {
        // Given
        var members = staticItems("delta", "gamma", "beta", "alpha");
        var groups = grouping(new String[] {"alpha", "beta"});
        var dependencies = deps("gamma", "delta");

        // When
        var result =
                SimplifiedDependencyAwareSorter.sort(members, groups, dependencies, SortableTypeMember.DEFAULT_ORDER);

        // Then
        var resultNames = names(result);
        assertThat(resultNames.indexOf("alpha")).isLessThan(resultNames.indexOf("beta"));
        assertThat(resultNames.indexOf("gamma")).isLessThan(resultNames.indexOf("delta"));
    }

    @Test
    void sort_multipleGroupsWithDepsOnSingletons_allConstraintsHonoured() {
        // Given
        var members = staticItems("f", "e", "d", "c", "b", "a");
        var groups = grouping(new String[] {"a", "b"}, new String[] {"e", "f"});
        var dependencies = deps("d", "c");

        // When
        var result =
                SimplifiedDependencyAwareSorter.sort(members, groups, dependencies, SortableTypeMember.DEFAULT_ORDER);

        // Then
        var resultNames = names(result);
        assertThat(resultNames.indexOf("a")).isLessThan(resultNames.indexOf("b"));
        assertThat(resultNames.indexOf("e")).isLessThan(resultNames.indexOf("f"));
        assertThat(resultNames.indexOf("d")).isLessThan(resultNames.indexOf("c"));
    }
}
