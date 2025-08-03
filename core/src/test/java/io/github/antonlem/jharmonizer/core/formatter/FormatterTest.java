package io.github.antonlem.jharmonizer.core.formatter;

import static org.assertj.core.api.Assertions.assertThat;

import com.palantir.javaformat.java.JavaFormatterOptions.Style;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class FormatterTest {

    private Formatter formatter;

    @Test
    void fixImports_validSourceClass_returnExpectedFormattedClass() {
        String sourceClass = "import junit.framework.TestCase; \npublic class Person {}";
        String expectedClass = "public class Person {}";

        FormatingResult formatingResult = formatter.fixImports(sourceClass);
        FormatingStatistic getFormatingStatistic = formatingResult.getFormatingStatistic();

        assertThat(formatingResult.getFormatedSourceCode()).isEqualTo(expectedClass);
        assertThat(getFormatingStatistic).isNotNull();
        assertThat(getFormatingStatistic.getFormattedCodeLength()).isEqualTo(expectedClass.length());
        assertThat(getFormatingStatistic.getFormattingTimeInNanos()).isGreaterThan(1000000);
    }

    @Test
    void formatSourceAndFixImports_validSourceClass_returnExpectedFormattedClass() {
        String sourceClass = "public class Person {}";
        String expectedClass = "public class Person {}\n";
        FormatingResult formatingResult = formatter.formatSourceAndFixImports(sourceClass);
        FormatingStatistic getFormatingStatistic = formatingResult.getFormatingStatistic();

        assertThat(formatingResult.getFormatedSourceCode()).isEqualTo(expectedClass);
        assertThat(getFormatingStatistic).isNotNull();
        assertThat(getFormatingStatistic.getFormattedCodeLength()).isEqualTo(expectedClass.length());
        assertThat(getFormatingStatistic.getFormattingTimeInNanos()).isGreaterThan(1000000);
    }

    @BeforeEach
    void setUp() {
        formatter = new Formatter(Style.PALANTIR, true);
    }
}
