/*
 * SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.lemon_ant.jharmonizer.sorting;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

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
        // When
        Throwable thrown = catchThrowable(() -> SortingUtils.buildItemIndex(null));

        // Then
        assertThat(thrown).isInstanceOf(NullPointerException.class);
    }

    @Test
    void resolveGroupMemberIndex_nullMap_throwsNullPointerException() {
        // When
        Throwable thrown = catchThrowable(() -> SortingUtils.resolveGroupMemberIndex(null, "item"));

        // Then
        assertThat(thrown).isInstanceOf(NullPointerException.class);
    }

    @Test
    void resolveGroupMemberIndex_nullMember_throwsNullPointerException() {
        // Given
        Map<String, Integer> emptyMap = Collections.emptyMap();

        // When
        Throwable thrown = catchThrowable(() -> SortingUtils.resolveGroupMemberIndex(emptyMap, null));

        // Then
        assertThat(thrown).isInstanceOf(NullPointerException.class);
    }

    @Test
    void validateNotAlreadyGrouped_nullMember_throwsNullPointerException() {
        // When
        Throwable thrown = catchThrowable(() -> SortingUtils.validateNotAlreadyGrouped(SortingUtils.UNASSIGNED, null));

        // Then
        assertThat(thrown).isInstanceOf(NullPointerException.class);
    }

    @Test
    void resolveDependencyEdge_nullEdge_throwsNullPointerException() {
        // Given
        Map<String, Integer> emptyMap = Collections.emptyMap();

        // When
        Throwable thrown = catchThrowable(() -> SortingUtils.resolveDependencyEdge(null, emptyMap));

        // Then
        assertThat(thrown).isInstanceOf(NullPointerException.class);
    }

    @Test
    void resolveDependencyEdge_nullMap_throwsNullPointerException() {
        // Given
        Dependencies.Dependency<String> edge = new Dependencies.Dependency<>("provider", "dependent");

        // When
        Throwable thrown = catchThrowable(() -> SortingUtils.resolveDependencyEdge(edge, null));

        // Then
        assertThat(thrown).isInstanceOf(NullPointerException.class);
    }
}
