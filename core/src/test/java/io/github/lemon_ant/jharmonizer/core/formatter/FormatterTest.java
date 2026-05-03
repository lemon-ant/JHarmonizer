// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
package io.github.lemon_ant.jharmonizer.core.formatter;

import static io.github.lemon_ant.jharmonizer.core.config.unified.UnifiedFormatterStyle.NONE;
import static io.github.lemon_ant.jharmonizer.core.config.unified.UnifiedFormatterStyle.PALANTIR;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import io.github.lemon_ant.jharmonizer.core.translator.SrcCharacterRange;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class FormatterTest {

    @Test
    void fixImports_validSrcClass_returnsExpectedFormattedClass() {
        // Given
        Formatter formatter = new Formatter(PALANTIR, true);
        String srcClass = "import junit.framework.TestCase; \npublic class Person {}";
        String expectedClass = "public class Person {}\n";

        // When
        FormattingResult formattingResult = formatter.formatSrc(srcClass, Path.of(""), List.of());
        FormattingStatistic formattingStatistic = formattingResult.getFormattingStatistic();

        // Then
        assertThat(formattingResult.getFormattedSrcCode()).isEqualTo(expectedClass);
        assertThat(formattingStatistic).isNotNull();
        assertThat(formattingStatistic.getFormattedCodeLength()).isEqualTo(expectedClass.length());
        assertThat(formattingStatistic.getFormattingTimeInNanos()).isGreaterThan(1_000_000);
    }

    @Test
    void formatSourceAndFixImports_validSrcClass_returnsExpectedFormattedClass() {
        // Given
        Formatter formatter = new Formatter(PALANTIR, false);
        String srcClass = "public class Person {}";
        String expectedClass = "public class Person {}\n";

        // When
        FormattingResult formattingResult = formatter.formatSrc(srcClass, Path.of(""), List.of());
        FormattingStatistic formattingStatistic = formattingResult.getFormattingStatistic();

        // Then
        assertThat(formattingResult.getFormattedSrcCode()).isEqualTo(expectedClass);
        assertThat(formattingStatistic).isNotNull();
        assertThat(formattingStatistic.getFormattedCodeLength()).isEqualTo(expectedClass.length());
        assertThat(formattingStatistic.getFormattingTimeInNanos()).isGreaterThan(1_000_000);
    }

    @Test
    void formatSource_withFullExclusionRange_keepSrcUnchanged() {
        // Given
        Formatter formatter = new Formatter(PALANTIR, false);
        String srcCode = "class Person {  }\n";

        // When
        FormattingResult formattingResult = formatter.formatSrc(
                srcCode, Path.of("Person.java"), List.of(new SrcCharacterRange(0, srcCode.length())));

        // Then
        assertThat(formattingResult.getFormattedSrcCode()).isEqualTo(srcCode);
    }

    @Test
    void formatSource_middleExclusionRange_preserveExcludedFragment() {
        // Given
        Formatter formatter = new Formatter(PALANTIR, false);
        String excludedFragment = "int  preserved;";
        String srcCode = "class Person{int a;  %s}\n".formatted(excludedFragment);
        int excludedStart = srcCode.indexOf(excludedFragment);
        int excludedEnd = excludedStart + excludedFragment.length();

        // When
        FormattingResult formattingResult = formatter.formatSrc(
                srcCode, Path.of("Person.java"), List.of(new SrcCharacterRange(excludedStart, excludedEnd)));

        // Then
        assertThat(formattingResult.getFormattedSrcCode())
                .contains("class Person {")
                .contains("int a;")
                .contains(excludedFragment);
    }

    @Test
    void formatSource_overlappingExclusionRanges_throwIllegalArgumentException() {
        // Given
        Formatter formatter = new Formatter(PALANTIR, false);
        String srcCode = "class Person{int a; int b;}\n";
        List<SrcCharacterRange> formattingSkippedRanges =
                List.of(new SrcCharacterRange(0, 10), new SrcCharacterRange(8, 15));

        // When
        Throwable thrown =
                catchThrowable(() -> formatter.formatSrc(srcCode, Path.of("Person.java"), formattingSkippedRanges));

        // Then
        assertThat(thrown)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Excluded ranges must be sorted and non-overlapping");
    }

    @Test
    void formatSrc_noneStyleWithoutFixImports_returnsSrcCodeUnchanged() {
        // Given
        Formatter formatter = new Formatter(NONE, false);
        String srcCode = "class Unchanged  { }";

        // When
        FormattingResult formattingResult = formatter.formatSrc(srcCode, Path.of("Unchanged.java"), List.of());

        // Then
        assertThat(formattingResult.getFormattedSrcCode()).isEqualTo(srcCode);
    }

    @Test
    void formatSrc_noneStyleWithFixImports_fixesUnusedImports() {
        // Given
        Formatter formatter = new Formatter(NONE, true);
        String srcCode = "import java.util.List; class Demo {}";

        // When
        FormattingResult formattingResult = formatter.formatSrc(srcCode, Path.of("Demo.java"), List.of());

        // Then
        assertThat(formattingResult.getFormattedSrcCode()).doesNotContain("import java.util.List;");
    }

    @Test
    void formatSrc_withExclusionRangeAndFixImports_fixesImportsAfterPartialFormatting() {
        // Given
        Formatter formatter = new Formatter(PALANTIR, true);
        String srcCode = "import java.util.List; class Partial{int a;}";
        int start = srcCode.indexOf("int a;");
        int end = start + "int a;".length();

        // When
        FormattingResult formattingResult =
                formatter.formatSrc(srcCode, Path.of("Partial.java"), List.of(new SrcCharacterRange(start, end)));

        // Then
        assertThat(formattingResult.getFormattedSrcCode()).doesNotContain("import java.util.List;");
    }

    @Test
    void formatSrc_fullExclusionRangeWithFixImports_fixesImportsOnUnformattedSrc() {
        // Given
        Formatter formatter = new Formatter(PALANTIR, true);
        String srcCode = "import java.util.List; class FullExclude { int  x; }";

        // When
        FormattingResult formattingResult = formatter.formatSrc(
                srcCode, Path.of("FullExclude.java"), List.of(new SrcCharacterRange(0, srcCode.length())));

        // Then
        assertThat(formattingResult.getFormattedSrcCode()).doesNotContain("import java.util.List;");
    }
}
