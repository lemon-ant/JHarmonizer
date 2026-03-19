package io.github.lemon_ant.jharmonizer.core.formatter;

import static java.util.Comparator.comparingInt;
import static java.util.Map.entry;

import com.google.common.collect.Range;
import com.palantir.javaformat.java.FormatterException;
import com.palantir.javaformat.java.JavaFormatterOptions;
import com.palantir.javaformat.java.JavaFormatterOptions.Builder;
import com.palantir.javaformat.java.JavaFormatterOptions.Style;
import io.github.lemon_ant.jharmonizer.core.config.unified.UnifiedFormatterStyle;
import io.github.lemon_ant.jharmonizer.core.translator.SrcCharacterRange;
import io.github.lemon_ant.jharmonizer.core.utilities.StopWatch;
import io.github.lemon_ant.jharmonizer.core.utilities.StopWatch.TimedResult;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.Validate;

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

    @NonNull
    public FormattingResult formatSource(
            @NonNull String srcCode,
            @NonNull Path srcPath,
            @NonNull List<@NonNull SrcCharacterRange> formattingSkippedRanges) {
        log.debug("Formatting {}", srcPath);
        TimedResult<String> formattingResult =
                StopWatch.measure(() -> applyFormatting(srcCode, formattingSkippedRanges));

        String formattedSource = formattingResult.getResult();
        return new FormattingResult(
                formattedSource, new FormattingStatistic(formattedSource.length(), formattingResult.getNanos()));
    }

    @NonNull
    private String applyFormatting(String srcCode, List<@NonNull SrcCharacterRange> formattingSkippedRanges) {
        if (formatterStyle == null) {
            return fixImports ? invokePalantir(() -> formatter.fixImports(srcCode)) : srcCode;
        }

        if (formattingSkippedRanges.isEmpty()) {
            return invokePalantir(
                    () -> fixImports ? formatter.formatSourceAndFixImports(srcCode) : formatter.formatSource(srcCode));
        }

        String partlyFormattedSrc = srcCode;
        Collection<Range<Integer>> formattingRanges =
                invertExcludedRanges(srcCode.length(), formattingSkippedRanges).stream()
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

    /**
     * Inverts excluded ranges into the complementary ranges that should be formatted.
     */
    @NonNull
    private static List<SrcCharacterRange> invertExcludedRanges(
            int sourceLength, List<SrcCharacterRange> excludedRanges) {
        Validate.isTrue(sourceLength >= 0, "Source length must be non-negative: %s", sourceLength);

        List<SrcCharacterRange> normalizedRanges = excludedRanges.stream()
                .sorted(comparingInt(SrcCharacterRange::getStartInclusive)
                        .thenComparingInt(SrcCharacterRange::getEndExclusive))
                .toList();
        List<SrcCharacterRange> includedRanges = new ArrayList<>();
        int nextStart = 0;

        for (SrcCharacterRange excludedRange : normalizedRanges) {
            Validate.isTrue(
                    excludedRange.getEndExclusive() <= sourceLength,
                    "Excluded range [%s, %s) exceeds source length %s",
                    excludedRange.getStartInclusive(),
                    excludedRange.getEndExclusive(),
                    sourceLength);
            Validate.isTrue(
                    excludedRange.getStartInclusive() >= nextStart,
                    "Excluded ranges must be sorted and non-overlapping, but [%s, %s) overlaps before %s",
                    excludedRange.getStartInclusive(),
                    excludedRange.getEndExclusive(),
                    nextStart);

            if (nextStart < excludedRange.getStartInclusive()) {
                includedRanges.add(SrcCharacterRange.of(nextStart, excludedRange.getStartInclusive()));
            }
            nextStart = excludedRange.getEndExclusive();
        }

        if (nextStart < sourceLength) {
            includedRanges.add(SrcCharacterRange.of(nextStart, sourceLength));
        }

        return List.copyOf(includedRanges);
    }

    @FunctionalInterface
    private interface FormattingOperation {
        String execute() throws FormatterException;
    }
}
