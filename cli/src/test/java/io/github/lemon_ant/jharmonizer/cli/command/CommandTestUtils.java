package io.github.lemon_ant.jharmonizer.cli.command;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import io.github.lemon_ant.jharmonizer.core.SrcProcessor;
import io.github.lemon_ant.jharmonizer.core.flow.FlowProcessingResult;
import io.github.lemon_ant.jharmonizer.core.flow.FlowType;
import io.github.lemon_ant.jharmonizer.core.flow.SrcProcessingResult;
import io.github.lemon_ant.jharmonizer.core.processing_stat.SrcProcessingStats.AggregatedProcessingStatistic;
import java.nio.file.Path;
import java.util.List;
import lombok.NonNull;
import lombok.experimental.UtilityClass;
import org.mockito.MockedConstruction;
import org.slf4j.LoggerFactory;

@UtilityClass
class CommandTestUtils {

    @NonNull
    static MockedConstruction<SrcProcessor> mockSuccessfulProcessorConstruction() {
        return mockConstruction(SrcProcessor.class, (mock, context) -> {
            when(mock.processSources(any(Path.class), any(), any(), any())).thenReturn(buildSuccessfulResult());
        });
    }

    /**
     * Creates a real successful {@link SrcProcessingResult} for test stubs.
     *
     * @return a successful processing result with mock statistics
     */
    @NonNull
    static SrcProcessingResult buildSuccessfulResult() {
        return SrcProcessingResult.buildResult(FlowType.REORDER, mock(AggregatedProcessingStatistic.class), List.of());
    }

    /**
     * Creates a real failed {@link SrcProcessingResult} for test stubs.
     *
     * @return a failed processing result with a mock stop trigger
     */
    @NonNull
    static SrcProcessingResult buildFailedResult() {
        return SrcProcessingResult.buildResult(
                FlowType.CHECK_FAIL_FAST,
                mock(AggregatedProcessingStatistic.class),
                List.of(mock(FlowProcessingResult.class)));
    }

    @NonNull
    static AutoCloseable suppressBaseCommandLogs() {
        Logger logger = (Logger) LoggerFactory.getLogger(BaseCommand.class);
        Level previousLevel = logger.getLevel();
        logger.setLevel(Level.OFF);
        return () -> logger.setLevel(previousLevel);
    }
}
