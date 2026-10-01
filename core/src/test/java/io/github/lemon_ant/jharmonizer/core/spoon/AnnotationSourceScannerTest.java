// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
package io.github.lemon_ant.jharmonizer.core.spoon;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.groups.Tuple.tuple;

import io.github.lemon_ant.jharmonizer.core.spoon.AnnotationSrcGroup.AnnotationSrcFragment;
import io.github.lemon_ant.jharmonizer.core.spoon.AnnotationSrcGroup.AnnotationSrcGap;
import java.util.List;
import lombok.NonNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
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
        AnnotationSrcFragment fragment = AnnotationSourceScanner.scan(srcCode)
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

    @Test
    void scan_annotationsAndGaps_preservesSourceRangesAndCachesReplacement() {
        // Given
        String srcCode = "@Deprecated /* note */ @SuppressWarnings(\"all\") class Sample {}";
        int firstAnnotationEnd = srcCode.indexOf(" @SuppressWarnings");
        int secondAnnotationStart = srcCode.indexOf("@SuppressWarnings");
        int secondAnnotationEnd = srcCode.indexOf(" class");
        int groupEnd = srcCode.indexOf("class");

        // When
        AnnotationSrcGroup group = AnnotationSourceScanner.scan(srcCode).get(0);

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
    void scan_boundedDeclaration_countsInternalTextAndPreservesOffsets(@NonNull String annotation) {
        // Given
        String prefix = "/* leading */ ";
        String srcCode = prefix + annotation + " /* trailing */ @B class Sample {}";

        // When
        List<AnnotationSrcGroup> groups = AnnotationSourceScanner.scan(srcCode);

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

    @Test
    void scan_escapedLineTerminator_requiresNoPhysicalSeparator() {
        // When
        AnnotationSrcFragment fragment = AnnotationSourceScanner.scan("@A // note\\u000a @B class Sample {}")
                .get(0)
                .getAnnotationSrcFragments()
                .get(0);

        // Then
        assertThat(fragment.getSrcCode()).isEqualTo("@A // note\\u000a");
        assertThat(fragment.getTrailingLineSeparatorCount()).isZero();
        assertThat(fragment.requiresTrailingBlankLine()).isFalse();
        assertThat(fragment.getTrailingLineSeparators()).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"@A(", "@A(\""})
    void scan_invalidAnnotationArguments_rejectsSource(@NonNull String srcCode) {
        assertThatThrownBy(() -> AnnotationSourceScanner.scan(srcCode)).isInstanceOf(IllegalArgumentException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"\n", "\r\n", "\r"})
    void scan_lineComment_countsCommentWithoutLineSeparator(@NonNull String lineSeparator) {
        List<AnnotationSrcGroup> groups = AnnotationSourceScanner.scan("@A(// note  " + lineSeparator + "\"aa\")");
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
        List<AnnotationSrcGroup> groups = AnnotationSourceScanner.scan(
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

    @Test
    void scan_returnedGroups_preventsMutation() {
        List<AnnotationSrcGroup> groups = AnnotationSourceScanner.scan("@Z @A class Sample {}");
        assertThatThrownBy(groups::clear).isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> groups.get(0).getAnnotationSrcFragments().clear())
                .isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> groups.get(0).getAnnotationSrcGapsInSrcOrder().clear())
                .isInstanceOf(UnsupportedOperationException.class);
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
                AnnotationSourceScanner.scan(srcCode).get(0).getAnnotationSrcFragments();

        // Then
        AnnotationSrcFragment firstFragment = fragments.get(0);
        assertThat(firstFragment.getSrcCode()).isEqualTo(commentedAnnotation);
        assertThat(firstFragment.getTrailingLineSeparators()).isEqualTo(blankLineSeparator);
        assertThat(firstFragment.getTrailingLineSeparatorCount()).isEqualTo(2);
        assertThat(firstFragment.requiresTrailingBlankLine()).isTrue();
        assertThat(firstFragment.getDescriptor().getDeclarationLength()).isEqualTo("@A".length());
        assertThat(fragments.get(1).getSrcCode()).isEqualTo("// above" + lineSeparator + "@B");
        assertThat(fragments.get(1).requiresTrailingBlankLine()).isFalse();
    }

    @ParameterizedTest
    @ValueSource(
            strings = {" /* trailing */", " /* first */ /** second */", " /* first\n continuation */ /* second */"})
    void scan_trailingBlockComments_preservesCommentAndSeparatorBounds(@NonNull String comments) {
        // Given
        String annotation = "@A";
        String srcCode = annotation + comments + "\n/* detached */\n@B class Sample {}";

        // When
        AnnotationSrcFragment fragment = AnnotationSourceScanner.scan(srcCode)
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
        AnnotationSrcFragment fragment = AnnotationSourceScanner.scan(srcCode)
                .get(0)
                .getAnnotationSrcFragments()
                .get(0);

        // Then
        assertThat(fragment.getSrcCode()).isEqualTo(commentedAnnotation);
        assertThat(fragment.getTrailingLineSeparatorCount()).isEqualTo(1);
        assertThat(fragment.requiresTrailingBlankLine()).isFalse();
        assertThat(fragment.getTrailingLineSeparators()).isEqualTo(lineSeparator);
        assertThat(fragment.getDescriptor().getDeclarationLength()).isEqualTo(annotation.length());
    }

    @Test
    void scan_whitespaceAndComments_countsDeclarationTokensAndComments() {
        List<AnnotationSrcGroup> groups =
                AnnotationSourceScanner.scan("@A (value = 1 /* note */ + 2) @B class Sample {}");
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
}
