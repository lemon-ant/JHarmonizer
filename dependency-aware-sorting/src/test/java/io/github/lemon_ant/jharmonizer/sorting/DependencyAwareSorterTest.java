package io.github.lemon_ant.jharmonizer.sorting;

import org.junit.jupiter.api.Test;

import java.util.*;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests for {@link DependencyAwareSorter}.
 *
 * <p>Inherits all shared tests from {@link AbstractDependencyAwareSortingTest} (scenarios valid
 * for both general and simplified algorithms). This subclass adds scenarios specific to
 * the general algorithm — e.g. dependencies between groups and intra-group dependency
 * validation.</p>
 */
class DependencyAwareSorterTest extends AbstractDependencyAwareSortingTest {

    // --------------------------------------------------------- sort impl --- //

    @Override
    List<SortableTypeMember> sort(List<SortableTypeMember> members,
                                  Groups<SortableTypeMember> groups,
                                  Dependencies<SortableTypeMember> dependencies) {
        return DependencyAwareSorter.sort(members, groups, dependencies,
                SortableTypeMember.DEFAULT_ORDER);
    }

    @Override
    List<SortableTypeMember> sort(List<SortableTypeMember> members,
                                  Groups<SortableTypeMember> groups,
                                  Dependencies<SortableTypeMember> dependencies,
                                  Comparator<SortableTypeMember> comparator) {
        return DependencyAwareSorter.sort(members, groups, dependencies,
                comparator);
    }

    // --- scenarios specific to the general algorithm (group + dep overlap) --- //

    @Test
    void dependenciesBetweenGroups() {
        var result = DependencyAwareSorter.sort(
                staticItems("delta", "echo", "alpha", "beta"),
                grouping(new String[]{"delta", "echo"}, new String[]{"alpha", "beta"}),
                deps("delta", "beta"),
                SortableTypeMember.DEFAULT_ORDER);

        List<String> resultNames = names(result);
        assertThat(resultNames.indexOf("delta")).isLessThan(resultNames.indexOf("alpha"));
        assertThat(resultNames.indexOf("echo")).isLessThan(resultNames.indexOf("beta"));
        assertThat(resultNames.indexOf("delta")).isLessThan(resultNames.indexOf("echo"));
        assertThat(resultNames.indexOf("alpha")).isLessThan(resultNames.indexOf("beta"));
    }

    @Test
    void intraGroupDependencyConflictingWithOrderThrows() {
        assertThatThrownBy(() ->
                DependencyAwareSorter.sort(
                        staticItems("alpha", "beta"),
                        grouping(new String[]{"alpha", "beta"}),
                        deps("beta", "alpha"),
                        SortableTypeMember.DEFAULT_ORDER))
                .isInstanceOf(SortingException.class)
                .message().containsIgnoringCase("group");
    }

    @Test
    void intraGroupDependencyCompatibleWithOrderIsAccepted() {
        assertThatNoException().isThrownBy(() ->
                DependencyAwareSorter.sort(
                        staticItems("alpha", "beta"),
                        grouping(new String[]{"alpha", "beta"}),
                        deps("alpha", "beta"),
                        SortableTypeMember.DEFAULT_ORDER));
    }
}
