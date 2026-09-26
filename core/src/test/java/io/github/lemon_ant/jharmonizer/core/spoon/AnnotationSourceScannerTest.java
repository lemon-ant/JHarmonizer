// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
package io.github.lemon_ant.jharmonizer.core.spoon;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.lemon_ant.jharmonizer.core.spoon.AnnotationSourceScanner.AnnotationSrcFragment;
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
        AnnotationSrcFragment fragment =
                AnnotationSourceScanner.scan(srcCode).get(0).get(1);

        // Then
        assertThat(fragment.getLeadingCommentStart()).isEqualTo(prefix.length());
        assertThat(fragment.getStart()).isEqualTo(prefix.length() + comments.length());
        assertThat(fragment.getTrailingCommentEndExclusive())
                .isEqualTo(prefix.length() + comments.length() + "@B".length());
        assertThat(fragment.getFollowingTokenStart()).isEqualTo(srcCode.indexOf("class"));
        assertThat(fragment.getDescriptor().getDeclarationLength()).isEqualTo("@B".length());
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
        List<List<AnnotationSrcFragment>> groups = AnnotationSourceScanner.scan(srcCode);

        // Then
        AnnotationSrcFragment fragment = groups.get(0).get(0);
        assertThat(fragment.getDescriptor().getDeclarationLength()).isEqualTo(annotation.length());
        assertThat(fragment.getStart()).isEqualTo(prefix.length());
        assertThat(fragment.getFollowingTokenStart()).isEqualTo(srcCode.indexOf("@B"));
        assertThat(fragment.getTrailingCommentEndExclusive())
                .isEqualTo(prefix.length() + annotation.length() + " /* trailing */".length());
        assertThat(groups.get(0).get(1).getDescriptor().getDeclarationLength()).isEqualTo("@B".length());
    }

    @ParameterizedTest
    @ValueSource(strings = {"@A(", "@A(\""})
    void scan_invalidAnnotationArguments_rejectsSource(@NonNull String srcCode) {
        assertThatThrownBy(() -> AnnotationSourceScanner.scan(srcCode)).isInstanceOf(IllegalArgumentException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"\n", "\r\n", "\r"})
    void scan_lineComment_countsCommentWithoutLineSeparator(@NonNull String lineSeparator) {
        List<List<AnnotationSrcFragment>> groups =
                AnnotationSourceScanner.scan("@A(// note  " + lineSeparator + "\"aa\")");
        assertThat(groups.get(0).get(0).getDescriptor().getDeclarationLength())
                .isEqualTo("@A(// note  \"aa\")".length());
        assertThat(groups.get(0).get(0).getDescriptor().getArguments()).isEqualTo("\"aa\"");
    }

    @Test
    void scan_multipleGroups_preservesEachGroupAfterBufferReuse() {
        List<List<AnnotationSrcFragment>> groups = AnnotationSourceScanner.scan(
                "@Deprecated class AnnotationGroups { @SuppressWarnings(\"all\") void execute() {} }");
        assertThat(groups).hasSize(2).allSatisfy(group -> assertThat(group).hasSize(1));
        assertThat(groups.stream().flatMap(List::stream))
                .extracting(fragment -> fragment.getDescriptor().getName())
                .containsExactly("Deprecated", "SuppressWarnings");
    }

    @Test
    void scan_returnedGroups_preventsMutation() {
        List<List<AnnotationSrcFragment>> groups = AnnotationSourceScanner.scan("@Z @A class Sample {}");
        assertThatThrownBy(groups::clear).isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> groups.get(0).clear()).isInstanceOf(UnsupportedOperationException.class);
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
                AnnotationSourceScanner.scan(srcCode).get(0);

        // Then
        AnnotationSrcFragment firstFragment = fragments.get(0);
        assertThat(firstFragment.getTrailingCommentEndExclusive()).isEqualTo(commentedAnnotation.length());
        assertThat(firstFragment.getTrailingLineSeparatorEndExclusive())
                .isEqualTo(commentedAnnotation.length() + blankLineSeparator.length());
        assertThat(firstFragment.getTrailingLineSeparatorCount()).isEqualTo(2);
        assertThat(firstFragment.getFollowingTokenStart()).isEqualTo(srcCode.indexOf("@B"));
        assertThat(firstFragment.getDescriptor().getDeclarationLength()).isEqualTo("@A".length());
        assertThat(fragments.get(1).getLeadingCommentStart())
                .isEqualTo(firstFragment.getTrailingLineSeparatorEndExclusive());
    }

    @ParameterizedTest
    @ValueSource(
            strings = {" /* trailing */", " /* first */ /** second */", " /* first\n continuation */ /* second */"})
    void scan_trailingBlockComments_preservesCommentAndSeparatorBounds(@NonNull String comments) {
        // Given
        String annotation = "@A";
        String srcCode = annotation + comments + "\n/* detached */\n@B class Sample {}";

        // When
        AnnotationSrcFragment fragment =
                AnnotationSourceScanner.scan(srcCode).get(0).get(0);

        // Then
        assertThat(fragment.getTrailingCommentEndExclusive()).isEqualTo(annotation.length() + comments.length());
        assertThat(fragment.getTrailingLineSeparatorEndExclusive())
                .isEqualTo(fragment.getTrailingCommentEndExclusive());
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
        AnnotationSrcFragment fragment =
                AnnotationSourceScanner.scan(srcCode).get(0).get(0);

        // Then
        assertThat(fragment.getTrailingCommentEndExclusive()).isEqualTo(commentedAnnotation.length());
        assertThat(fragment.getTrailingLineSeparatorEndExclusive())
                .isEqualTo(commentedAnnotation.length() + lineSeparator.length());
        assertThat(fragment.getDescriptor().getDeclarationLength()).isEqualTo(annotation.length());
    }

    @Test
    void scan_whitespaceAndComments_countsDeclarationTokensAndComments() {
        List<List<AnnotationSrcFragment>> groups =
                AnnotationSourceScanner.scan("@A (value = 1 /* note */ + 2) @B class Sample {}");
        assertThat(groups.get(0).get(0).getDescriptor().getDeclarationLength())
                .isEqualTo("@A(value=1/* note */+2)".length());
        assertThat(groups.get(0).get(0).getDescriptor().getArguments()).isEqualTo("value=1+2");
    }
}
