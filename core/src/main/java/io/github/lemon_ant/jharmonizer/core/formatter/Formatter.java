package io.github.lemon_ant.jharmonizer.core.formatter;

import static java.util.Map.entry;

import com.google.common.collect.Range;
import com.palantir.javaformat.java.FormatterException;
import com.palantir.javaformat.java.JavaFormatterOptions;
import com.palantir.javaformat.java.JavaFormatterOptions.Builder;
import com.palantir.javaformat.java.JavaFormatterOptions.Style;
import io.github.lemon_ant.jharmonizer.core.config.unified.UnifiedFormatterStyle;
import io.github.lemon_ant.jharmonizer.core.optout.SourceCharacterRange;
import io.github.lemon_ant.jharmonizer.core.utilities.StopWatch;
import io.github.lemon_ant.jharmonizer.core.utilities.StopWatch.TimedResult;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class Formatter {

    private static final Map<UnifiedFormatterStyle, Style> UNIFIED_2_PALANTIR_FORMATTING_STYLE = Map.ofEntries(
            /* UnifiedFormatterStyle.NONE is not mapped to return null */
            entry(UnifiedFormatterStyle.PALANTIR, Style.PALANTIR),
            entry(UnifiedFormatterStyle.AOSP, Style.AOSP),
            entry(UnifiedFormatterStyle.GOOGLE, Style.GOOGLE));

    private final boolean fixImports;

    @SuppressWarnings("PMD.AvoidFieldNameMatchingTypeName")
    private final com.palantir.javaformat.java.Formatter formatter;

    private final Style formatterStyle;



    /**
     * Creates a new Formatter.
     * @param style the style
     * @param fixImports the fix imports
     */
    public Formatter(@NonNull UnifiedFormatterStyle style, boolean fixImports) {
        this.fixImports = fixImports;
        this.formatterStyle = UNIFIED_2_PALANTIR_FORMATTING_STYLE.get(style);
        Builder formatterBuilder = JavaFormatterOptions.builder();
        if (null != formatterStyle) {
            formatterBuilder.style(formatterStyle);
        }
        JavaFormatterOptions options = formatterBuilder.build();
        formatter = com.palantir.javaformat.java.Formatter.createFormatter(options);
    }

    /**
     * Formats the given source code.
     *
     * @param srcCode the source code to format
     * @return a FormatingResult containing the formatted source code and statistics
     */
    @NonNull
    public FormatingResult formatSource(@NonNull String srcCode, @NonNull Path srcPath) {
        return formatSource(srcCode, srcPath, List.of());
    }

    @NonNull
    public FormatingResult formatSource(
            @NonNull String srcCode,
            @NonNull Path srcPath,
            @NonNull List<SourceCharacterRange> formattingExclusionRanges) {
        log.debug("Formatting {}", srcPath);
        TimedResult<String> formatingResult =
                StopWatch.measure(() -> applyFormatting(srcCode, formattingExclusionRanges));

        String formattedSource = formatingResult.getResult();
        return new FormatingResult(
                formattedSource, new FormatingStatistic(formattedSource.length(), formatingResult.getNanos()));
    }

    @NonNull
    private String applyFormatting(String srcCode, List<SourceCharacterRange> formattingExclusionRanges) {
        if (formatterStyle == null) {
            return fixImports ? invokePalantir(() -> formatter.fixImports(srcCode)) : srcCode;
        }

        if (formattingExclusionRanges.isEmpty()) {
            return invokePalantir(
                    () -> fixImports ? formatter.formatSourceAndFixImports(srcCode) : formatter.formatSource(srcCode));
        }

        String partlyFormattedSrc = srcCode;
        Collection<Range<Integer>> formattingRanges =
                SourceCharacterRange.invert(srcCode.length(), formattingExclusionRanges).stream()
                        .map(range -> Range.closedOpen(range.getStartInclusive(), range.getEndExclusive()))
                        .toList();
        if (!formattingRanges.isEmpty()) {
            partlyFormattedSrc = invokePalantir(() -> formatter.formatSource(srcCode, formattingRanges));
        }

        String formattedSrc = partlyFormattedSrc;
        return fixImports ? invokePalantir(() -> formatter.fixImports(formattedSrc)) : formattedSrc;
    }

    @NonNull
    private static String invokePalantir(FormattingOperation formattingOperation) {
        try {
            return formattingOperation.execute();
        } catch (FormatterException exception) {
            throw new IllegalArgumentException("Palantir formatting failure: " + exception.getMessage(), exception);
        }
    }

    @FunctionalInterface
    private interface FormattingOperation {
        String execute() throws FormatterException;
    }
}
