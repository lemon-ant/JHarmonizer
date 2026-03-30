package io.github.lemon_ant.jharmonizer.core.processing_stat;

import io.github.lemon_ant.jharmonizer.core.processing_stat.SrcProcessingStats.AggregatedProcessingStatistic;
import lombok.NonNull;
import lombok.experimental.UtilityClass;

@UtilityClass
public class AggregatedProcessingStatisticTestCreator {

    @NonNull
    public static AggregatedProcessingStatistic createEmpty() {
        return new AggregatedProcessingStatistic(0, 0, 0, null, null);
    }
}
