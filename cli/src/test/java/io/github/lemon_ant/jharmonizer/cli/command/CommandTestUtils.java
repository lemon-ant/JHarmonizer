package io.github.lemon_ant.jharmonizer.cli.command;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.when;

import io.github.lemon_ant.jharmonizer.core.SrcProcessor;
import io.github.lemon_ant.jharmonizer.core.processing_stat.SrcProcessingStats.AggregatedProcessingStatistic;
import java.nio.file.Path;
import lombok.NonNull;
import lombok.experimental.UtilityClass;
import org.mockito.MockedConstruction;

@UtilityClass
class CommandTestUtils {

    @NonNull
    static MockedConstruction<SrcProcessor> mockSuccessfulProcessorConstruction() {
        return mockConstruction(SrcProcessor.class, (mock, context) -> {
            when(mock.processSources(any(Path.class), any(), any(), any()))
                    .thenReturn(mock(AggregatedProcessingStatistic.class));
        });
    }
}
