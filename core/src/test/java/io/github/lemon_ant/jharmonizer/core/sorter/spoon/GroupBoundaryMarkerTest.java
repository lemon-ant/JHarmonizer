// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
package io.github.lemon_ant.jharmonizer.core.sorter.spoon;

import static io.github.lemon_ant.jharmonizer.core.config.compiled.CompiledMemberGroupTestCreator.createTrivialMemberGroup;
import static io.github.lemon_ant.jharmonizer.core.spoon.SpoonGroupSeparatorUtils.resolveSeparator;
import static io.github.lemon_ant.jharmonizer.core.testutils.TestCaseResourceUtils.TEST_CASES_DIR;
import static org.assertj.core.api.Assertions.assertThat;

import io.github.lemon_ant.jharmonizer.core.config.unified.UnifiedSeparator;
import io.github.lemon_ant.jharmonizer.core.spoon.SpoonGroupSeparatorUtils.GroupSeparator;
import io.github.lemon_ant.jharmonizer.core.testutils.SpoonTestCaseUtils;
import java.net.URL;
import java.util.List;
import lombok.NonNull;
import org.junit.jupiter.api.Test;
import spoon.reflect.declaration.CtType;
import spoon.reflect.declaration.CtTypeMember;

class GroupBoundaryMarkerTest {
    private static final URL FIXTURE_URL = GroupBoundaryMarkerTest.class.getResource(
            "/" + TEST_CASES_DIR + "/core/sorter/spoon/type-member-grouper/valid/TypeMemberGrouperFixture.java");

    @Test
    void markGroupBoundaries_groupIsEmpty_skipSeparatorMetadataEmission() {
        // Given
        CtType<?> parsedMainType = SpoonTestCaseUtils.parseMainTypeFromJavaFixtureResource(FIXTURE_URL);
        CtTypeMember alphaFieldMember =
                SpoonTestCaseUtils.requireTypeMemberBySimpleName(parsedMainType.getTypeMembers(), "alpha");
        List<MemberGroupBlock> orderedBlocks = List.of(
                createGroupBlock("Empty header", UnifiedSeparator.HEADER, List.of()),
                createGroupBlock("No separator member", UnifiedSeparator.NONE, List.of(alphaFieldMember)));

        // When
        GroupBoundaryMarker.markGroupBoundaries(orderedBlocks);

        // Then
        assertThat(resolveSeparator(alphaFieldMember))
                .extracting(GroupSeparator::getType, GroupSeparator::getHeaderText)
                .containsExactly(UnifiedSeparator.NONE, null);
    }

    @Test
    void markGroupBoundaries_groupsContainMembers_writeMetadataOnlyToFirstMemberOfEachNonEmptyGroup() {
        // Given
        CtType<?> parsedMainType = SpoonTestCaseUtils.parseMainTypeFromJavaFixtureResource(FIXTURE_URL);
        CtTypeMember alphaFieldMember =
                SpoonTestCaseUtils.requireTypeMemberBySimpleName(parsedMainType.getTypeMembers(), "alpha");
        CtTypeMember bravoFieldMember =
                SpoonTestCaseUtils.requireTypeMemberBySimpleName(parsedMainType.getTypeMembers(), "bravo");
        CtTypeMember charlieMethodMember =
                SpoonTestCaseUtils.requireTypeMemberBySimpleName(parsedMainType.getTypeMembers(), "charlie");
        CtTypeMember deltaMethodMember =
                SpoonTestCaseUtils.requireTypeMemberBySimpleName(parsedMainType.getTypeMembers(), "delta");
        List<MemberGroupBlock> orderedBlocks = List.of(
                createGroupBlock("Header fields", UnifiedSeparator.HEADER, List.of(alphaFieldMember, bravoFieldMember)),
                createGroupBlock("Methods", UnifiedSeparator.NEW_LINE, List.of(charlieMethodMember, deltaMethodMember)),
                createGroupBlock("No separator", UnifiedSeparator.NONE, List.of()));

        // When
        GroupBoundaryMarker.markGroupBoundaries(orderedBlocks);

        // Then
        assertThat(resolveSeparator(alphaFieldMember))
                .extracting(GroupSeparator::getType, GroupSeparator::getHeaderText)
                .containsExactly(UnifiedSeparator.HEADER, "Header fields");
        assertThat(resolveSeparator(bravoFieldMember))
                .extracting(GroupSeparator::getType, GroupSeparator::getHeaderText)
                .containsExactly(UnifiedSeparator.NONE, null);
        assertThat(resolveSeparator(charlieMethodMember))
                .extracting(GroupSeparator::getType, GroupSeparator::getHeaderText)
                .containsExactly(UnifiedSeparator.NEW_LINE, null);
        assertThat(resolveSeparator(deltaMethodMember))
                .extracting(GroupSeparator::getType, GroupSeparator::getHeaderText)
                .containsExactly(UnifiedSeparator.NONE, null);
    }

    @NonNull
    private static MemberGroupBlock createGroupBlock(
            String groupName, UnifiedSeparator separator, List<CtTypeMember> members) {
        return new MemberGroupBlock(createTrivialMemberGroup(groupName, false, 0, separator), members);
    }
}
