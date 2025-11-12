package io.github.lemon_ant.jharmonizer.core;

import static java.util.Collections.synchronizedList;

import io.github.lemon_ant.jharmonizer.core.SourceProcessingStats.AggregatedStats;
import io.github.lemon_ant.jharmonizer.core.SourceProcessingStats.FileStats;
import io.github.lemon_ant.jharmonizer.core.flow.FlowProcessingResult;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@SuppressWarnings("PMD.GuardLogStatement")
public class ProcessingResultCollector {
    // TODO Verify
    Collection<FileStats> fileStats = synchronizedList(new ArrayList<>());

    public ProcessingResultCollector collectResult(FlowProcessingResult flowProcessingResult) {
        log.trace("[ProcessingResultCollector] Accumulating statistics for {}", flowProcessingResult.getPath());
        // TODO Looks like bad design and useless storing into the collection
        fileStats.add(FileStats.builder()
                .path(flowProcessingResult.getPath())
                .size(flowProcessingResult.getParsingStatistic().getOriginalSourceCodeLength())
                .processingTimeNanos(TimeUnit.MILLISECONDS.toNanos(flowProcessingResult
                                .getParsingStatistic()
                                .getParsingTimeInNanos()
                        + flowProcessingResult.getSortingStatistic().getSortingTimeInNanos()
                        + flowProcessingResult.getSerializationStatistic().getProcessingTimeInNanos()
                        + flowProcessingResult.getFormatingStatistic().getFormattingTimeInNanos()))
                .build());
        return this;
    }

    public void printAggregatedStatistics() {
        AggregatedStats stats = fileStats.parallelStream().collect(SourceProcessingStats.statsCollector());
        log.info(stats.toString());
    }
}
