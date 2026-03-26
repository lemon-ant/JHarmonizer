package io.github.lemon_ant.jharmonizer.core.sorter.spoon;

import static io.github.lemon_ant.jharmonizer.core.config.compiled.CompiledMemberGroupTestCreator.createTrivialMemberGroup;
import static org.assertj.core.api.Assertions.assertThat;

import io.github.lemon_ant.jharmonizer.core.config.unified.UnifiedSeparator;
import io.github.lemon_ant.jharmonizer.core.testutils.SpoonTestCaseUtils;
import io.github.lemon_ant.jharmonizer.core.translator.spoon.SpoonSourcePrinterUtils;
import java.net.URL;
import java.util.List;
import lombok.NonNull;
import org.junit.jupiter.api.Test;
import spoon.reflect.declaration.CtType;
import spoon.reflect.declaration.CtTypeMember;

class GroupBoundaryMarkerTest {

    private static final URL FIXTURE_URL = GroupBoundaryMarkerTest.class.getResource(
            "/test-cases/core/sorter/spoon/type-member-grouper/valid/TypeMemberGrouperFixture.java");
    private static final CtType<?> PARSED_MAIN_TYPE =
            SpoonTestCaseUtils.parseMainTypeFromJavaFixtureResource(FIXTURE_URL);

    @Test
    void markGroupBoundaries_groupsContainMembers_writeMetadataOnlyToFirstMemberOfEachNonEmptyGroup() {
        // Given
        CtTypeMember alphaFieldMember =
                SpoonTestCaseUtils.requireTypeMemberBySimpleName(PARSED_MAIN_TYPE.getTypeMembers(), "alpha");
        CtTypeMember bravoFieldMember =
                SpoonTestCaseUtils.requireTypeMemberBySimpleName(PARSED_MAIN_TYPE.getTypeMembers(), "bravo");
        CtTypeMember charlieMethodMember =
                SpoonTestCaseUtils.requireTypeMemberBySimpleName(PARSED_MAIN_TYPE.getTypeMembers(), "charlie");
        CtTypeMember deltaMethodMember =
                SpoonTestCaseUtils.requireTypeMemberBySimpleName(PARSED_MAIN_TYPE.getTypeMembers(), "delta");
        List<MemberGroupBlock> orderedBlocks = List.of(
                createGroupBlock("Header fields", UnifiedSeparator.HEADER, List.of(alphaFieldMember, bravoFieldMember)),
                createGroupBlock("Methods", UnifiedSeparator.NEW_LINE, List.of(charlieMethodMember, deltaMethodMember)),
                createGroupBlock("No separator", UnifiedSeparator.NONE, List.of()));

        // When
        GroupBoundaryMarker.markGroupBoundaries(orderedBlocks);

        // Then
        assertThat(alphaFieldMember.getMetadata(SpoonSourcePrinterUtils.GROUP_HEADER_METADATA))
                .isEqualTo("Header fields");
        assertThat(bravoFieldMember.getMetadata(SpoonSourcePrinterUtils.GROUP_HEADER_METADATA))
                .isNull();
        assertThat(charlieMethodMember.getMetadata(SpoonSourcePrinterUtils.GROUP_HEADER_METADATA))
                .isEqualTo("");
        assertThat(deltaMethodMember.getMetadata(SpoonSourcePrinterUtils.GROUP_HEADER_METADATA))
                .isNull();
    }

    @Test
    void markGroupBoundaries_groupIsEmpty_skipSeparatorMetadataEmission() {
        // Given
        CtTypeMember alphaFieldMember =
                SpoonTestCaseUtils.requireTypeMemberBySimpleName(PARSED_MAIN_TYPE.getTypeMembers(), "alpha");
        alphaFieldMember.putMetadata(SpoonSourcePrinterUtils.GROUP_HEADER_METADATA, null);
        List<MemberGroupBlock> orderedBlocks =
                List.of(createGroupBlock("Empty header", UnifiedSeparator.HEADER, List.of()));

        // When
        GroupBoundaryMarker.markGroupBoundaries(orderedBlocks);

        // Then
        assertThat(alphaFieldMember.getMetadata(SpoonSourcePrinterUtils.GROUP_HEADER_METADATA))
                .isNull();
    }

    @NonNull
    private static MemberGroupBlock createGroupBlock(
            String groupName, UnifiedSeparator separator, List<CtTypeMember> members) {
        return new MemberGroupBlock(createTrivialMemberGroup(groupName, false, 0, separator), members);
    }
}
