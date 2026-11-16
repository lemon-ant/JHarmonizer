// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
package io.github.lemon_ant.jharmonizer.core.sorter.spoon;

import static io.github.lemon_ant.jharmonizer.core.sorter.spoon.SpoonAnnotationSorterTestUtils.sortAnnotationGroups;
import static io.github.lemon_ant.jharmonizer.core.testutils.SpoonTestCaseUtils.parseAstModelFromJavaFixtureResource;
import static io.github.lemon_ant.jharmonizer.core.testutils.TestCaseResourceUtils.requireClasspathResourceUrl;
import static java.util.stream.Collectors.toSet;
import static org.assertj.core.api.Assertions.assertThat;

import io.github.lemon_ant.jharmonizer.core.config.unified.AnnotationDescriptor;
import io.github.lemon_ant.jharmonizer.core.translator.spoon.annotation.AnnotationSrcGroup;
import io.github.lemon_ant.jharmonizer.core.translator.spoon.annotation.AnnotationSrcGroup.AnnotationSrcFragment;
import java.net.URL;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import lombok.NonNull;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RepeatableAnnotationAliasMatrixTest {

    @NonNull
    private static final URL FIXTURE = requireClasspathResourceUrl(
            "/test-cases/core/e2e/reorder/43-repeatable-annotation-alias-matrix/input/RepeatableAnnotationAliasMatrix.java");

    @NonNull
    private List<AnnotationSrcGroup> aliasPairGroups;

    @NonNull
    private List<AnnotationSrcGroup> allSpellingGroups;

    @NonNull
    private List<AnnotationSrcGroup> containerAndLeafGroups;

    @NonNull
    private AnnotationSrcGroup independentHomonymGroup;

    @NonNull
    private Comparator<AnnotationDescriptor> reverseSourceOrderComparator;

    @BeforeAll
    void setUp() {
        // Parse once without modifying Spoon's model. The E2E runner compiles and executes this same fixture.
        List<AnnotationSrcGroup> annotationGroups =
                parseAstModelFromJavaFixtureResource(FIXTURE).getAnnotationSrcGroups();
        aliasPairGroups = annotationGroups.stream()
                .filter(annotationGroup ->
                        annotationGroup.getAnnotationSrcFragments().size() == 2)
                .filter(annotationGroup -> {
                    List<AnnotationSrcFragment> annotationFragments = annotationGroup.getAnnotationSrcFragments();
                    return annotationFragments
                            .getFirst()
                            .getDescriptor()
                            .getName()
                            .equals(annotationFragments
                                    .getLast()
                                    .getDescriptor()
                                    .getName());
                })
                .toList();
        allSpellingGroups = annotationGroups.stream()
                .filter(annotationGroup -> collectTagArguments(annotationGroup)
                        .equals(List.of("\"7\"", "\"6\"", "\"5\"", "\"4\"", "\"3\"", "\"2\"", "\"1\"", "\"0\"")))
                .toList();
        containerAndLeafGroups = annotationGroups.stream()
                .filter(annotationGroup -> {
                    Set<String> annotationNames = annotationGroup.getAnnotationSrcFragments().stream()
                            .map(annotationFragment ->
                                    annotationFragment.getDescriptor().getName())
                            .collect(toSet());
                    return annotationNames.equals(Set.of("Tags", "Tag"))
                            || annotationNames.equals(Set.of("TagGroups", "Tags"))
                            || annotationNames.equals(Set.of("Tags$Kinds", "Tag$Kind"));
                })
                .toList();
        independentHomonymGroup = annotationGroups.stream()
                .filter(annotationGroup -> collectTagArguments(annotationGroup)
                        .equals(List.of("\"primary-z\"", "\"other-z\"", "\"primary-a\"", "\"other-a\"")))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("The matrix fixture has no independent homonym group"));
        reverseSourceOrderComparator = Comparator.comparing(
                        AnnotationDescriptor::getArguments, Comparator.nullsFirst(Comparator.<String>naturalOrder()))
                .thenComparing(AnnotationDescriptor::getName);
    }

    @Test
    void aliasMatrix_allContexts_containsEveryOrderedSpellingPair() {
        assertThat(aliasPairGroups).hasSize(8 * 8 * 3 + 5);
        assertThat(aliasPairGroups)
                .filteredOn(
                        annotationGroup -> collectTagArguments(annotationGroup).equals(List.of("\"z\"", "\"a\"")))
                .hasSize(8 * 8 * 3);
        assertThat(aliasPairGroups)
                .filteredOn(annotationGroup -> annotationGroup
                        .getAnnotationSrcFragments()
                        .getFirst()
                        .getDescriptor()
                        .getName()
                        .equals("Tag$Kind"))
                .hasSize(4);
        assertThat(allSpellingGroups).hasSize(3);
        assertThat(containerAndLeafGroups).hasSize(19);
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("aliasSpellingPairs")
    void orderPreservationGroupKeys_aliasSpellingPair_sharesDependency(
            @NonNull String description, @NonNull AnnotationSrcGroup annotationGroup) {
        List<AnnotationSrcFragment> repeatedAnnotations = annotationGroup.getAnnotationSrcFragments();
        assertThat(repeatedAnnotations).as(description).hasSize(2);
        // Candidate key sets may differ between uses; a shared key is the actual dependency contract.
        assertThat(repeatedAnnotations.getFirst().getDescriptor().getOrderPreservationGroupKeys())
                .as(description)
                .containsAnyElementsOf(
                        repeatedAnnotations.getLast().getDescriptor().getOrderPreservationGroupKeys());
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("allSpellingGroups")
    void orderPreservationGroupKeys_allSpellingsTogether_sharesEveryRepeatedPairDependency(
            @NonNull String description, @NonNull AnnotationSrcGroup annotationGroup) {
        List<AnnotationSrcFragment> repeatedAnnotations = annotationGroup.getAnnotationSrcFragments().stream()
                .filter(annotationFragment ->
                        annotationFragment.getDescriptor().getName().equals("Tag"))
                .toList();
        for (int firstIndex = 0; firstIndex < repeatedAnnotations.size(); firstIndex++) {
            for (int secondIndex = firstIndex + 1; secondIndex < repeatedAnnotations.size(); secondIndex++) {
                assertThat(repeatedAnnotations.get(firstIndex).getDescriptor().getOrderPreservationGroupKeys())
                        .as("%s, pair %s/%s", description, firstIndex, secondIndex)
                        .containsAnyElementsOf(repeatedAnnotations
                                .get(secondIndex)
                                .getDescriptor()
                                .getOrderPreservationGroupKeys());
            }
        }
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("containerAndLeafGroups")
    void orderPreservationGroupKeys_containerAndLeafAliases_sharesDependency(
            @NonNull String description, @NonNull AnnotationSrcGroup annotationGroup) {
        List<AnnotationSrcFragment> annotationFragments = annotationGroup.getAnnotationSrcFragments();
        assertThat(annotationFragments).as(description).hasSize(2);
        assertThat(annotationFragments.getFirst().getDescriptor().getOrderPreservationGroupKeys())
                .as(description)
                .containsAnyElementsOf(
                        annotationFragments.getLast().getDescriptor().getOrderPreservationGroupKeys());
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("aliasSpellingPairs")
    void sort_aliasSpellingPair_preservesRepeatedSourceOrder(
            @NonNull String description, @NonNull AnnotationSrcGroup annotationGroup) {
        // When
        AnnotationSrcGroup sortedGroup = sortAnnotationGroups(List.of(annotationGroup), reverseSourceOrderComparator)
                .getFirst();

        // Then
        assertThat(sortedGroup.getAnnotationSrcFragments())
                .as(description)
                .containsExactlyElementsOf(annotationGroup.getAnnotationSrcFragments());
    }

    @Test
    void sort_confirmedDistinctHomonyms_keepsIndependentFamiliesSortable() {
        // Given
        List<AnnotationSrcFragment> originalAnnotations = independentHomonymGroup.getAnnotationSrcFragments();

        // When
        AnnotationSrcGroup sortedGroup = sortAnnotationGroups(
                        List.of(independentHomonymGroup), reverseSourceOrderComparator)
                .getFirst();

        // Then
        assertThat(originalAnnotations.getFirst().getDescriptor().getOrderPreservationGroupKeys())
                .doesNotContainAnyElementsOf(
                        originalAnnotations.get(1).getDescriptor().getOrderPreservationGroupKeys());
        assertThat(collectTagArguments(sortedGroup))
                .containsExactly("\"other-z\"", "\"other-a\"", "\"primary-z\"", "\"primary-a\"");
    }

    @NonNull
    private static List<String> collectTagArguments(AnnotationSrcGroup annotationGroup) {
        return annotationGroup.getAnnotationSrcFragments().stream()
                .map(AnnotationSrcFragment::getDescriptor)
                .filter(annotationDescriptor -> annotationDescriptor.getName().equals("Tag"))
                .map(AnnotationDescriptor::getArguments)
                .toList();
    }

    @NonNull
    private static Arguments describeGroup(AnnotationSrcGroup annotationGroup) {
        return Arguments.of(
                "offset " + annotationGroup.getStart() + ": " + annotationGroup.getReplacementCode(), annotationGroup);
    }

    @NonNull
    private Stream<Arguments> aliasSpellingPairs() {
        return aliasPairGroups.stream().map(RepeatableAnnotationAliasMatrixTest::describeGroup);
    }

    @NonNull
    private Stream<Arguments> allSpellingGroups() {
        return allSpellingGroups.stream().map(RepeatableAnnotationAliasMatrixTest::describeGroup);
    }

    @NonNull
    private Stream<Arguments> containerAndLeafGroups() {
        return containerAndLeafGroups.stream().map(RepeatableAnnotationAliasMatrixTest::describeGroup);
    }
}
