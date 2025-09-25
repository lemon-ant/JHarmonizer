package io.github.lemon_ant.jharmonizer.core.formatter;

import com.palantir.javaformat.java.Formatter;
import com.palantir.javaformat.java.JavaFormatterOptions;
import com.palantir.javaformat.java.JavaFormatterOptions.Style;
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@UtilityClass
public final class FormatterHelper {

    @SuppressWarnings("PMD.AvoidFieldNameMatchingTypeName")
    private static final Formatter FORMATTER = createFormatter();

    @SneakyThrows
    public static String fixImports(String formatingSource) {
        return FORMATTER.fixImports(formatingSource);
    }

    @SneakyThrows
    public static String formatSource(String formatingSource) {
        return FORMATTER.formatSource(formatingSource);
    }

    @SneakyThrows
    public static String formatSourceAndFixImports(String formatingSource) {
        return FORMATTER.formatSourceAndFixImports(formatingSource);
    }

    private static Formatter createFormatter() {
        JavaFormatterOptions options =
                JavaFormatterOptions.builder().style(Style.PALANTIR).build();
        return Formatter.createFormatter(options);
    }
}
