package io.github.lemon_ant.jharmonizer.cli.command;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import io.github.lemon_ant.jharmonizer.core.SrcProcessingResult;
import io.github.lemon_ant.jharmonizer.core.SrcProcessingResultCreator;
import io.github.lemon_ant.jharmonizer.core.SrcProcessor;
import io.github.lemon_ant.jharmonizer.core.processing_stat.FlowProcessingStats.AggregatedProcessingStatistic;
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
        return SrcProcessingResultCreator.create(mock(AggregatedProcessingStatistic.class), true);
    }

    /**
     * Creates a real failed {@link SrcProcessingResult} for test stubs.
     *
     * @return a failed processing result
     */
    @NonNull
    static SrcProcessingResult buildFailedResult() {
        return SrcProcessingResultCreator.create(mock(AggregatedProcessingStatistic.class), false);
    }

    @NonNull
    static AutoCloseable suppressBaseCommandLogs() {
        Logger logger = (Logger) LoggerFactory.getLogger(BaseCommand.class);
        Level previousLevel = logger.getLevel();
        logger.setLevel(Level.OFF);
        return () -> logger.setLevel(previousLevel);
    }

    /**
     * Captures log events emitted by {@link BaseCommand} during the scope of the returned {@link AutoCloseable}.
     * Close the returned resource after the action under test completes.
     * All captured events are appended to {@code capturedEvents} on close.
     *
     * @param capturedEvents the list to which captured events are appended when the resource is closed
     * @return an {@code AutoCloseable} that detaches the log appender and populates the list on close
     */
    @NonNull
    static AutoCloseable captureBaseCommandLogEvents(@NonNull List<? super ILoggingEvent> capturedEvents) {
        Logger logger = (Logger) LoggerFactory.getLogger(BaseCommand.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        return () -> {
            logger.detachAppender(appender);
            appender.stop();
            capturedEvents.addAll(appender.list);
        };
    }
}
