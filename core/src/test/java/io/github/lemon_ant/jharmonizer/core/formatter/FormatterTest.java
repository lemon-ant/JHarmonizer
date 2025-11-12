package io.github.lemon_ant.jharmonizer.core.formatter;

import static io.github.lemon_ant.jharmonizer.core.config.unified.UnifiedFormatterStyle.PALANTIR;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class FormatterTest {

    private Formatter formatter;

    @Test
    void fixImports_validSourceClass_returnExpectedFormattedClass() {
        formatter = new Formatter(PALANTIR, true);
        String sourceClass = "import junit.framework.TestCase; \npublic class Person {}";
        String expectedClass = "public class Person {}\n";

        FormatingResult formatingResult = formatter.formatSource(sourceClass);
        FormatingStatistic getFormatingStatistic = formatingResult.getFormatingStatistic();

        assertThat(formatingResult.getFormatedSourceCode()).isEqualTo(expectedClass);
        assertThat(getFormatingStatistic).isNotNull();
        assertThat(getFormatingStatistic.getFormattedCodeLength()).isEqualTo(expectedClass.length());
        assertThat(getFormatingStatistic.getFormattingTimeInNanos()).isGreaterThan(1000000);
    }

    @Test
    void formatSourceAndFixImports_validSourceClass_returnExpectedFormattedClass() {
        formatter = new Formatter(PALANTIR, false);
        String sourceClass = "public class Person {}";
        String expectedClass = "public class Person {}\n";
        FormatingResult formatingResult = formatter.formatSource(sourceClass);
        FormatingStatistic getFormatingStatistic = formatingResult.getFormatingStatistic();

        assertThat(formatingResult.getFormatedSourceCode()).isEqualTo(expectedClass);
        assertThat(getFormatingStatistic).isNotNull();
        assertThat(getFormatingStatistic.getFormattedCodeLength()).isEqualTo(expectedClass.length());
        assertThat(getFormatingStatistic.getFormattingTimeInNanos()).isGreaterThan(1000000);
    }
}
