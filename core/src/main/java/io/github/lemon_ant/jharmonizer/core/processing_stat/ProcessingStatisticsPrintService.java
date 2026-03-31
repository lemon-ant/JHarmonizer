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
import lombok.NonNull;
import lombok.experimental.UtilityClass;

/**
 * Renders processing statistics into a structured log-friendly report.
 */
@UtilityClass
public class ProcessingStatisticsPrintService {

    private static final int METRIC_WIDTH = 32;
    private static final int VALUE_WIDTH = 49;
    private static final String HEADER = "JHarmonizer harmonization summary";

    /**
     * Builds a pseudo-table report with a dedicated section for unexpected internal errors.
     *
     * @param stats aggregated statistics to print
     * @return formatted multiline report suitable for logs
     */
    @NonNull
    public String render(@NonNull AggregatedProcessingStatistic stats) {
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
        reportLines.add(renderRow("Min size", formatSizeWithPath(stats.getSmallestFile())));
        reportLines.add(renderRow("Max size", formatSizeWithPath(stats.getLargestFile())));
        reportLines.add(
                renderRow("Total processing time", formatHmsMillisFromNanos(stats.getTotalProcessingTimeNanos())));
        reportLines.add(renderRow(
                "Average processing time",
                formatSecondsMicrosecondsFromNanos(stats.calculateAverageProcessingTime()) + " s/file"));
        reportLines.add(
                renderRow("Files with unexpected internal errors", String.valueOf(sortedUnexpectedErrors.size())));
        reportLines.add(renderSeparator('-'));
        reportLines.add("Unexpected internal error files:");

        if (sortedUnexpectedErrors.isEmpty()) {
            reportLines.add("  - none");
        } else {
            for (Path path : sortedUnexpectedErrors) {
                reportLines.add("  - " + abbreviatePathForDisplay(path, AggregatedProcessingStatistic.MAX_PATH_LENGTH));
            }
        }

        reportLines.add(renderSeparator('='));
        return String.join(System.lineSeparator(), reportLines);
    }

    @NonNull
    private String formatSizeWithPath(@Nullable FileProcessingStatistic fileProcessingStatistic) {
        if (fileProcessingStatistic == null) {
            return formatBytes(0L);
        }
        return formatBytes(fileProcessingStatistic.getSize()) + " ("
                + abbreviatePathForDisplay(
                        fileProcessingStatistic.getPath(), AggregatedProcessingStatistic.MAX_PATH_LENGTH)
                + ")";
    }

    @NonNull
    private String renderRow(@NonNull String metric, @NonNull String value) {
        return String.format("| %-" + METRIC_WIDTH + "s | %-" + VALUE_WIDTH + "s |", metric, value);
    }

    @NonNull
    private String renderSeparator(char separatorChar) {
        return String.valueOf(separatorChar).repeat(METRIC_WIDTH + VALUE_WIDTH + 7);
    }
}
