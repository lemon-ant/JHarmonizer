package io.github.lemon_ant.jharmonizer.core.sorter.spoon.dependency_graph;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.lemon_ant.jharmonizer.core.testutils.SpoonTestCaseUtils;
import java.net.URL;
import java.util.EnumSet;
import java.util.Set;
import org.junit.jupiter.api.Test;
import spoon.reflect.declaration.CtType;
import spoon.reflect.declaration.CtTypeMember;

class MemberDependencyGraphTest {

    private static final URL DEPENDENCY_GRAPH_FIXTURE_URL = MemberDependencyGraphTest.class.getResource(
            "/test-cases/core/sorter/spoon/dependency-graph/valid/DependencyGraphFixture.java");
    private static final CtType<?> PARSED_FIXTURE_MAIN_TYPE =
            SpoonTestCaseUtils.parseMainTypeFromJavaFixtureResource(DEPENDENCY_GRAPH_FIXTURE_URL);

    private final CtTypeMember deltaFieldMember =
            SpoonTestCaseUtils.requireTypeMemberBySimpleName(PARSED_FIXTURE_MAIN_TYPE.getTypeMembers(), "DELTA");
    private final CtTypeMember charlieFieldMember =
            SpoonTestCaseUtils.requireTypeMemberBySimpleName(PARSED_FIXTURE_MAIN_TYPE.getTypeMembers(), "CHARLIE");
    private final CtTypeMember echoFieldMember =
            SpoonTestCaseUtils.requireTypeMemberBySimpleName(PARSED_FIXTURE_MAIN_TYPE.getTypeMembers(), "ECHO");
    private final CtTypeMember alphaFieldMember =
            SpoonTestCaseUtils.requireTypeMemberBySimpleName(PARSED_FIXTURE_MAIN_TYPE.getTypeMembers(), "ALPHA");
    private final CtTypeMember bravoFieldMember =
            SpoonTestCaseUtils.requireTypeMemberBySimpleName(PARSED_FIXTURE_MAIN_TYPE.getTypeMembers(), "BRAVO");

    @Test
    void findDirectDependents_whenAllowedKindsEmpty_shouldReturnDependentsOfAllKinds() {
        // Given
        MemberDependencyGraph memberDependencyGraph = new MemberDependencyGraph();
        memberDependencyGraph.addEdge(
                alphaFieldMember, bravoFieldMember, MemberDependencyEdgeKind.DECLARATION_DEPENDENCY);
        memberDependencyGraph.addEdge(alphaFieldMember, deltaFieldMember, MemberDependencyEdgeKind.ACCESSOR_BUNDLE);

        // When
        Set<CtTypeMember> directDependents = memberDependencyGraph.findDirectDependents(
                alphaFieldMember, EnumSet.noneOf(MemberDependencyEdgeKind.class));

        // Then
        assertThat(directDependents).containsExactlyInAnyOrder(bravoFieldMember, deltaFieldMember);
    }

    @Test
    void findDirectDependents_whenAllowedKindsRestricted_shouldFilterByEdgeKind() {
        // Given
        MemberDependencyGraph memberDependencyGraph = new MemberDependencyGraph();
        memberDependencyGraph.addEdge(
                alphaFieldMember, bravoFieldMember, MemberDependencyEdgeKind.DECLARATION_DEPENDENCY);
        memberDependencyGraph.addEdge(alphaFieldMember, deltaFieldMember, MemberDependencyEdgeKind.ACCESSOR_BUNDLE);

        // When
        Set<CtTypeMember> directDeclarationDependents = memberDependencyGraph.findDirectDependents(
                alphaFieldMember, EnumSet.of(MemberDependencyEdgeKind.DECLARATION_DEPENDENCY));
        Set<CtTypeMember> directAccessorBundleDependents = memberDependencyGraph.findDirectDependents(
                alphaFieldMember, EnumSet.of(MemberDependencyEdgeKind.ACCESSOR_BUNDLE));

        // Then
        assertThat(directDeclarationDependents).containsExactly(bravoFieldMember);
        assertThat(directAccessorBundleDependents).containsExactly(deltaFieldMember);
    }

    @Test
    void findTransitiveDependents_whenAllowedKindsRestricted_shouldComputeTransitiveClosureForThatKind() {
        // Given
        MemberDependencyGraph memberDependencyGraph = new MemberDependencyGraph();
        memberDependencyGraph.addEdge(
                alphaFieldMember, bravoFieldMember, MemberDependencyEdgeKind.DECLARATION_DEPENDENCY);
        memberDependencyGraph.addEdge(
                bravoFieldMember, charlieFieldMember, MemberDependencyEdgeKind.DECLARATION_DEPENDENCY);
        memberDependencyGraph.addEdge(alphaFieldMember, deltaFieldMember, MemberDependencyEdgeKind.ACCESSOR_BUNDLE);

        // When
        Set<CtTypeMember> declarationDependents = memberDependencyGraph.findTransitiveDependents(
                alphaFieldMember, EnumSet.of(MemberDependencyEdgeKind.DECLARATION_DEPENDENCY));
        Set<CtTypeMember> accessorBundleDependents = memberDependencyGraph.findTransitiveDependents(
                alphaFieldMember, EnumSet.of(MemberDependencyEdgeKind.ACCESSOR_BUNDLE));
        Set<CtTypeMember> allDependents = memberDependencyGraph.findTransitiveDependents(
                alphaFieldMember, EnumSet.noneOf(MemberDependencyEdgeKind.class));

        // Then
        assertThat(declarationDependents).containsExactlyInAnyOrder(bravoFieldMember, charlieFieldMember);
        assertThat(accessorBundleDependents).containsExactly(deltaFieldMember);
        assertThat(allDependents).containsExactlyInAnyOrder(bravoFieldMember, charlieFieldMember, deltaFieldMember);
    }

    @Test
    void findTransitiveDependents_whenCalledTwice_shouldReturnCachedInstance() {
        // Given

        MemberDependencyGraph memberDependencyGraph = new MemberDependencyGraph();
        memberDependencyGraph.addEdge(
                alphaFieldMember, bravoFieldMember, MemberDependencyEdgeKind.DECLARATION_DEPENDENCY);
        memberDependencyGraph.addEdge(
                bravoFieldMember, charlieFieldMember, MemberDependencyEdgeKind.DECLARATION_DEPENDENCY);
        memberDependencyGraph.addEdge(alphaFieldMember, deltaFieldMember, MemberDependencyEdgeKind.ACCESSOR_BUNDLE);

        // When
        Set<CtTypeMember> firstCallResult = memberDependencyGraph.findTransitiveDependents(
                alphaFieldMember, EnumSet.of(MemberDependencyEdgeKind.DECLARATION_DEPENDENCY));
        Set<CtTypeMember> secondCallResult = memberDependencyGraph.findTransitiveDependents(
                alphaFieldMember, EnumSet.of(MemberDependencyEdgeKind.DECLARATION_DEPENDENCY));
        Set<CtTypeMember> differentEdgeKindMaskResult = memberDependencyGraph.findTransitiveDependents(
                alphaFieldMember, EnumSet.noneOf(MemberDependencyEdgeKind.class));

        // Then
        assertThat(secondCallResult).isSameAs(firstCallResult);
        assertThat(firstCallResult).containsExactlyInAnyOrder(bravoFieldMember, charlieFieldMember);
        assertThat(differentEdgeKindMaskResult)
                .isNotSameAs(firstCallResult)
                .containsExactlyInAnyOrder(bravoFieldMember, charlieFieldMember, deltaFieldMember);
    }

    @Test
    void findTransitiveDependents_whenEdgeAdded_shouldInvalidateCachedResults() {
        // Given
        MemberDependencyGraph memberDependencyGraph = new MemberDependencyGraph();
        memberDependencyGraph.addEdge(
                alphaFieldMember, bravoFieldMember, MemberDependencyEdgeKind.DECLARATION_DEPENDENCY);
        memberDependencyGraph.addEdge(
                bravoFieldMember, charlieFieldMember, MemberDependencyEdgeKind.DECLARATION_DEPENDENCY);
        Set<MemberDependencyEdgeKind> allowedEdgeKinds = EnumSet.of(MemberDependencyEdgeKind.DECLARATION_DEPENDENCY);

        // When
        Set<CtTypeMember> firstCallResult =
                memberDependencyGraph.findTransitiveDependents(alphaFieldMember, allowedEdgeKinds);
        memberDependencyGraph.addEdge(
                charlieFieldMember, echoFieldMember, MemberDependencyEdgeKind.DECLARATION_DEPENDENCY);
        Set<CtTypeMember> secondCallResult =
                memberDependencyGraph.findTransitiveDependents(alphaFieldMember, allowedEdgeKinds);
        Set<CtTypeMember> thirdCallResult =
                memberDependencyGraph.findTransitiveDependents(alphaFieldMember, allowedEdgeKinds);

        // Then
        assertThat(secondCallResult)
                .isNotSameAs(firstCallResult)
                .containsExactlyInAnyOrder(bravoFieldMember, charlieFieldMember, echoFieldMember);
        assertThat(thirdCallResult).isSameAs(secondCallResult);
    }

    @Test
    void findTransitiveProviders_whenGraphAcyclic_shouldReturnTransitiveProviders() {
        // Given
        MemberDependencyGraph memberDependencyGraph = new MemberDependencyGraph();
        memberDependencyGraph.addEdge(
                alphaFieldMember, bravoFieldMember, MemberDependencyEdgeKind.DECLARATION_DEPENDENCY);
        memberDependencyGraph.addEdge(
                bravoFieldMember, charlieFieldMember, MemberDependencyEdgeKind.DECLARATION_DEPENDENCY);

        // When
        Set<CtTypeMember> transitiveProviders = memberDependencyGraph.findTransitiveProviders(
                charlieFieldMember, EnumSet.of(MemberDependencyEdgeKind.DECLARATION_DEPENDENCY));

        // Then
        assertThat(transitiveProviders).containsExactlyInAnyOrder(alphaFieldMember, bravoFieldMember);
    }

    @Test
    void findTransitiveDependents_whenResultReturned_shouldBeUnmodifiable() {
        // Given

        MemberDependencyGraph memberDependencyGraph = new MemberDependencyGraph();
        memberDependencyGraph.addEdge(
                alphaFieldMember, bravoFieldMember, MemberDependencyEdgeKind.DECLARATION_DEPENDENCY);

        // When
        Set<CtTypeMember> transitiveDependents = memberDependencyGraph.findTransitiveDependents(alphaFieldMember);

        // Then
        assertThat(transitiveDependents).containsExactly(bravoFieldMember);
        assertThatThrownBy(() -> transitiveDependents.add(alphaFieldMember))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
