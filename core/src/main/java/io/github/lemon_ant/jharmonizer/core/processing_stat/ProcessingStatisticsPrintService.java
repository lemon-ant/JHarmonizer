package io.github.lemon_ant.jharmonizer.core.processing_stat;

import static io.github.lemon_ant.jharmonizer.core.processing_stat.HumanReadableFormatsUtils.formatBytes;
import static io.github.lemon_ant.jharmonizer.core.processing_stat.HumanReadableFormatsUtils.formatHmsMillisFromNanos;
import static io.github.lemon_ant.jharmonizer.core.processing_stat.HumanReadableFormatsUtils.formatSecondsMicrosecondsFromNanos;
import static io.github.lemon_ant.jharmonizer.core.processing_stat.PathDisplayFormatUtil.abbreviatePathForDisplay;

import edu.umd.cs.findbugs.annotations.Nullable;
import io.github.lemon_ant.jharmonizer.core.processing_stat.SrcProcessingStats.AggregatedProcessingStatistic;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import lombok.NonNull;
import lombok.experimental.UtilityClass;

/**
 * Renders processing statistics into a structured log-friendly report.
 */
@UtilityClass
public class ProcessingStatisticsPrintService {

    private static final int METRIC_WIDTH = 40;
    private static final int VALUE_WIDTH = 24;
    private static final String ELLIPSIS = "...";
    private static final int ELLIPSIS_LENGTH = ELLIPSIS.length();
    private static final int DETAIL_PATH_MAX_LENGTH = 120;
    private static final String HEADER = "JHarmonizer harmonization summary";

    /**
     * Builds a pseudo-table report with a dedicated section for unexpected internal errors.
     *
     * @param stats aggregated statistics to print
     * @return formatted multiline report suitable for logs
     */
    @NonNull
    public static String render(@NonNull AggregatedProcessingStatistic stats) {
        List<Path> sortedUnexpectedErrors = stats.getFilesWithUnexpectedErrors().stream()
                .sorted(Comparator.comparing(Path::toString))
                .toList();
        List<String> reportLines = new ArrayList<>();

        reportLines.add("");
        reportLines.add(renderSeparator('='));
        reportLines.add(" " + HEADER);
        reportLines.add(renderSeparator('='));
        reportLines.add(renderRow("Files processed", String.format("%,d", stats.getFileCount())));
        reportLines.add(renderRow("Total size", formatBytes(stats.getTotalSize())));
        reportLines.add(renderRow("Average size", formatBytes(stats.calculateAverageSize())));
        reportLines.add(renderRow("Min size", formatSize(stats.getSmallestFile())));
        reportLines.add(renderRow("Max size", formatSize(stats.getLargestFile())));
        reportLines.add(
                renderRow("Total processing time", formatHmsMillisFromNanos(stats.getTotalProcessingTimeNanos())));
        reportLines.add(renderRow(
                "Parsing time (share)",
                formatPhaseTimeAndPercent(stats.getTotalParsingTimeNanos(), stats.calculateParsingTimePercent())));
        reportLines.add(renderRow(
                "Sorting time (share)",
                formatPhaseTimeAndPercent(stats.getTotalSortingTimeNanos(), stats.calculateSortingTimePercent())));
        reportLines.add(renderRow(
                "Serialization time (share)",
                formatPhaseTimeAndPercent(
                        stats.getTotalSerializationTimeNanos(), stats.calculateSerializationTimePercent())));
        reportLines.add(renderRow(
                "Formatting time (share)",
                formatPhaseTimeAndPercent(
                        stats.getTotalFormattingTimeNanos(), stats.calculateFormattingTimePercent())));
        reportLines.add(renderRow(
                "Average processing time",
                formatSecondsMicrosecondsFromNanos(stats.calculateAverageProcessingTime()) + " s/file"));
        reportLines.add(
                renderRow("Files with unexpected internal errors", String.valueOf(sortedUnexpectedErrors.size())));
        reportLines.add(renderSeparator('-'));
        addSizeBoundaryPaths(reportLines, stats);

        if (sortedUnexpectedErrors.isEmpty()) {
            reportLines.add("Unexpected internal error files: none");
        } else {
            reportLines.add("Unexpected internal error files:");
            for (Path path : sortedUnexpectedErrors) {
                reportLines.add("- " + abbreviatePathForDisplay(path, DETAIL_PATH_MAX_LENGTH));
            }
        }
        return String.join(System.lineSeparator(), reportLines) + System.lineSeparator();
    }

    @NonNull
    private static String formatSize(@Nullable FileProcessingStatistic fileProcessingStatistic) {
        if (fileProcessingStatistic == null) {
            return formatBytes(0L);
        }
        return formatBytes(fileProcessingStatistic.getSize());
    }

    private static void addSizeBoundaryPaths(
            @NonNull List<String> reportLines, @NonNull AggregatedProcessingStatistic stats) {
        if (stats.getSmallestFile() == null && stats.getLargestFile() == null) {
            return;
        }
        reportLines.add("Size boundary files:");
        if (stats.getSmallestFile() != null) {
            reportLines.add("- Min size file: "
                    + abbreviatePathForDisplay(stats.getSmallestFile().getPath(), DETAIL_PATH_MAX_LENGTH));
        }
        if (stats.getLargestFile() != null) {
            reportLines.add("- Max size file: "
                    + abbreviatePathForDisplay(stats.getLargestFile().getPath(), DETAIL_PATH_MAX_LENGTH));
        }
    }

    @NonNull
    private static String renderRow(@NonNull String metric, @NonNull String value) {
        String metricCell = fitCell(metric, METRIC_WIDTH);
        String valueCell = fitCell(value, VALUE_WIDTH);
        return String.format("| %-" + METRIC_WIDTH + "s | %-" + VALUE_WIDTH + "s |", metricCell, valueCell);
    }

    @NonNull
    private static String renderSeparator(char separatorChar) {
        return String.valueOf(separatorChar).repeat(METRIC_WIDTH + VALUE_WIDTH + 7);
    }

    @NonNull
    private static String formatPhaseTimeAndPercent(long phaseTimeNanos, double phasePercent) {
        return formatSecondsMicrosecondsFromNanos(phaseTimeNanos) + " s ("
                + String.format(Locale.ROOT, "%.2f%%", phasePercent) + ")";
    }

    @NonNull
    private static String fitCell(@NonNull String value, int maxWidth) {
        if (value.length() <= maxWidth) {
            return value;
        }
        if (maxWidth <= ELLIPSIS_LENGTH) {
            return value.substring(0, maxWidth);
        }
        return value.substring(0, maxWidth - ELLIPSIS_LENGTH) + ELLIPSIS;
    }
}
