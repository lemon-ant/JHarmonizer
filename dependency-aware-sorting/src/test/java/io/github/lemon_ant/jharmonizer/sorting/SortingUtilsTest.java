package io.github.lemon_ant.jharmonizer.sorting;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Collections;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link SortingUtils} utility methods.
 *
 * <p>These tests exercise the package-private API, focusing on null-argument guards
 * that are not reachable through the public {@link SimplifiedDependencyAwareSorter} API.</p>
 */
class SortingUtilsTest {

    @Test
    void buildItemIndex_nullList_throwsNullPointerException() {
        // When / Then
        assertThatThrownBy(() -> SortingUtils.buildItemIndex(null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void resolveGroupMemberIndex_nullMap_throwsNullPointerException() {
        // When / Then
        assertThatThrownBy(() -> SortingUtils.resolveGroupMemberIndex(null, "item"))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void resolveGroupMemberIndex_nullMember_throwsNullPointerException() {
        // Given
        Map<String, Integer> emptyMap = Collections.emptyMap();

        // When / Then
        assertThatThrownBy(() -> SortingUtils.resolveGroupMemberIndex(emptyMap, null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void validateNotAlreadyGrouped_nullMember_throwsNullPointerException() {
        // When / Then
        assertThatThrownBy(() -> SortingUtils.validateNotAlreadyGrouped(SortingUtils.UNASSIGNED, null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void resolveDependencyEdge_nullEdge_throwsNullPointerException() {
        // Given
        Map<String, Integer> emptyMap = Collections.emptyMap();

        // When / Then
        assertThatThrownBy(() -> SortingUtils.resolveDependencyEdge(null, emptyMap))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void resolveDependencyEdge_nullMap_throwsNullPointerException() {
        // Given
        Dependencies.Dependency<String> edge = new Dependencies.Dependency<>("provider", "dependent");

        // When / Then
        assertThatThrownBy(() -> SortingUtils.resolveDependencyEdge(edge, null))
                .isInstanceOf(NullPointerException.class);
    }
}
