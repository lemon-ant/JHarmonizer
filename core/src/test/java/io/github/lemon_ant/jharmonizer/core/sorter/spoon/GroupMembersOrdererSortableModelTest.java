package io.github.lemon_ant.jharmonizer.core.sorter.spoon;

import static io.github.lemon_ant.jharmonizer.core.sorter.spoon.SpoonTypeMemberUtils.streamExplicitSrcTypeMembers;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.github.lemon_ant.jharmonizer.core.config.compiled.CompiledMemberGroup;
import io.github.lemon_ant.jharmonizer.core.config.compiled.CompiledMemberGroupTestCreator;
import io.github.lemon_ant.jharmonizer.core.config.compiled.OrderingRule;
import io.github.lemon_ant.jharmonizer.core.sorter.spoon.dependency_graph.MemberDependencyEdgeKind;
import io.github.lemon_ant.jharmonizer.core.sorter.spoon.dependency_graph.MemberDependencyGraph;
import io.github.lemon_ant.jharmonizer.core.testutils.SpoonTestCaseUtils;
import java.net.URL;
import java.util.List;
import java.util.Set;
import lombok.NonNull;
import org.junit.jupiter.api.Test;
import spoon.reflect.declaration.CtType;
import spoon.reflect.declaration.CtTypeMember;

class GroupMembersOrdererSortableModelTest {

    private static final String FIXTURE_CLASSPATH_RESOURCE =
            "/test-cases/core/sorter/spoon/group-ordering-rule/valid/GroupOrderingRuleFixture.java";
    private static final URL FIXTURE_RESOURCE_URL =
            GroupMembersOrdererSortableModelTest.class.getResource(FIXTURE_CLASSPATH_RESOURCE);
    private static final CtType<?> FIXTURE_MAIN_TYPE =
            SpoonTestCaseUtils.parseMainTypeFromJavaFixtureResource(FIXTURE_RESOURCE_URL);
    private static final List<CtTypeMember> FIXTURE_MEMBERS =
            streamExplicitSrcTypeMembers(FIXTURE_MAIN_TYPE).toList();

    @Test
    void convertTypeMembers2SortableTypeMembers_representativeChainPresent_cachedSortableMembersReused() {
        // Given
        CtTypeMember alphaFieldMember = requireFixtureMemberBySimpleName("alphaField");
        CtTypeMember bravoFieldMember = requireFixtureMemberBySimpleName("bravoField");
        CtTypeMember zuluFieldMember = requireFixtureMemberBySimpleName("zuluField");
        CompiledMemberGroup compiledMemberGroup = CompiledMemberGroupTestCreator.createCompiledMemberGroup(
                "alpha-chain", false, List.of(OrderingRule.ALPHA));
        MemberDependencyGraph dependencyGraph = mock(MemberDependencyGraph.class);

        stubDeclarationDependents(dependencyGraph, alphaFieldMember, Set.of());
        stubDeclarationDependents(dependencyGraph, bravoFieldMember, Set.of(alphaFieldMember));
        stubDeclarationDependents(dependencyGraph, zuluFieldMember, Set.of(bravoFieldMember));
        stubAccessorBundleDependents(dependencyGraph, alphaFieldMember, Set.of());
        stubAccessorBundleDependents(dependencyGraph, bravoFieldMember, Set.of());
        stubAccessorBundleDependents(dependencyGraph, zuluFieldMember, Set.of());

        // When
        List<SortableTypeMember> sortableTypeMembers = GroupMembersOrderer.convertTypeMembers2SortableTypeMembers(
                compiledMemberGroup,
                List.of(zuluFieldMember, bravoFieldMember, alphaFieldMember),
                dependencyGraph,
                ComparatorUtils.buildOrderingComparator(compiledMemberGroup.getOrderingRules()));

        SortableTypeMember alphaSortableTypeMember = requireSortableTypeMember(sortableTypeMembers, alphaFieldMember);
        SortableTypeMember bravoSortableTypeMember = requireSortableTypeMember(sortableTypeMembers, bravoFieldMember);
        SortableTypeMember zuluSortableTypeMember = requireSortableTypeMember(sortableTypeMembers, zuluFieldMember);

        // Then
        assertThat(alphaSortableTypeMember.getRepresentativeTypeMember()).isSameAs(alphaSortableTypeMember);
        assertThat(bravoSortableTypeMember.getRepresentativeTypeMember()).isSameAs(alphaSortableTypeMember);
        assertThat(zuluSortableTypeMember.getRepresentativeTypeMember()).isSameAs(bravoSortableTypeMember);
        assertThat(zuluSortableTypeMember.getRepresentativeTypeMember().getRepresentativeTypeMember())
                .isSameAs(alphaSortableTypeMember);
    }

    @Test
    void convertTypeMembers2SortableTypeMembers_accessorBundlePresent_cachedSortableInstanceReused() {
        // Given
        CtTypeMember getValueMethodMember = requireFixtureMemberBySimpleName("getValue");
        CtTypeMember setValueMethodMember = requireFixtureMemberBySimpleName("setValue");
        CtTypeMember middleMethodMember = requireFixtureMemberBySimpleName("middleMethod");
        CompiledMemberGroup compiledMemberGroup = CompiledMemberGroupTestCreator.createCompiledMemberGroup(
                "accessor-cache", true, List.of(OrderingRule.ALPHA));
        MemberDependencyGraph dependencyGraph = mock(MemberDependencyGraph.class);

        stubDeclarationDependents(dependencyGraph, getValueMethodMember, Set.of());
        stubDeclarationDependents(dependencyGraph, setValueMethodMember, Set.of());
        stubDeclarationDependents(dependencyGraph, middleMethodMember, Set.of());
        stubAccessorBundleDependents(dependencyGraph, getValueMethodMember, Set.of(setValueMethodMember));
        stubAccessorBundleDependents(dependencyGraph, setValueMethodMember, Set.of());
        stubAccessorBundleDependents(dependencyGraph, middleMethodMember, Set.of());

        // When
        List<SortableTypeMember> sortableTypeMembers = GroupMembersOrderer.convertTypeMembers2SortableTypeMembers(
                compiledMemberGroup,
                List.of(middleMethodMember, setValueMethodMember, getValueMethodMember),
                dependencyGraph,
                ComparatorUtils.buildOrderingComparator(compiledMemberGroup.getOrderingRules()));

        SortableTypeMember getValueSortableTypeMember =
                requireSortableTypeMember(sortableTypeMembers, getValueMethodMember);
        SortableTypeMember setValueSortableTypeMember =
                requireSortableTypeMember(sortableTypeMembers, setValueMethodMember);
        SortableTypeMember middleSortableTypeMember =
                requireSortableTypeMember(sortableTypeMembers, middleMethodMember);

        // Then
        assertThat(getValueSortableTypeMember.getRepresentativeTypeMember()).isSameAs(getValueSortableTypeMember);
        assertThat(setValueSortableTypeMember.getRepresentativeTypeMember()).isSameAs(getValueSortableTypeMember);
        assertThat(middleSortableTypeMember.getRepresentativeTypeMember()).isSameAs(middleSortableTypeMember);
    }

    @NonNull
    private static SortableTypeMember requireSortableTypeMember(
            List<SortableTypeMember> sortableTypeMembers, CtTypeMember expectedTypeMember) {
        return sortableTypeMembers.stream()
                .filter(sortableTypeMember -> sortableTypeMember.getTypeMember() == expectedTypeMember)
                .findFirst()
                .orElseThrow();
    }

    private static void stubDeclarationDependents(
            MemberDependencyGraph dependencyGraph, CtTypeMember typeMember, Set<CtTypeMember> declarationDependents) {
        when(dependencyGraph.findTransitiveDependents(
                        typeMember, Set.of(MemberDependencyEdgeKind.DECLARATION_DEPENDENCY)))
                .thenReturn(declarationDependents);
    }

    private static void stubAccessorBundleDependents(
            MemberDependencyGraph dependencyGraph,
            CtTypeMember typeMember,
            Set<CtTypeMember> accessorBundleDependents) {
        when(dependencyGraph.findDirectDependents(typeMember, Set.of(MemberDependencyEdgeKind.ACCESSOR_BUNDLE)))
                .thenReturn(accessorBundleDependents);
    }

    @NonNull
    private static CtTypeMember requireFixtureMemberBySimpleName(String expectedSimpleName) {
        return SpoonTestCaseUtils.requireTypeMemberBySimpleName(FIXTURE_MEMBERS, expectedSimpleName);
    }
}
