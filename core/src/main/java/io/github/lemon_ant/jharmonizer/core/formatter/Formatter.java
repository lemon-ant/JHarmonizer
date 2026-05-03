// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
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
import java.util.Collections;
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
    public FormattingResult formatSrc(
            @NonNull String srcCode,
            @NonNull Path srcPath,
            @NonNull List<@NonNull SrcCharacterRange> formattingSkippedRanges) {
        log.trace("Formatting {}", srcPath);
        TimedResult<String> formattingResult =
                StopWatch.measure(() -> applyFormatting(srcCode, formattingSkippedRanges));

        String formattedSrc = formattingResult.getResult();
        return new FormattingResult(
                formattedSrc, new FormattingStatistic(formattedSrc.length(), formattingResult.getNanos()));
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
        Collection<Range<Integer>> formattingRanges = invertExcludedRanges(srcCode.length(), formattingSkippedRanges);
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
    private static List<Range<Integer>> invertExcludedRanges(int srcLength, List<SrcCharacterRange> excludedRanges) {
        Validate.isTrue(srcLength >= 0, "Source length must be non-negative: %s", srcLength);

        List<SrcCharacterRange> normalizedRanges = excludedRanges.stream()
                .sorted(comparingInt(SrcCharacterRange::getStartInclusive)
                        .thenComparingInt(SrcCharacterRange::getEndExclusive))
                .toList();
        List<Range<Integer>> includedRanges = new ArrayList<>();
        int nextStart = 0;

        for (SrcCharacterRange excludedRange : normalizedRanges) {
            Validate.isTrue(
                    excludedRange.getEndExclusive() <= srcLength,
                    "Excluded range [%s, %s) exceeds source length %s",
                    excludedRange.getStartInclusive(),
                    excludedRange.getEndExclusive(),
                    srcLength);
            Validate.isTrue(
                    excludedRange.getStartInclusive() >= nextStart,
                    "Excluded ranges must be sorted and non-overlapping, but [%s, %s) overlaps before %s",
                    excludedRange.getStartInclusive(),
                    excludedRange.getEndExclusive(),
                    nextStart);

            if (nextStart < excludedRange.getStartInclusive()) {
                includedRanges.add(Range.closedOpen(nextStart, excludedRange.getStartInclusive()));
            }
            nextStart = excludedRange.getEndExclusive();
        }

        if (nextStart < srcLength) {
            includedRanges.add(Range.closedOpen(nextStart, srcLength));
        }

        return Collections.unmodifiableList(includedRanges);
    }

    @FunctionalInterface
    private interface FormattingOperation {
        String execute() throws FormatterException;
    }
}
