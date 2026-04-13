package io.github.lemon_ant.jharmonizer.cli.command;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import io.github.lemon_ant.jharmonizer.core.SrcProcessingResult;
import io.github.lemon_ant.jharmonizer.core.SrcProcessor;
import io.github.lemon_ant.jharmonizer.core.processing_stat.FlowProcessingStats.AggregatedProcessingStatistic;
import java.lang.reflect.Constructor;
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
            when(mock.processSources(any(Path.class), any(), any(), any())).thenReturn(buildSuccessfulResult());
        });
    }

    /**
     * Creates a real successful {@link SrcProcessingResult} for test stubs.
     * Uses reflection because the constructor is package-private to the core module.
     *
     * @return a successful processing result with mock statistics
     */
    @NonNull
    static SrcProcessingResult buildSuccessfulResult() {
        return createSrcProcessingResult(mock(AggregatedProcessingStatistic.class), true);
    }

    /**
     * Creates a real failed {@link SrcProcessingResult} for test stubs.
     * Uses reflection because the constructor is package-private to the core module.
     *
     * @return a failed processing result
     */
    @NonNull
    static SrcProcessingResult buildFailedResult() {
        return createSrcProcessingResult(mock(AggregatedProcessingStatistic.class), false);
    }

    @NonNull
    @NonNull
    private static SrcProcessingResult createSrcProcessingResult(
            AggregatedProcessingStatistic statistics, boolean success) {
        try {
            Constructor<SrcProcessingResult> constructor = SrcProcessingResult.class.getDeclaredConstructor(
                    AggregatedProcessingStatistic.class, boolean.class);
            constructor.setAccessible(true);
            return constructor.newInstance(statistics, success);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Failed to create SrcProcessingResult for testing", exception);
        }
    }

    @NonNull
    static AutoCloseable suppressBaseCommandLogs() {
        Logger logger = (Logger) LoggerFactory.getLogger(BaseCommand.class);
        Level previousLevel = logger.getLevel();
        logger.setLevel(Level.OFF);
        return () -> logger.setLevel(previousLevel);
    }
}
