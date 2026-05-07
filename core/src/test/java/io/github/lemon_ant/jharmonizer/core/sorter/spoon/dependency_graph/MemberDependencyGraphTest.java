// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
package io.github.lemon_ant.jharmonizer.core.sorter.spoon.dependency_graph;

// @jharmonizer:fully-off
// jharmonizer v1.0.1 incorrectly reorders dependent static fields in test classes;
// remove this directive once jharmonizer is upgraded to a version that respects field initialization order.
import static io.github.lemon_ant.jharmonizer.core.testutils.TestCaseResourceUtils.TEST_CASES_DIR;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.lemon_ant.jharmonizer.core.testutils.SpoonTestCaseUtils;
import io.github.lemon_ant.jharmonizer.core.testutils.TestCaseResourceUtils;
import java.net.URL;
import java.util.EnumSet;
import java.util.Set;
import org.junit.jupiter.api.Test;
import spoon.reflect.declaration.CtType;
import spoon.reflect.declaration.CtTypeMember;

class MemberDependencyGraphTest {

    private static final URL DEPENDENCY_GRAPH_FIXTURE_URL = TestCaseResourceUtils.requireClasspathResourceUrl(
            "/" + TEST_CASES_DIR + "/core/sorter/spoon/dependency-graph/valid/DependencyGraphFixture.java");
    private static final CtType<?> PARSED_FIXTURE_MAIN_TYPE =
            SpoonTestCaseUtils.parseMainTypeFromJavaFixtureResource(DEPENDENCY_GRAPH_FIXTURE_URL);
    private static final CtTypeMember ALPHA_FIELD_MEMBER =
            SpoonTestCaseUtils.requireTypeMemberBySimpleName(PARSED_FIXTURE_MAIN_TYPE.getTypeMembers(), "ALPHA");
    private static final CtTypeMember BRAVO_FIELD_MEMBER =
            SpoonTestCaseUtils.requireTypeMemberBySimpleName(PARSED_FIXTURE_MAIN_TYPE.getTypeMembers(), "BRAVO");
    private static final CtTypeMember CHARLIE_FIELD_MEMBER =
            SpoonTestCaseUtils.requireTypeMemberBySimpleName(PARSED_FIXTURE_MAIN_TYPE.getTypeMembers(), "CHARLIE");
    private static final CtTypeMember DELTA_FIELD_MEMBER =
            SpoonTestCaseUtils.requireTypeMemberBySimpleName(PARSED_FIXTURE_MAIN_TYPE.getTypeMembers(), "DELTA");
    private static final CtTypeMember ECHO_FIELD_MEMBER =
            SpoonTestCaseUtils.requireTypeMemberBySimpleName(PARSED_FIXTURE_MAIN_TYPE.getTypeMembers(), "ECHO");

    @Test
    void findDirectDependents_allowedKindsEmpty_dependentsOfAllKindsReturned() {
        // Given
        MemberDependencyGraph memberDependencyGraph = new MemberDependencyGraph();
        memberDependencyGraph.addEdge(
                ALPHA_FIELD_MEMBER, BRAVO_FIELD_MEMBER, MemberDependencyEdgeKind.DECLARATION_DEPENDENCY);
        memberDependencyGraph.addEdge(ALPHA_FIELD_MEMBER, DELTA_FIELD_MEMBER, MemberDependencyEdgeKind.ACCESSOR_BUNDLE);

        // When
        Set<CtTypeMember> directDependents = memberDependencyGraph.findDirectDependents(
                ALPHA_FIELD_MEMBER, EnumSet.noneOf(MemberDependencyEdgeKind.class));

        // Then
        assertThat(directDependents).containsExactlyInAnyOrder(BRAVO_FIELD_MEMBER, DELTA_FIELD_MEMBER);
    }

    @Test
    void findDirectDependents_allowedKindsRestricted_dependentsFilteredByEdgeKind() {
        // Given
        MemberDependencyGraph memberDependencyGraph = new MemberDependencyGraph();
        memberDependencyGraph.addEdge(
                ALPHA_FIELD_MEMBER, BRAVO_FIELD_MEMBER, MemberDependencyEdgeKind.DECLARATION_DEPENDENCY);
        memberDependencyGraph.addEdge(ALPHA_FIELD_MEMBER, DELTA_FIELD_MEMBER, MemberDependencyEdgeKind.ACCESSOR_BUNDLE);

        // When
        Set<CtTypeMember> directDeclarationDependents = memberDependencyGraph.findDirectDependents(
                ALPHA_FIELD_MEMBER, EnumSet.of(MemberDependencyEdgeKind.DECLARATION_DEPENDENCY));
        Set<CtTypeMember> directAccessorBundleDependents = memberDependencyGraph.findDirectDependents(
                ALPHA_FIELD_MEMBER, EnumSet.of(MemberDependencyEdgeKind.ACCESSOR_BUNDLE));

        // Then
        assertThat(directDeclarationDependents).containsExactly(BRAVO_FIELD_MEMBER);
        assertThat(directAccessorBundleDependents).containsExactly(DELTA_FIELD_MEMBER);
    }

    @Test
    void findTransitiveDependents_allowedKindsRestricted_transitiveClosureComputedForThatKind() {
        // Given
        MemberDependencyGraph memberDependencyGraph = new MemberDependencyGraph();
        memberDependencyGraph.addEdge(
                ALPHA_FIELD_MEMBER, BRAVO_FIELD_MEMBER, MemberDependencyEdgeKind.DECLARATION_DEPENDENCY);
        memberDependencyGraph.addEdge(
                BRAVO_FIELD_MEMBER, CHARLIE_FIELD_MEMBER, MemberDependencyEdgeKind.DECLARATION_DEPENDENCY);
        memberDependencyGraph.addEdge(ALPHA_FIELD_MEMBER, DELTA_FIELD_MEMBER, MemberDependencyEdgeKind.ACCESSOR_BUNDLE);

        // When
        Set<CtTypeMember> declarationDependents = memberDependencyGraph.findTransitiveDependents(
                ALPHA_FIELD_MEMBER, EnumSet.of(MemberDependencyEdgeKind.DECLARATION_DEPENDENCY));
        Set<CtTypeMember> accessorBundleDependents = memberDependencyGraph.findTransitiveDependents(
                ALPHA_FIELD_MEMBER, EnumSet.of(MemberDependencyEdgeKind.ACCESSOR_BUNDLE));
        Set<CtTypeMember> allDependents = memberDependencyGraph.findTransitiveDependents(
                ALPHA_FIELD_MEMBER, EnumSet.noneOf(MemberDependencyEdgeKind.class));

        // Then
        assertThat(declarationDependents).containsExactlyInAnyOrder(BRAVO_FIELD_MEMBER, CHARLIE_FIELD_MEMBER);
        assertThat(accessorBundleDependents).containsExactly(DELTA_FIELD_MEMBER);
        assertThat(allDependents)
                .containsExactlyInAnyOrder(BRAVO_FIELD_MEMBER, CHARLIE_FIELD_MEMBER, DELTA_FIELD_MEMBER);
    }

    @Test
    void findTransitiveDependents_calledTwice_cachedInstanceReturned() {
        // Given
        MemberDependencyGraph memberDependencyGraph = new MemberDependencyGraph();
        memberDependencyGraph.addEdge(
                ALPHA_FIELD_MEMBER, BRAVO_FIELD_MEMBER, MemberDependencyEdgeKind.DECLARATION_DEPENDENCY);
        memberDependencyGraph.addEdge(
                BRAVO_FIELD_MEMBER, CHARLIE_FIELD_MEMBER, MemberDependencyEdgeKind.DECLARATION_DEPENDENCY);
        memberDependencyGraph.addEdge(ALPHA_FIELD_MEMBER, DELTA_FIELD_MEMBER, MemberDependencyEdgeKind.ACCESSOR_BUNDLE);

        // When
        Set<CtTypeMember> firstCallResult = memberDependencyGraph.findTransitiveDependents(
                ALPHA_FIELD_MEMBER, EnumSet.of(MemberDependencyEdgeKind.DECLARATION_DEPENDENCY));
        Set<CtTypeMember> secondCallResult = memberDependencyGraph.findTransitiveDependents(
                ALPHA_FIELD_MEMBER, EnumSet.of(MemberDependencyEdgeKind.DECLARATION_DEPENDENCY));
        Set<CtTypeMember> differentEdgeKindMaskResult = memberDependencyGraph.findTransitiveDependents(
                ALPHA_FIELD_MEMBER, EnumSet.noneOf(MemberDependencyEdgeKind.class));

        // Then
        assertThat(secondCallResult).isSameAs(firstCallResult);
        assertThat(firstCallResult).containsExactlyInAnyOrder(BRAVO_FIELD_MEMBER, CHARLIE_FIELD_MEMBER);
        assertThat(differentEdgeKindMaskResult)
                .isNotSameAs(firstCallResult)
                .containsExactlyInAnyOrder(BRAVO_FIELD_MEMBER, CHARLIE_FIELD_MEMBER, DELTA_FIELD_MEMBER);
    }

    @Test
    void findTransitiveDependents_edgeAdded_cachedResultsInvalidated() {
        // Given
        MemberDependencyGraph memberDependencyGraph = new MemberDependencyGraph();
        memberDependencyGraph.addEdge(
                ALPHA_FIELD_MEMBER, BRAVO_FIELD_MEMBER, MemberDependencyEdgeKind.DECLARATION_DEPENDENCY);
        memberDependencyGraph.addEdge(
                BRAVO_FIELD_MEMBER, CHARLIE_FIELD_MEMBER, MemberDependencyEdgeKind.DECLARATION_DEPENDENCY);
        Set<MemberDependencyEdgeKind> declarationEdgeKinds =
                EnumSet.of(MemberDependencyEdgeKind.DECLARATION_DEPENDENCY);

        // When
        Set<CtTypeMember> firstCallResult =
                memberDependencyGraph.findTransitiveDependents(ALPHA_FIELD_MEMBER, declarationEdgeKinds);
        memberDependencyGraph.addEdge(
                CHARLIE_FIELD_MEMBER, ECHO_FIELD_MEMBER, MemberDependencyEdgeKind.DECLARATION_DEPENDENCY);
        Set<CtTypeMember> secondCallResult =
                memberDependencyGraph.findTransitiveDependents(ALPHA_FIELD_MEMBER, declarationEdgeKinds);
        Set<CtTypeMember> thirdCallResult =
                memberDependencyGraph.findTransitiveDependents(ALPHA_FIELD_MEMBER, declarationEdgeKinds);

        // Then
        assertThat(secondCallResult)
                .isNotSameAs(firstCallResult)
                .containsExactlyInAnyOrder(BRAVO_FIELD_MEMBER, CHARLIE_FIELD_MEMBER, ECHO_FIELD_MEMBER);
        assertThat(thirdCallResult).isSameAs(secondCallResult);
    }

    @Test
    void findTransitiveProviders_graphAcyclic_transitiveProvidersReturned() {
        // Given
        MemberDependencyGraph memberDependencyGraph = new MemberDependencyGraph();
        memberDependencyGraph.addEdge(
                ALPHA_FIELD_MEMBER, BRAVO_FIELD_MEMBER, MemberDependencyEdgeKind.DECLARATION_DEPENDENCY);
        memberDependencyGraph.addEdge(
                BRAVO_FIELD_MEMBER, CHARLIE_FIELD_MEMBER, MemberDependencyEdgeKind.DECLARATION_DEPENDENCY);

        // When
        Set<CtTypeMember> transitiveProviders = memberDependencyGraph.findTransitiveProviders(
                CHARLIE_FIELD_MEMBER, EnumSet.of(MemberDependencyEdgeKind.DECLARATION_DEPENDENCY));

        // Then
        assertThat(transitiveProviders).containsExactlyInAnyOrder(ALPHA_FIELD_MEMBER, BRAVO_FIELD_MEMBER);
    }

    @Test
    void findTransitiveDependents_resultReturned_resultUnmodifiable() {
        // Given
        MemberDependencyGraph memberDependencyGraph = new MemberDependencyGraph();
        memberDependencyGraph.addEdge(
                ALPHA_FIELD_MEMBER, BRAVO_FIELD_MEMBER, MemberDependencyEdgeKind.DECLARATION_DEPENDENCY);

        // When
        Set<CtTypeMember> transitiveDependents = memberDependencyGraph.findTransitiveDependents(ALPHA_FIELD_MEMBER);

        // Then
        assertThat(transitiveDependents).containsExactly(BRAVO_FIELD_MEMBER);
        assertThatThrownBy(() -> transitiveDependents.add(ALPHA_FIELD_MEMBER))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
