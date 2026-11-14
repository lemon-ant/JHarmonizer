// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
package io.github.lemon_ant.jharmonizer.core.spoon;

import static io.github.lemon_ant.jharmonizer.core.testutils.SpoonTestCaseUtils.scanAnnotationSrcGroups;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.groups.Tuple.tuple;

import io.github.lemon_ant.jharmonizer.core.spoon.AnnotationSrcGroup.AnnotationSrcFragment;
import io.github.lemon_ant.jharmonizer.core.spoon.AnnotationSrcGroup.AnnotationSrcGap;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import lombok.NonNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

class AnnotationSourceScannerTest {

    @ParameterizedTest
    @ValueSource(strings = {"\n", "\r\n", "\r"})
    void scan_adjacentLeadingComments_preservesCommentAndTokenBounds(@NonNull String lineSeparator) {
        // Given
        String prefix = "@A // trailing" + lineSeparator;
        String comments = "// leading" + lineSeparator + "/* block */" + lineSeparator;
        String srcCode = prefix + comments + "@B class Sample {}";

        // When
        AnnotationSrcFragment fragment = scanAnnotationSrcGroups(srcCode)
                .get(0)
                .getAnnotationSrcFragments()
                .get(1);

        // Then
        assertThat(fragment.getSrcCode()).isEqualTo(comments + "@B");
        assertThat(fragment.getStart()).isEqualTo(prefix.length());
        assertThat(fragment.getAnnotationStart()).isEqualTo(prefix.length() + comments.length());
        assertThat(fragment.getEndExclusive()).isEqualTo(srcCode.indexOf(" class"));
        assertThat(fragment.getDescriptor().getDeclarationLength()).isEqualTo("@B".length());
    }

    @ParameterizedTest
    @ValueSource(strings = {"\n", "\r\n", "\r"})
    void scan_annotationTypeDeclarationBoundary_preservesPrecedingGapRanges(@NonNull String lineSeparator) {
        // Given
        String annotationPrefix = "@Deprecated";
        String gapSrcCode = lineSeparator + "/* before type */" + lineSeparator;
        String srcCode = annotationPrefix + gapSrcCode + "@interface Marker {}";
        int groupEnd = srcCode.indexOf("@interface");

        // When
        List<AnnotationSrcGroup> groups = scanAnnotationSrcGroups(srcCode);

        // Then
        assertThat(groups).hasSize(1);
        AnnotationSrcGroup group = groups.getFirst();
        assertThat(group.getStart()).isZero();
        assertThat(group.getEndExclusive()).isEqualTo(groupEnd);
        assertThat(group.getAnnotationSrcFragments())
                .extracting(
                        AnnotationSrcFragment::getStart,
                        AnnotationSrcFragment::getEndExclusive,
                        AnnotationSrcFragment::getSrcCode)
                .containsExactly(tuple(0, annotationPrefix.length(), annotationPrefix));
        assertThat(group.getAnnotationSrcGapsInSrcOrder())
                .extracting(AnnotationSrcGap::getStart, AnnotationSrcGap::getEndExclusive, AnnotationSrcGap::getSrcCode)
                .containsExactly(tuple(annotationPrefix.length(), groupEnd, gapSrcCode));
        assertThat(group.getReplacementCode()).isEqualTo(annotationPrefix + gapSrcCode);
    }

    @Test
    void scan_annotationsAndGaps_preservesSourceRangesAndCachesReplacement() {
        // Given
        String srcCode = "@Deprecated /* note */ @SuppressWarnings(\"all\") class Sample {}";
        int firstAnnotationEnd = srcCode.indexOf(" @SuppressWarnings");
        int secondAnnotationStart = srcCode.indexOf("@SuppressWarnings");
        int secondAnnotationEnd = srcCode.indexOf(" class");
        int groupEnd = srcCode.indexOf("class");

        // When
        AnnotationSrcGroup group = scanAnnotationSrcGroups(srcCode).get(0);

        // Then
        assertThat(group.getStart()).isZero();
        assertThat(group.getEndExclusive()).isEqualTo(groupEnd);
        assertThat(group.getAnnotationSrcFragments())
                .extracting(
                        AnnotationSrcFragment::getStart,
                        AnnotationSrcFragment::getEndExclusive,
                        AnnotationSrcFragment::getSrcCode)
                .containsExactly(
                        tuple(0, firstAnnotationEnd, "@Deprecated /* note */"),
                        tuple(secondAnnotationStart, secondAnnotationEnd, "@SuppressWarnings(\"all\")"));
        assertThat(group.getAnnotationSrcGapsInSrcOrder())
                .extracting(AnnotationSrcGap::getStart, AnnotationSrcGap::getEndExclusive, AnnotationSrcGap::getSrcCode)
                .containsExactly(
                        tuple(firstAnnotationEnd, secondAnnotationStart, " "),
                        tuple(secondAnnotationEnd, groupEnd, " "));
        String replacementCode = group.getReplacementCode();
        assertThat(replacementCode).isEqualTo(srcCode.substring(0, groupEnd));
        assertThat(group.getReplacementCode()).isSameAs(replacementCode);
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                "@qualified.Name",
                "@qualified/* name */.Name()",
                "@A(/** argument */\"aa\")",
                "@A({@Nested(/* nested */\"aa\")})"
            })
    void scan_boundedDeclaration_measuresInternalTextAndPreservesOffsets(@NonNull String annotation) {
        // Given
        String prefix = "/* leading */ ";
        String srcCode = prefix + annotation + " /* trailing */ @B class Sample {}";

        // When
        List<AnnotationSrcGroup> groups = scanAnnotationSrcGroups(srcCode);

        // Then
        AnnotationSrcGroup group = groups.get(0);
        AnnotationSrcFragment fragment = group.getAnnotationSrcFragments().get(0);
        assertThat(fragment.getDescriptor().getDeclarationLength()).isEqualTo(annotation.length());
        assertThat(fragment.getStart()).isEqualTo(prefix.length());
        assertThat(fragment.getSrcCode()).isEqualTo(annotation + " /* trailing */");
        assertThat(group.getStart()).isEqualTo(prefix.length());
        assertThat(group.getEndExclusive()).isEqualTo(srcCode.indexOf("class"));
        assertThat(group.getAnnotationSrcFragments().get(1).getDescriptor().getDeclarationLength())
                .isEqualTo("@B".length());
    }

    @ParameterizedTest
    @CsvSource(
            value = {"@Alpha|6", "@Alpha ( 123 )|11", "@Alpha ( \"a b\" )|13"},
            delimiter = '|')
    void scan_declarationPrefixAndSpacing_measuresTokenSrcLength(
            @NonNull String annotation, int expectedDeclarationLength) {
        String srcCode = "int variable; " + annotation + " @BehindAlpha";

        // When
        List<AnnotationSrcFragment> annotationSrcFragments =
                scanAnnotationSrcGroups(srcCode).getFirst().getAnnotationSrcFragments();

        // Then
        assertThat(annotationSrcFragments)
                .extracting(fragment -> fragment.getDescriptor().getDeclarationLength())
                .containsExactly(expectedDeclarationLength, 12);
    }

    @Test
    void scan_escapedLineTerminator_requiresNoPhysicalSeparator() {
        // When
        AnnotationSrcFragment fragment = scanAnnotationSrcGroups("@A // note\\u000a @B class Sample {}")
                .get(0)
                .getAnnotationSrcFragments()
                .get(0);

        // Then
        assertThat(fragment.getSrcCode()).isEqualTo("@A // note\\u000a");
        assertThat(fragment.getTrailingLineSeparatorCount()).isZero();
        assertThat(fragment.requiresBlankLineAfterTrailingComments()).isFalse();
        assertThat(fragment.getTrailingLineSeparators()).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"@A(", "@A(\""})
    void scan_invalidAnnotationArguments_rejectsSource(@NonNull String srcCode) {
        assertThatThrownBy(() -> scanAnnotationSrcGroups(srcCode))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("JDT");
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                "\n",
                "\r\n",
                "\r",
                "\\u000a",
                "\\u000d",
                "\\u000d\\u000a",
                "\\u000d\n",
                "\r\\u000a",
                "\\u000d\\u0020",
                "\r\\u0020",
                "\\uuuu000a"
            })
    void scan_lineComment_measuresCommentWithoutLineSeparator(@NonNull String lineSeparator) {
        List<AnnotationSrcGroup> groups = scanAnnotationSrcGroups("@A(// note  " + lineSeparator + "\"aa\")");
        assertThat(groups.get(0)
                        .getAnnotationSrcFragments()
                        .get(0)
                        .getDescriptor()
                        .getDeclarationLength())
                .isEqualTo("@A(// note  \"aa\")".length());
        assertThat(groups.get(0)
                        .getAnnotationSrcFragments()
                        .get(0)
                        .getDescriptor()
                        .getArguments())
                .isEqualTo("\"aa\"");
    }

    @Test
    void scan_multipleGroups_keepsIndependentFragmentLists() {
        List<AnnotationSrcGroup> groups = scanAnnotationSrcGroups(
                "@Deprecated class AnnotationSrcGroups { @SuppressWarnings(\"all\") void execute() {} }");
        assertThat(groups)
                .hasSize(2)
                .allSatisfy(
                        group -> assertThat(group.getAnnotationSrcFragments()).hasSize(1));
        assertThat(groups.stream().flatMap(group -> group.getAnnotationSrcFragments().stream()))
                .extracting(fragment -> fragment.getDescriptor().getName())
                .containsExactly("Deprecated", "SuppressWarnings");
        assertThat(groups)
                .extracting(AnnotationSrcGroup::getReplacementCode)
                .containsExactly("@Deprecated ", "@SuppressWarnings(\"all\") ");
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                "",
                "class Sample {}",
                "class Sample { String text = \"\\n\"; }",
                "class S\\u0061mple {}",
                "class Sample { String text = \"\\u0040\"; }",
                "class Sample { String text = \"\\\\u0040\"; }",
                "/* \\uu0040 */ class Sample {}",
                "/* \\u005cu0040 */ class Sample {}"
            })
    void scan_noAnnotations_returnsNoGroups(@NonNull String srcCode) {
        assertThat(scanAnnotationSrcGroups(srcCode)).isEmpty();
    }

    @ParameterizedTest
    @CsvSource({"@Z @A, 1", "@UnknownOwner.Tag @Tag, 2"})
    void scan_returnedGroups_preventsMutation(@NonNull String annotationDeclarations, int expectedGroupKeyCount) {
        List<AnnotationSrcGroup> groups = scanAnnotationSrcGroups(annotationDeclarations + " class Sample {}");
        assertThatThrownBy(groups::clear).isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> groups.get(0).getAnnotationSrcFragments().clear())
                .isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> groups.get(0).getAnnotationSrcGapsInSrcOrder().clear())
                .isInstanceOf(UnsupportedOperationException.class);
        assertThat(groups.getFirst().getAnnotationSrcFragments()).allSatisfy(annotationSrcFragment -> {
            Set<String> groupKeys = annotationSrcFragment.getDescriptor().getOrderPreservationGroupKeys();
            assertThat(groupKeys).hasSize(expectedGroupKeyCount);
            assertThatThrownBy(groupKeys::clear).isInstanceOf(UnsupportedOperationException.class);
        });
    }

    @ParameterizedTest
    @ValueSource(strings = {"\n", "\r\n", "\r"})
    void scan_separatedFollowingComments_preservesCommentAndSeparatorBounds(@NonNull String lineSeparator) {
        // Given
        String commentedAnnotation = "@A // trailing" + lineSeparator + "/* block */" + lineSeparator + "// below";
        String blankLineSeparator = lineSeparator.repeat(2);
        String srcCode = commentedAnnotation + blankLineSeparator + "// above" + lineSeparator + "@B class Sample {}";

        // When
        List<AnnotationSrcFragment> fragments =
                scanAnnotationSrcGroups(srcCode).get(0).getAnnotationSrcFragments();

        // Then
        AnnotationSrcFragment firstFragment = fragments.get(0);
        assertThat(firstFragment.getSrcCode()).isEqualTo(commentedAnnotation);
        assertThat(firstFragment.getTrailingLineSeparators()).isEqualTo(blankLineSeparator);
        assertThat(firstFragment.getTrailingLineSeparatorCount()).isEqualTo(2);
        assertThat(firstFragment.requiresBlankLineAfterTrailingComments()).isTrue();
        assertThat(firstFragment.getDescriptor().getDeclarationLength()).isEqualTo("@A".length());
        assertThat(fragments.get(1).getSrcCode()).isEqualTo("// above" + lineSeparator + "@B");
        assertThat(fragments.get(1).requiresBlankLineAfterTrailingComments()).isFalse();
    }

    @ParameterizedTest
    @MethodSource("provideLexicalTextBlockBodies")
    void scan_textBlockJavaEscapes_preservesLexicalArguments(
            @NonNull String body, @NonNull String expectedLexicalBody) {
        // Given
        String annotation = "@A(\"\"\"\n" + body + "\n\"\"\")";

        // When
        AnnotationSrcFragment fragment = scanAnnotationSrcGroups(annotation)
                .getFirst()
                .getAnnotationSrcFragments()
                .getFirst();

        // Then
        assertThat(fragment.getDescriptor().getDeclarationLength()).isEqualTo(annotation.length());
        assertThat(fragment.getDescriptor().getArguments()).isEqualTo("\"\"\"\n" + expectedLexicalBody + "\n\"\"\"");
        assertThat(fragment.getSrcCode()).isEqualTo(annotation);
    }

    @ParameterizedTest
    @ValueSource(
            strings = {" /* trailing */", " /* first */ /** second */", " /* first\n continuation */ /* second */"})
    void scan_trailingBlockComments_preservesCommentAndSeparatorBounds(@NonNull String comments) {
        // Given
        String annotation = "@A";
        String srcCode = annotation + comments + "\n/* detached */\n@B class Sample {}";

        // When
        AnnotationSrcFragment fragment = scanAnnotationSrcGroups(srcCode)
                .get(0)
                .getAnnotationSrcFragments()
                .get(0);

        // Then
        assertThat(fragment.getSrcCode()).isEqualTo(annotation + comments);
        assertThat(fragment.getTrailingLineSeparators()).isEmpty();
        assertThat(fragment.getDescriptor().getDeclarationLength()).isEqualTo(annotation.length());
    }

    @ParameterizedTest
    @ValueSource(strings = {"\n", "\r\n", "\r"})
    void scan_trailingLineComment_preservesCommentAndTerminatorBounds(@NonNull String lineSeparator) {
        // Given
        String annotation = "@A";
        String commentedAnnotation = annotation + " /* block */ // trailing  ";
        String srcCode = commentedAnnotation + lineSeparator + "// detached" + lineSeparator + "@B class Sample {}";

        // When
        AnnotationSrcFragment fragment = scanAnnotationSrcGroups(srcCode)
                .get(0)
                .getAnnotationSrcFragments()
                .get(0);

        // Then
        assertThat(fragment.getSrcCode()).isEqualTo(commentedAnnotation);
        assertThat(fragment.getTrailingLineSeparatorCount()).isEqualTo(1);
        assertThat(fragment.requiresBlankLineAfterTrailingComments()).isFalse();
        assertThat(fragment.getTrailingLineSeparators()).isEqualTo(lineSeparator);
        assertThat(fragment.getDescriptor().getDeclarationLength()).isEqualTo(annotation.length());
    }

    @ParameterizedTest
    @MethodSource("provideUnicodeEscapedDeclarations")
    void scan_unicodeEscapedDeclaration_measuresRawTokenTextAndPreservesDecodedKeys(
            @NonNull String annotation, @NonNull String translatedAnnotation, int expectedDeclarationLength) {
        // Given
        String prefix = "/* leading */ ";
        String srcCode = prefix + annotation + " /* trailing */ class Sample {}";
        AnnotationSrcFragment translatedFragment = scanAnnotationSrcGroups(translatedAnnotation)
                .getFirst()
                .getAnnotationSrcFragments()
                .getFirst();

        // When
        AnnotationSrcFragment fragment = scanAnnotationSrcGroups(srcCode)
                .getFirst()
                .getAnnotationSrcFragments()
                .getFirst();

        // Then
        assertThat(fragment.getDescriptor().getDeclarationLength()).isEqualTo(expectedDeclarationLength);
        assertThat(fragment.getDescriptor().getName())
                .isEqualTo(translatedFragment.getDescriptor().getName());
        assertThat(fragment.getDescriptor().getArguments())
                .isEqualTo(translatedFragment.getDescriptor().getArguments());
        assertThat(fragment.getDescriptor().getOrderPreservationGroupKeys())
                .isEqualTo(translatedFragment.getDescriptor().getOrderPreservationGroupKeys());
        assertThat(fragment.getAnnotationStart()).isEqualTo(prefix.length());
        assertThat(fragment.getSrcCode()).isEqualTo(annotation + " /* trailing */");
        assertThat(fragment.getEndExclusive()).isEqualTo(srcCode.indexOf(" class"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "  ", "    ", "\\u0020\\u0020", "\\u0009", " \\uuuu0020"})
    void scan_unicodeEscapedTextBlock_excludesIncidentalIndentation(@NonNull String indentation) {
        // Given
        String annotation = "@A(\"\"\"\n" + indentation + "\\u0061\n" + indentation + "\"\"\")";

        // When
        AnnotationSrcFragment fragment = scanAnnotationSrcGroups(annotation)
                .getFirst()
                .getAnnotationSrcFragments()
                .getFirst();

        // Then
        assertThat(fragment.getDescriptor().getDeclarationLength()).isEqualTo("@A(\"\"\"\n\\u0061\n\"\"\")".length());
        assertThat(fragment.getDescriptor().getArguments()).isEqualTo("\"\"\"\na\n\"\"\"");
        assertThat(fragment.getSrcCode()).isEqualTo(annotation);
    }

    @Test
    void scan_whitespaceAndComments_measuresTokenAndCommentText() {
        List<AnnotationSrcGroup> groups = scanAnnotationSrcGroups("@A (value = 1 /* note */ + 2) @B class Sample {}");
        assertThat(groups.get(0)
                        .getAnnotationSrcFragments()
                        .get(0)
                        .getDescriptor()
                        .getDeclarationLength())
                .isEqualTo("@A(value=1/* note */+2)".length());
        assertThat(groups.get(0)
                        .getAnnotationSrcFragments()
                        .get(0)
                        .getDescriptor()
                        .getArguments())
                .isEqualTo("value=1+2");
    }

    @NonNull
    private static Stream<Arguments> provideLexicalTextBlockBodies() {
        return Stream.of(
                Arguments.of("\\n\\s", "\\n\\s"),
                Arguments.of("\\\\u0061", "\\\\u0061"),
                Arguments.of("\\u005c\\\\u0061", "\\\\a"),
                Arguments.of("\\u005cn\\u005cs", "\\n\\s"),
                Arguments.of("\\u005c\\u005cu0061", "\\\\u0061"));
    }

    @NonNull
    private static Stream<Arguments> provideUnicodeEscapedDeclarations() {
        return Stream.of(
                Arguments.of("\\u0040A", "@A", 7),
                Arguments.of("\\uuuu0040A", "@A", 10),
                Arguments.of("@\\u0041", "@A", 7),
                Arguments.of("@\\uuuu0041", "@A", 10),
                Arguments.of("@pkg\\u002eA", "@pkg.A", 11),
                Arguments.of("@A\\u0028\\u0029", "@A()", 14),
                Arguments.of("@A(\"\\u0061\")", "@A(\"a\")", 12),
                Arguments.of("@A('\\u0061')", "@A('a')", 12),
                Arguments.of("@A(\\u0022x\\u0022)", "@A(\"x\")", 17),
                Arguments.of("@A({\\u0040\\u0042(\"\\u0061\")})", "@A({@B(\"a\")})", 28),
                Arguments.of(
                        "@pkg /* \\u0061 */ \\u002e A ( /* \\u0062 */ \"\\u0063\" )",
                        "@pkg /* a */ . A ( /* b */ \"c\" )",
                        45),
                Arguments.of("\\u0040 \\u0041 \\u0028 \\u0029", "@ A ( )", 24),
                Arguments.of("@A(\\u0020\"\\u0061\"\\u0009)", "@A( \"a\"\t)", 12),
                Arguments.of("@A(\\u002f\\u002a x \\u002a\\u002f 1)", "@A(/* x */ 1)", 32),
                Arguments.of("@A(//\\u0061\\u000a 1)", "@A(//a\n 1)", 13),
                Arguments.of("@A(\\u0022\\u0022\\u0022\nx\n\\u0022\\u0022\\u0022)", "@A(\"\"\"\nx\n\"\"\")", 43));
    }
}
