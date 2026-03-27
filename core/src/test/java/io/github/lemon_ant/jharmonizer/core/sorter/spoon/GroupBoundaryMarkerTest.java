package io.github.lemon_ant.jharmonizer.core.sorter.spoon;

import static io.github.lemon_ant.jharmonizer.core.config.compiled.CompiledMemberGroupTestCreator.createTrivialMemberGroup;
import static io.github.lemon_ant.jharmonizer.core.files_handler.SrcFileCreator.createSrcFile;
import static org.assertj.core.api.Assertions.assertThat;

import io.github.lemon_ant.jharmonizer.core.SourceProcessor;
import io.github.lemon_ant.jharmonizer.core.config.input.jharmonizer.JHarmonizerConfigurationManager;
import io.github.lemon_ant.jharmonizer.core.config.unified.UnifiedSeparator;
import io.github.lemon_ant.jharmonizer.core.flow.FlowType;
import io.github.lemon_ant.jharmonizer.core.testutils.SpoonTestCaseUtils;
import io.github.lemon_ant.jharmonizer.core.testutils.TestCaseResourceUtils;
import io.github.lemon_ant.jharmonizer.core.translator.spoon.SpoonAstModel;
import io.github.lemon_ant.jharmonizer.core.translator.spoon.SpoonParser;
import io.github.lemon_ant.jharmonizer.core.translator.spoon.SpoonSourcePrinterUtils;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import lombok.NonNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import spoon.reflect.declaration.CtType;
import spoon.reflect.declaration.CtTypeMember;

class GroupBoundaryMarkerTest {

    private static final URL FIXTURE_URL = GroupBoundaryMarkerTest.class.getResource(
            "/test-cases/core/sorter/spoon/type-member-grouper/valid/TypeMemberGrouperFixture.java");
    private static final URL SEPARATOR_DIRECTIVE_CONFIG_RESOURCE_URL =
            TestCaseResourceUtils.requireClasspathResourceUrl(
                    "/test-cases/core/sorter/spoon/group-boundary-marker/separator-directive-config.yml");
    private static final URL SEPARATOR_DIRECTIVE_INPUT_RESOURCE_URL = TestCaseResourceUtils.requireClasspathResourceUrl(
            "/test-cases/core/sorter/spoon/group-boundary-marker/input/SeparatorDirectiveSample.java");
    private static final URL SEPARATOR_DIRECTIVE_EXPECTED_RESOURCE_URL =
            TestCaseResourceUtils.requireClasspathResourceUrl(
                    "/test-cases/core/sorter/spoon/group-boundary-marker/expected/SeparatorDirectiveSample.java");
    private static final URL NEW_LINE_SEPARATOR_CONFIG_RESOURCE_URL =
            TestCaseResourceUtils.requireClasspathResourceUrl(
                    "/test-cases/core/sorter/spoon/group-boundary-marker/new-line-separator-config.yml");
    private static final URL NEW_LINE_SEPARATOR_INPUT_RESOURCE_URL =
            TestCaseResourceUtils.requireClasspathResourceUrl(
                    "/test-cases/core/sorter/spoon/group-boundary-marker/input/NewLineSeparatorBehaviorSample.java");
    private static final URL NEW_LINE_SEPARATOR_EXPECTED_RESOURCE_URL =
            TestCaseResourceUtils.requireClasspathResourceUrl(
                    "/test-cases/core/sorter/spoon/group-boundary-marker/expected/NewLineSeparatorBehaviorSample.java");

    @TempDir
    Path temporaryDirectory;

    @Test
    void markGroupBoundaries_groupsContainMembers_writeMetadataOnlyToFirstMemberOfEachNonEmptyGroup() {
        // Given
        CtType<?> parsedMainType = parseMainType();
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
        assertThat(alphaFieldMember.getMetadata(SpoonSourcePrinterUtils.GROUP_HEADER_METADATA))
                .isEqualTo("Header fields");
        assertThat(bravoFieldMember.getMetadata(SpoonSourcePrinterUtils.GROUP_HEADER_METADATA))
                .isNull();
        assertThat(charlieMethodMember.getMetadata(SpoonSourcePrinterUtils.GROUP_HEADER_METADATA))
                .isEqualTo(SpoonSourcePrinterUtils.GROUP_SEPARATOR_NEW_LINE);
        assertThat(deltaMethodMember.getMetadata(SpoonSourcePrinterUtils.GROUP_HEADER_METADATA))
                .isNull();
    }

    @Test
    void markGroupBoundaries_headerCommentAlreadyPresent_writeOnlyAlreadyPresentMetadata() {
        // Given
        SpoonAstModel spoonAstModel = SpoonParser.parseJavaSrcFile(createSrcFile("""
                class HeaderFixture {
                    // Header fields
                    int a;
                    int z;
                }
                """, Path.of("HeaderFixture.java")));
        CtType<?> parsedMainType = spoonAstModel.getMainType().orElseThrow();
        CtTypeMember alphaFieldMember =
                SpoonTestCaseUtils.requireTypeMemberBySimpleName(parsedMainType.getTypeMembers(), "a");
        CtTypeMember bravoFieldMember =
                SpoonTestCaseUtils.requireTypeMemberBySimpleName(parsedMainType.getTypeMembers(), "z");
        List<MemberGroupBlock> orderedBlocks = List.of(createGroupBlock(
                "Header fields", UnifiedSeparator.HEADER, List.of(alphaFieldMember, bravoFieldMember)));

        // When
        GroupBoundaryMarker.markGroupBoundaries(orderedBlocks);

        // Then
        assertThat(alphaFieldMember.getMetadata(SpoonSourcePrinterUtils.GROUP_HEADER_METADATA))
                .isEqualTo("Header fields");
    }

    @Test
    void markGroupBoundaries_groupIsEmpty_skipSeparatorMetadataEmission() {
        // Given
        CtType<?> parsedMainType = parseMainType();
        CtTypeMember alphaFieldMember =
                SpoonTestCaseUtils.requireTypeMemberBySimpleName(parsedMainType.getTypeMembers(), "alpha");
        List<MemberGroupBlock> orderedBlocks = List.of(
                createGroupBlock("Empty header", UnifiedSeparator.HEADER, List.of()),
                createGroupBlock("No separator member", UnifiedSeparator.NONE, List.of(alphaFieldMember)));

        // When
        GroupBoundaryMarker.markGroupBoundaries(orderedBlocks);

        // Then
        assertThat(alphaFieldMember.getMetadata(SpoonSourcePrinterUtils.GROUP_HEADER_METADATA))
                .isNull();
    }

    @Test
    void processSources_separatorDirectiveConfig_emitExpectedHeaderBlankLineAndNoneOutput() throws Exception {
        // Given
        String originalSourceCode =
                TestCaseResourceUtils.readClasspathResourceAsString(SEPARATOR_DIRECTIVE_INPUT_RESOURCE_URL);
        String expectedSourceCode =
                TestCaseResourceUtils.readClasspathResourceAsString(SEPARATOR_DIRECTIVE_EXPECTED_RESOURCE_URL);
        Path javaFilePath = writeJavaFile(temporaryDirectory, "SeparatorDirectiveSample.java", originalSourceCode);
        SourceProcessor sourceProcessor =
                new SourceProcessor(JHarmonizerConfigurationManager.parseFlexibleUnifiedConfigFromClasspathResource(
                        SEPARATOR_DIRECTIVE_CONFIG_RESOURCE_URL));

        // When
        sourceProcessor.processSources(temporaryDirectory, List.of("*.java"), List.of(), FlowType.RESTRUCTURE);
        String processedSourceCode = Files.readString(javaFilePath, StandardCharsets.UTF_8);

        // Then
        assertThat(processedSourceCode).isEqualTo(expectedSourceCode);
    }

    @Test
    void processSources_newLineSeparator_printedOnlyWhenMissingAndNeverAsHeaderComment() throws Exception {
        // Given
        String originalSourceCode =
                TestCaseResourceUtils.readClasspathResourceAsString(NEW_LINE_SEPARATOR_INPUT_RESOURCE_URL);
        String expectedSourceCode =
                TestCaseResourceUtils.readClasspathResourceAsString(NEW_LINE_SEPARATOR_EXPECTED_RESOURCE_URL);
        Path javaFilePath = writeJavaFile(temporaryDirectory, "NewLineSeparatorBehaviorSample.java", originalSourceCode);
        SourceProcessor sourceProcessor =
                new SourceProcessor(JHarmonizerConfigurationManager.parseFlexibleUnifiedConfigFromClasspathResource(
                        NEW_LINE_SEPARATOR_CONFIG_RESOURCE_URL));

        // When
        sourceProcessor.processSources(temporaryDirectory, List.of("*.java"), List.of(), FlowType.RESTRUCTURE);
        String processedSourceCode = Files.readString(javaFilePath, StandardCharsets.UTF_8);

        // Then
        assertThat(processedSourceCode).isEqualTo(expectedSourceCode);
        assertThat(processedSourceCode).doesNotContain("// " + SpoonSourcePrinterUtils.GROUP_SEPARATOR_NEW_LINE);
    }

    @NonNull
    private static CtType<?> parseMainType() {
        return SpoonTestCaseUtils.parseMainTypeFromJavaFixtureResource(FIXTURE_URL);
    }

    @NonNull
    private static MemberGroupBlock createGroupBlock(
            String groupName, UnifiedSeparator separator, List<CtTypeMember> members) {
        return new MemberGroupBlock(createTrivialMemberGroup(groupName, false, 0, separator), members);
    }

    @NonNull
    private static Path writeJavaFile(Path baseDirectoryPath, String fileName, String fileContent) throws Exception {
        Path javaFilePath = baseDirectoryPath.resolve(fileName);
        return Files.writeString(javaFilePath, fileContent, StandardCharsets.UTF_8);
    }
}
