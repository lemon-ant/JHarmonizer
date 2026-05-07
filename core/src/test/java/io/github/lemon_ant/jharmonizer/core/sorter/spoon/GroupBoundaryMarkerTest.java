// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
package io.github.lemon_ant.jharmonizer.core.sorter.spoon;

// @jharmonizer:fully-off
// jharmonizer v1.0.1 incorrectly reorders dependent static fields in test classes;
// remove this directive once jharmonizer is upgraded to a version that respects field initialization order.
import static io.github.lemon_ant.jharmonizer.core.config.compiled.CompiledMemberGroupTestCreator.createTrivialMemberGroup;
import static io.github.lemon_ant.jharmonizer.core.testutils.TestCaseResourceUtils.TEST_CASES_DIR;
import static org.assertj.core.api.Assertions.assertThat;

import io.github.lemon_ant.jharmonizer.core.config.unified.UnifiedSeparator;
import io.github.lemon_ant.jharmonizer.core.testutils.SpoonTestCaseUtils;
import io.github.lemon_ant.jharmonizer.core.translator.spoon.SpoonSrcPrinterUtils;
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
        assertThat(alphaFieldMember.getMetadata(SpoonSrcPrinterUtils.GROUP_HEADER_METADATA))
                .isEqualTo("Header fields");
        assertThat(bravoFieldMember.getMetadata(SpoonSrcPrinterUtils.GROUP_HEADER_METADATA))
                .isNull();
        assertThat(charlieMethodMember.getMetadata(SpoonSrcPrinterUtils.GROUP_HEADER_METADATA))
                .isEqualTo(SpoonSrcPrinterUtils.GROUP_SEPARATOR_NEW_LINE);
        assertThat(deltaMethodMember.getMetadata(SpoonSrcPrinterUtils.GROUP_HEADER_METADATA))
                .isNull();
    }

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
        assertThat(alphaFieldMember.getMetadata(SpoonSrcPrinterUtils.GROUP_HEADER_METADATA))
                .isNull();
    }

    @NonNull
    private static MemberGroupBlock createGroupBlock(
            String groupName, UnifiedSeparator separator, List<CtTypeMember> members) {
        return new MemberGroupBlock(createTrivialMemberGroup(groupName, false, 0, separator), members);
    }
}
