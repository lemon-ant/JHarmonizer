package io.github.lemon_ant.jharmonizer.core.sorter.spoon;

import static io.github.lemon_ant.jharmonizer.core.sorter.spoon.SpoonTypeMemberUtils.streamExplicitSourceTypeMembers;
import static io.github.lemon_ant.jharmonizer.core.testutils.TestCaseResourceUtils.requireClasspathResourceUrl;
import static java.util.stream.Collectors.toUnmodifiableMap;
import static org.assertj.core.api.Assertions.assertThat;

import io.github.lemon_ant.jharmonizer.core.config.compiled.CompiledMemberGroup;
import io.github.lemon_ant.jharmonizer.core.config.compiled.CompiledMemberGroupTestCreator;
import io.github.lemon_ant.jharmonizer.core.config.compiled.SortKey;
import io.github.lemon_ant.jharmonizer.core.sorter.spoon.dependency_graph.MemberDependencyGraph;
import io.github.lemon_ant.jharmonizer.core.sorter.spoon.dependency_graph.MemberDependencyGraphBuilder;
import io.github.lemon_ant.jharmonizer.core.testutils.SpoonTestCaseUtils;
import java.net.URL;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import spoon.reflect.declaration.CtType;
import spoon.reflect.declaration.CtTypeMember;

class GroupMembersOrdererComplexDependenciesTest {

    private static final URL COMPLEX_FIXTURE_CLASSPATH_RESOURCE_PATH = requireClasspathResourceUrl(
            "/test-cases/core/sorter/spoon/group-member-ordering/valid/GroupMemberOrderingComplexFixture.java");

    @Test
    void orderMembersInsideGroups_dependencyGraphOverridesAlpha_expectedDependencyOrder() {
        // Given
        CtType<?> mainType =
                SpoonTestCaseUtils.parseMainTypeFromJavaFixtureResource(COMPLEX_FIXTURE_CLASSPATH_RESOURCE_PATH);
        CompiledMemberGroup compiledMemberGroup =
                CompiledMemberGroupTestCreator.createCompiledMemberGroup("complex", true, List.of(SortKey.ALPHA));
        Map<CtTypeMember, CompiledMemberGroup> memberToNaturalGroup = streamExplicitSourceTypeMembers(mainType)
                .collect(toUnmodifiableMap(member -> member, member -> compiledMemberGroup));
        MemberDependencyGraph memberDependencyGraph =
                MemberDependencyGraphBuilder.buildDependencyGraph(memberToNaturalGroup);
        List<MemberGroupBlock> memberGroupBlocks =
                TypeMemberGrouper.groupMembersByEffectiveGroups(memberToNaturalGroup);

        // When
        List<MemberGroupBlock> orderedMembers =
                GroupMembersOrderer.orderMembersInsideGroups(memberGroupBlocks, memberDependencyGraph);

        // Then
        assertThat(orderedMembers.getFirst().getTypeMembers().stream()
                        .map(SpoonTypeMemberUtils::deriveAlphaKey)
                        .toList())
                .containsExactly(
                        "w_provider:int",
                        "x_provider:int",
                        "y_provider:int",
                        "z_blank_final:int",
                        "z_provider:int",
                        "<init>",
                        "a_dependent:int",
                        "a0_dependent2:int",
                        "enabledFlag:boolean",
                        "getEnabledFlag():java.lang.Boolean",
                        "hasEnabledFlag():boolean",
                        "isEnabledFlag():boolean",
                        "setEnabledFlag(boolean):void");
    }
}
