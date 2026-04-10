package io.github.lemon_ant.jharmonizer.cli.command;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import io.github.lemon_ant.jharmonizer.core.SrcProcessor;
import io.github.lemon_ant.jharmonizer.core.processing_stat.SrcProcessingStats.AggregatedProcessingStatistic;
import java.nio.file.Path;
import lombok.NonNull;
import lombok.experimental.UtilityClass;
import org.mockito.MockedConstruction;
import org.slf4j.LoggerFactory;

@UtilityClass
class CommandTestUtils {

    @NonNull
    static MockedConstruction<SrcProcessor> mockSuccessfulProcessorConstruction() {
        return mockConstruction(SrcProcessor.class, (mock, context) -> {
            when(mock.processSources(any(Path.class), any(), any(), any()))
                    .thenReturn(mock(AggregatedProcessingStatistic.class));
        });
    }

    @NonNull
    static AutoCloseable suppressBaseCommandLogs() {
        Logger logger = (Logger) LoggerFactory.getLogger(BaseCommand.class);
        Level previousLevel = logger.getLevel();
        logger.setLevel(Level.OFF);
        return () -> logger.setLevel(previousLevel);
    }
}
