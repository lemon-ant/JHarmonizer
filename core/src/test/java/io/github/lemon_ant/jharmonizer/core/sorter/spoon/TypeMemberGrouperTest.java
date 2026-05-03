/*
 * SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.lemon_ant.jharmonizer.core.sorter.spoon;

import static io.github.lemon_ant.jharmonizer.core.config.compiled.CompiledMemberGroupTestCreator.createTrivialMemberGroup;
import static io.github.lemon_ant.jharmonizer.core.testutils.TestCaseResourceUtils.TEST_CASES_DIR;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.lemon_ant.jharmonizer.core.config.compiled.CompiledMemberGroup;
import io.github.lemon_ant.jharmonizer.core.testutils.SpoonTestCaseUtils;
import java.net.URL;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import spoon.reflect.declaration.CtType;
import spoon.reflect.declaration.CtTypeMember;

class TypeMemberGrouperTest {

    private static final URL FIXTURE_URL = TypeMemberGrouperTest.class.getResource(
            "/" + TEST_CASES_DIR + "/core/sorter/spoon/type-member-grouper/valid/TypeMemberGrouperFixture.java");
    private static final CtType<?> PARSED_MAIN_TYPE =
            SpoonTestCaseUtils.parseMainTypeFromJavaFixtureResource(FIXTURE_URL);

    @Test
    void groupMembersByEffectiveGroups_memberGroupsHaveDifferentOrderIndex_sortBlocksByOrderIndex() {
        // Given
        CtTypeMember alphaFieldMember =
                SpoonTestCaseUtils.requireTypeMemberBySimpleName(PARSED_MAIN_TYPE.getTypeMembers(), "alpha");
        CtTypeMember bravoFieldMember =
                SpoonTestCaseUtils.requireTypeMemberBySimpleName(PARSED_MAIN_TYPE.getTypeMembers(), "bravo");
        CtTypeMember charlieMethodMember =
                SpoonTestCaseUtils.requireTypeMemberBySimpleName(PARSED_MAIN_TYPE.getTypeMembers(), "charlie");
        CtTypeMember deltaMethodMember =
                SpoonTestCaseUtils.requireTypeMemberBySimpleName(PARSED_MAIN_TYPE.getTypeMembers(), "delta");
        CompiledMemberGroup earlyGroup = createTrivialMemberGroup("Early group", false, 10);
        CompiledMemberGroup lateGroup = createTrivialMemberGroup("Late group", false, 20);
        Map<CtTypeMember, CompiledMemberGroup> member2effectiveGroup = new LinkedHashMap<>();
        member2effectiveGroup.put(charlieMethodMember, lateGroup);
        member2effectiveGroup.put(alphaFieldMember, earlyGroup);
        member2effectiveGroup.put(deltaMethodMember, lateGroup);
        member2effectiveGroup.put(bravoFieldMember, earlyGroup);

        // When
        List<MemberGroupBlock> groupedMemberBlocks =
                TypeMemberGrouper.groupMembersByEffectiveGroups(member2effectiveGroup);

        // Then
        assertThat(groupedMemberBlocks).hasSize(2);
        assertThat(groupedMemberBlocks.getFirst().getCompiledMemberGroup()).isEqualTo(earlyGroup);
        assertThat(groupedMemberBlocks.getFirst().getTypeMembers()).containsExactly(alphaFieldMember, bravoFieldMember);
        assertThat(groupedMemberBlocks.getLast().getCompiledMemberGroup()).isEqualTo(lateGroup);
        assertThat(groupedMemberBlocks.getLast().getTypeMembers())
                .containsExactly(charlieMethodMember, deltaMethodMember);
    }

    @Test
    void groupMembersByEffectiveGroups_invoked_returnUnmodifiableCollections() {
        // Given
        CtTypeMember alphaFieldMember =
                SpoonTestCaseUtils.requireTypeMemberBySimpleName(PARSED_MAIN_TYPE.getTypeMembers(), "alpha");
        CtTypeMember bravoFieldMember =
                SpoonTestCaseUtils.requireTypeMemberBySimpleName(PARSED_MAIN_TYPE.getTypeMembers(), "bravo");
        CompiledMemberGroup singleGroup = createTrivialMemberGroup("Single group", false, 10);
        Map<CtTypeMember, CompiledMemberGroup> member2effectiveGroup = new LinkedHashMap<>();
        member2effectiveGroup.put(alphaFieldMember, singleGroup);
        member2effectiveGroup.put(bravoFieldMember, singleGroup);

        // When
        List<MemberGroupBlock> groupedMemberBlocks =
                TypeMemberGrouper.groupMembersByEffectiveGroups(member2effectiveGroup);

        // Then
        assertThat(groupedMemberBlocks).hasSize(1);
        assertThatThrownBy(() -> groupedMemberBlocks.add(groupedMemberBlocks.getFirst()))
                .isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> groupedMemberBlocks.getFirst().getTypeMembers().add(alphaFieldMember))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
