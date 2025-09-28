package io.github.lemon_ant.jharmonizer.core.formatter;

import com.palantir.javaformat.java.FormatterException;
import com.palantir.javaformat.java.JavaFormatterOptions;
import com.palantir.javaformat.java.JavaFormatterOptions.Style;
import io.github.lemon_ant.jharmonizer.core.utilities.StopWatch;
import io.github.lemon_ant.jharmonizer.core.utilities.StopWatch.TimedResult;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public final class Formatter {

    private final boolean fixImport;

    @NonNull
    @SuppressWarnings("PMD.AvoidFieldNameMatchingTypeName") // TODO An idiotic rule that must be removed
    private final com.palantir.javaformat.java.Formatter formatter;

    public Formatter(@NonNull Style style, boolean fixImports) {
        JavaFormatterOptions options =
                JavaFormatterOptions.builder().style(style).build();
        this.formatter = com.palantir.javaformat.java.Formatter.createFormatter(options);
        this.fixImport = fixImports;
    }

    /**
     * Fixes imports in the given source code.
     *
     * @param sourceCode the source code to fix imports for
     * @return a FormatingResult containing the formatted source code and statistics
     */
    @NonNull
    @SuppressWarnings("PMD.AvoidThrowingRawExceptionTypes")
    public FormatingResult fixImports(String sourceCode) {
        log.debug("Fix imports");
        TimedResult<String> formatingResult = StopWatch.measure(() -> {
            try {
                return formatter.fixImports(sourceCode);
            } catch (FormatterException e) {
                // TODO Come up with an exception model
                throw new RuntimeException(e);
            }
        });

        String formattedSource = formatingResult.getResult();
        return new FormatingResult(
                formattedSource, new FormatingStatistic(formattedSource.length(), formatingResult.getNanos()));
    }

    /**
     * Formats the given source code.
     *
     * @param sourceCode the source code to format
     * @return a FormatingResult containing the formatted source code and statistics
     */
    @NonNull
    @SuppressWarnings("PMD.AvoidThrowingRawExceptionTypes")
    public FormatingResult formatSource(String sourceCode) {
        log.debug("Formating source");
        TimedResult<String> formatingResult = StopWatch.measure(() -> {
            try {
                return formatter.formatSource(sourceCode);
            } catch (FormatterException e) {
                // TODO Come up with an exception model
                throw new RuntimeException(e);
            }
        });

        String formattedSource = formatingResult.getResult();
        return new FormatingResult(
                formattedSource, new FormatingStatistic(formattedSource.length(), formatingResult.getNanos()));
    }

    /**
     * Formats the given source code and fixes imports.
     *
     * @param sourceCode the source code to format and fix imports for
     * @return a FormatingResult containing the formatted source code and statistics
     */
    @NonNull
    @SuppressWarnings("PMD.AvoidThrowingRawExceptionTypes")
    public FormatingResult formatSourceAndFixImports(String sourceCode) {
        log.debug("Formating source and fix imports");
        TimedResult<String> formatingResult = StopWatch.measure(() -> {
            try {
                return formatter.formatSourceAndFixImports(sourceCode);
            } catch (FormatterException e) {
                // TODO Come up with an exception model
                throw new RuntimeException(e);
            }
        });

        String formattedSource = formatingResult.getResult();
        return new FormatingResult(
                formattedSource, new FormatingStatistic(formattedSource.length(), formatingResult.getNanos()));
    }
}
