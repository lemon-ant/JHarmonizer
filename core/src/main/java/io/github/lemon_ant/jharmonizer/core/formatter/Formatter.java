package io.github.lemon_ant.jharmonizer.core.formatter;

import static java.util.Map.entry;

import com.palantir.javaformat.java.FormatterException;
import com.palantir.javaformat.java.JavaFormatterOptions;
import com.palantir.javaformat.java.JavaFormatterOptions.Builder;
import com.palantir.javaformat.java.JavaFormatterOptions.Style;
import io.github.lemon_ant.jharmonizer.core.config.unified.UnifiedFormatterStyle;
import io.github.lemon_ant.jharmonizer.core.utilities.StopWatch;
import io.github.lemon_ant.jharmonizer.core.utilities.StopWatch.TimedResult;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.function.FailableFunction;

@Slf4j
public final class Formatter {

    private static final Map<UnifiedFormatterStyle, Style> UNIFIED_2_PALANTIR_FORMATTING_STYLE = Map.ofEntries(
            /* UnifiedFormatterStyle.NONE is not mapped to return null */
            entry(UnifiedFormatterStyle.PALANTIR, Style.PALANTIR),
            entry(UnifiedFormatterStyle.AOSP, Style.AOSP),
            entry(UnifiedFormatterStyle.GOOGLE, Style.GOOGLE));

    private final Function<String, String> formattingMethod;

    /**
     * Creates a new Formatter.
     * @param style the style
     * @param fixImports the fix imports
     */
    public Formatter(@NonNull UnifiedFormatterStyle style, boolean fixImports) {
        Style formatterStyle = UNIFIED_2_PALANTIR_FORMATTING_STYLE.get(style);
        Builder formatterBuilder = JavaFormatterOptions.builder();
        if (null != formatterStyle) {
            formatterBuilder.style(formatterStyle);
        }
        JavaFormatterOptions options = formatterBuilder.build();
        com.palantir.javaformat.java.Formatter formatter =
                com.palantir.javaformat.java.Formatter.createFormatter(options);
        formattingMethod = prepareSrcFormattingMethod(fixImports, formatterStyle, formatter);
    }

    /**
     * Single try/catch wrapper for any Palantir operation that takes only the source code.
     */
    private static Function<String, String> wrapFailableFunction(
            FailableFunction<String, String, FormatterException> palantirMethod) {
        return src -> {
            try {
                return palantirMethod.apply(src);
            } catch (FormatterException e) {
                throw new IllegalArgumentException("Palantir formatting failure for the source code: " + src, e);
            }
        };
    }

    /**
     * Formats the given source code.
     *
     * @param sourceCode the source code to format
     * @return a FormatingResult containing the formatted source code and statistics
     */
    @NonNull
    public FormatingResult formatSource(String sourceCode, Path srcPath) {
        log.debug("Formatting {}", srcPath);
        TimedResult<String> formatingResult = StopWatch.measure(() -> formattingMethod.apply(sourceCode));

        String formattedSource = formatingResult.getResult();
        return new FormatingResult(
                formattedSource, new FormatingStatistic(formattedSource.length(), formatingResult.getNanos()));
    }

    private Function<String, String> prepareSrcFormattingMethod(
            boolean fixImports, Style formatterStyle, com.palantir.javaformat.java.Formatter formatter) {
        Optional<FailableFunction<String, String, FormatterException>> palantirMethod;
        if (formatterStyle != null) {
            palantirMethod = Optional.of(fixImports ? formatter::formatSourceAndFixImports : formatter::formatSource);
        } else {
            palantirMethod = fixImports ? Optional.of(formatter::fixImports) : Optional.empty(); // no-palantirMethod
        }

        return palantirMethod.map(Formatter::wrapFailableFunction).orElse(Function.identity());
    }
}
