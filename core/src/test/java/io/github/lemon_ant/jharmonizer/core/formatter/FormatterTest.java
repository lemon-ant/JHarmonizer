package io.github.lemon_ant.jharmonizer.core.formatter;

import static io.github.lemon_ant.jharmonizer.core.config.unified.UnifiedFormatterStyle.PALANTIR;
import static org.assertj.core.api.Assertions.assertThat;

import io.github.lemon_ant.jharmonizer.core.common.SrcCharacterRange;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class FormatterTest {

    @Test
    void fixImports_validSourceClass_returnsExpectedFormattedClass() {
        // Given
        Formatter formatter = new Formatter(PALANTIR, true);
        String sourceClass = "import junit.framework.TestCase; \npublic class Person {}";
        String expectedClass = "public class Person {}\n";

        // When
        FormattingResult formattingResult = formatter.formatSource(sourceClass, Path.of(""), List.of());
        FormattingStatistic formattingStatistic = formattingResult.getFormattingStatistic();

        // Then
        assertThat(formattingResult.getFormattedSrcCode()).isEqualTo(expectedClass);
        assertThat(formattingStatistic).isNotNull();
        assertThat(formattingStatistic.getFormattedCodeLength()).isEqualTo(expectedClass.length());
        assertThat(formattingStatistic.getFormattingTimeInNanos()).isGreaterThan(1_000_000);
    }

    @Test
    void formatSourceAndFixImports_validSourceClass_returnsExpectedFormattedClass() {
        // Given
        Formatter formatter = new Formatter(PALANTIR, false);
        String sourceClass = "public class Person {}";
        String expectedClass = "public class Person {}\n";

        // When
        FormattingResult formattingResult = formatter.formatSource(sourceClass, Path.of(""), List.of());
        FormattingStatistic formattingStatistic = formattingResult.getFormattingStatistic();

        // Then
        assertThat(formattingResult.getFormattedSrcCode()).isEqualTo(expectedClass);
        assertThat(formattingStatistic).isNotNull();
        assertThat(formattingStatistic.getFormattedCodeLength()).isEqualTo(expectedClass.length());
        assertThat(formattingStatistic.getFormattingTimeInNanos()).isGreaterThan(1_000_000);
    }

    @Test
    void formatSource_withFullExclusionRange_keepSourceUnchanged() {
        // Given
        Formatter formatter = new Formatter(PALANTIR, false);
        String sourceCode = "class Person {  }\n";

        // When
        FormattingResult formatingResult = formatter.formatSource(
                sourceCode, Path.of("Person.java"), List.of(new SrcCharacterRange(0, sourceCode.length())));

        // Then
        assertThat(formatingResult.getFormattedSrcCode()).isEqualTo(sourceCode);
    }
}
